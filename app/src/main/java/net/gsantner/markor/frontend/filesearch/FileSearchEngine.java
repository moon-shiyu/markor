package net.gsantner.markor.frontend.filesearch;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Pair;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.snackbar.Snackbar;

import net.gsantner.markor.R;
import net.gsantner.opoc.util.GsCollectionUtils;
import net.gsantner.opoc.util.GsFileUtils;
import net.gsantner.opoc.wrapper.GsCallback;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import other.de.stanetz.jpencconverter.JavaPasswordbasedCryption;

@SuppressWarnings("WeakerAccess")

public class FileSearchEngine {
    public static final AtomicBoolean isSearchExecuting = new AtomicBoolean(false);
    public static final AtomicReference<WeakReference<Activity>> activity = new AtomicReference<>();

    private static final List<String> defaultIgnoredDirs = Arrays.asList("^\\.git$", "^\\.tmp$", ".*[Tt]humb.*");
    private static final int maxPreviewLength = 100;
    public static final int maxQueryHistoryCount = 20;
    public static final LinkedList<String> queryHistory = new LinkedList<>();

    public static void addToHistory(String query) {
        queryHistory.remove(query);

        if (queryHistory.size() == maxQueryHistoryCount) {
            queryHistory.removeLast();
        }
        queryHistory.addFirst(query);
    }

    public static class SearchOptions {
        public File rootSearchDir;
        public String query;

        public boolean isRegexQuery;
        public boolean isCaseSensitiveQuery;
        public boolean isSearchInContent;
        public boolean isOnlyFirstContentMatch;

        public int maxSearchDepth;
        public List<String> ignoredDirectories;
        public boolean isShowMatchPreview = true;
        public char[] password = new char[0];
        public int message = 0;
    }

    public static class FitFile {
        public final File file;
        public final String relPath;
        public final boolean isDirectory;
        public final @NonNull List<Pair<String, Integer>> children;

        public FitFile(
                final File file,
                final String relPath,
                final boolean isDirectory,
                final @Nullable List<Pair<String, Integer>> lineNumbers
        ) {
            this.file = file.getAbsoluteFile();
            this.relPath = relPath;
            this.isDirectory = isDirectory;
            this.children = Collections.unmodifiableList(lineNumbers != null ? lineNumbers : Collections.emptyList());
        }

        @NonNull
        @Override
        public String toString() {
            return (!children.isEmpty() ? String.format("(%s) ", children.size()) : "") + relPath;
        }
    }

    public static FileSearchEngine.QueueSearchFilesTask queueFileSearch(
            @NonNull final Activity activity,
            final SearchOptions config,
            final GsCallback.a1<List<FitFile>> callback
    ) {
        FileSearchEngine.activity.set(new WeakReference<>(activity));
        FileSearchEngine.isSearchExecuting.set(true);
        FileSearchEngine.addToHistory(config.query);
        FileSearchEngine.QueueSearchFilesTask task = new FileSearchEngine.QueueSearchFilesTask(config, callback);
        task.execute();

        return task;
    }

    // ---------------------------------------------------------------------------------------------
    // Pure, framework-free search core (no android.* calls -> unit testable on a plain JVM).
    // The AsyncTask below is a thin shell that injects cancellation / progress / stream and maps
    // the pure results to the android.util.Pair based FitFile consumed by the result dialog.
    // ---------------------------------------------------------------------------------------------

    /** A single content line hit. Plain Java mirror of the android.util.Pair stored in FitFile. */
    static final class LineMatch {
        final String preview;
        final int lineNumber;

        LineMatch(final String preview, final int lineNumber) {
            this.preview = preview;
            this.lineNumber = lineNumber;
        }
    }

    /** A matched file/dir. Plain Java mirror of FitFile (no android types). */
    static final class SearchHit {
        final File file;
        final String relPath;
        final boolean isDirectory;
        final List<LineMatch> children;

        SearchHit(final File file, final String relPath, final boolean isDirectory, final List<LineMatch> children) {
            this.file = file;
            this.relPath = relPath;
            this.isDirectory = isDirectory;
            this.children = children;
        }
    }

    /** Stack frame for the iterative directory walk (replaces android.util.Pair&lt;File,Integer&gt;). */
    private static final class Frame {
        final File dir;
        final int depth;

        Frame(final File dir, final int depth) {
            this.dir = dir;
            this.depth = depth;
        }
    }

    /** Lower-cases the query unless the search is case sensitive. */
    static String normalizeCase(final String query, final boolean caseSensitive) {
        return caseSensitive ? query : query.toLowerCase();
    }

    /** Expands the simple {@code *} glob wildcard to {@code .*} while leaving existing {@code .*} intact. */
    static String expandGlobToRegex(final String query) {
        return query.replaceAll("(?<![.])[*]", ".*");
    }

    /** Compiles a regex, returning {@code null} (instead of throwing / toasting) when it is invalid. */
    static Pattern tryCompile(final String regex) {
        try {
            return Pattern.compile(regex);
        } catch (Exception ex) {
            return null;
        }
    }

    /** Name match: regex performs a full {@link Matcher#matches()}, otherwise a substring containment. */
    static boolean matchesName(final String name, final String query, final Matcher matcher, final boolean isRegex) {
        return isRegex ? matcher.reset(name).matches() : name.contains(query);
    }

    /**
     * Match a single line and return its preview, or {@code null} when the line does not match.
     * Match indices are taken from the (optionally lower-cased) line, the preview from the original.
     */
    static String matchLinePreview(
            final String line,
            final String query,
            final Matcher matcher,
            final boolean isRegex,
            final boolean caseSensitive,
            final boolean showPreview,
            final int previewLength
    ) {
        final String preparedLine = caseSensitive ? line : line.toLowerCase();

        int start = -1, end = -1;
        if (isRegex) {
            if (matcher.reset(preparedLine).find()) {
                start = matcher.start();
                end = matcher.end();
            }
        } else {
            start = preparedLine.indexOf(query);
            if (start >= 0) {
                end = start + query.length();
            }
        }

        // Preview is based on original line
        if (start >= 0 && end <= line.length()) {
            if (!showPreview) {
                return "";
            }
            if (line.length() < previewLength) {
                return line;
            } else {
                int offset = (previewLength - (end - start)) / 2;
                int subStart = Math.max(start - offset, 0);
                int subEnd = Math.min(end + offset, line.length());
                return String.format("… %s …", line.substring(subStart, subEnd));
            }
        }
        return null;
    }

    /**
     * Split ignore patterns into an exact-match set and a regex-matcher set.
     * Quoted ({@code "..."}) patterns are exact; everything else is treated as a (glob-expanded) regex.
     * Patterns that fail to compile are appended to {@code invalidOut} (no UI side effect here).
     */
    static void splitIgnored(
            final List<String> list,
            final boolean caseSensitive,
            final Set<String> exactList,
            final Set<Matcher> regexList,
            final List<String> invalidOut
    ) {
        for (String pattern : (list != null ? list : new ArrayList<String>())) {
            if (pattern.isEmpty()) {
                continue;
            }
            if (!caseSensitive) {
                pattern = pattern.toLowerCase();
            }

            if (pattern.startsWith("\"")) {
                pattern = pattern.replace("\"", "");
                if (pattern.isEmpty()) {
                    continue;
                }
                exactList.add(pattern);
            } else {
                pattern = expandGlobToRegex(pattern);
                final Pattern compiled = tryCompile(pattern);
                if (compiled != null) {
                    regexList.add(compiled.matcher(""));
                } else if (invalidOut != null) {
                    invalidOut.add(pattern);
                }
            }
        }
    }

    /** True when {@code dirName} matches any exact entry or any ignore regex. */
    static boolean isIgnored(final String dirName, final Set<String> exactList, final Set<Matcher> regexList) {
        for (final String pattern : exactList) {
            if (dirName.equals(pattern)) {
                return true;
            }
        }

        for (final Matcher matcher : regexList) {
            if (matcher.reset(dirName).matches()) {
                return true;
            }
        }
        return false;
    }

    /** Encryption gate: only on Android M+ and only for the {@code .jenc} extension. */
    static boolean isEncryptedFile(final String fileName, final int sdkInt) {
        return sdkInt >= Build.VERSION_CODES.M && fileName.endsWith(JavaPasswordbasedCryption.DEFAULT_ENCRYPTION_EXTENSION);
    }

    /**
     * Run the recursive search. Pure logic: cancellation, progress reporting and stream opening
     * (including decryption) are injected so this can be exercised without the Android framework.
     *
     * @param matcher        pre-built matcher (may be {@code null} for a non-regex query)
     * @param isCancelled    polled before/within each directory; abort when it returns true
     * @param publishProgress receives {@code [queueLength, depth, resultCount, checkedFiles]}
     * @param openStream     opens a readable stream for a file (handles encrypted files)
     */
    static List<SearchHit> runSearch(
            final SearchOptions config,
            final Matcher matcher,
            final Set<String> ignoredExact,
            final Set<Matcher> ignoredRegex,
            final GsCallback.b0 isCancelled,
            final GsCallback.a1<int[]> publishProgress,
            final GsCallback.r1<InputStream, File> openStream
    ) {
        final List<SearchHit> result = new ArrayList<>();
        final int[] countCheckedFiles = {0};

        final ArrayDeque<Frame> stack = new ArrayDeque<>();
        stack.add(new Frame(config.rootSearchDir, 0));
        final int trimLength = config.rootSearchDir.getAbsolutePath().length() + 1;

        Frame frame;
        while ((frame = stack.pollLast()) != null && !isCancelled.callback()) {
            final int depth = frame.depth;
            final File dir = frame.dir;

            if (depth < config.maxSearchDepth && dir.canRead()) {
                handleDirectory(dir, trimLength, depth, config, matcher, ignoredExact, ignoredRegex, isCancelled, openStream, result, countCheckedFiles, stack);
                publishProgress.callback(new int[]{stack.size(), depth, result.size(), countCheckedFiles[0]});
            }
        }

        // Sorting is intentionally left to the caller: GsCollectionUtils.keySort relies on
        // android.util.Pair, which keeps this core free of Android dependencies for unit testing.
        return result;
    }

    private static void handleDirectory(
            final File dir,
            final int trimSize,
            final int depth,
            final SearchOptions config,
            final Matcher matcher,
            final Set<String> ignoredExact,
            final Set<Matcher> ignoredRegex,
            final GsCallback.b0 isCancelled,
            final GsCallback.r1<InputStream, File> openStream,
            final List<SearchHit> result,
            final int[] countCheckedFiles,
            final ArrayDeque<Frame> stack
    ) {

        final File[] files = dir.listFiles();

        if (files == null) {
            return;
        }

        countCheckedFiles[0] += files.length;

        for (final File file : files) {

            if (isCancelled.callback()) {
                return;
            }

            final String name = config.isCaseSensitiveQuery ? file.getName() : file.getName().toLowerCase();

            if (!isIgnored(name, ignoredExact, ignoredRegex)) {

                final boolean isDir = file.isDirectory();
                final String relPath = file.getAbsolutePath().substring(trimSize);

                final int beforeContentCount = result.size();
                if (config.isSearchInContent && !isDir && file.canRead() && GsFileUtils.isTextFile(file)) {
                    getContentMatches(file, relPath, config, matcher, isCancelled, openStream, result);
                }

                // Search name if directory or not already included due to content
                if (isDir || result.size() == beforeContentCount) {
                    if (matchesName(name, config.query, matcher, config.isRegexQuery)) {
                        result.add(new SearchHit(file, relPath, isDir, new ArrayList<>()));
                    }
                }

                // Only check for symbolic link directories
                if (isDir && depth < config.maxSearchDepth && !GsFileUtils.isSymbolicLink(file)) {
                    stack.addLast(new Frame(file, depth + 1));
                }
            }
        }
    }

    private static void getContentMatches(
            final File file,
            final String relPath,
            final SearchOptions config,
            final Matcher matcher,
            final GsCallback.b0 isCancelled,
            final GsCallback.r1<InputStream, File> openStream,
            final List<SearchHit> result
    ) {
        List<LineMatch> contentMatches = null;

        try (final BufferedReader br = new BufferedReader(new InputStreamReader(openStream.callback(file)))) {
            int lineNumber = 0;
            for (String line; (line = br.readLine()) != null; ) {
                if (isCancelled.callback()) {
                    break;
                }
                final String preview = matchLinePreview(line, config.query, matcher, config.isRegexQuery, config.isCaseSensitiveQuery, config.isShowMatchPreview, maxPreviewLength);
                if (preview != null) {

                    // We lazily create the match list
                    // And therefore avoid creating it for _every_ file
                    if (contentMatches == null) {
                        contentMatches = new ArrayList<>();
                        result.add(new SearchHit(file, relPath, false, contentMatches));
                    }

                    // Note that content matches is only created on the first find
                    contentMatches.add(new LineMatch(preview, lineNumber));

                    if (config.isOnlyFirstContentMatch) {
                        break;
                    }
                }
                lineNumber++;
            }
        } catch (Exception ignored) {
        }
    }

    public static class QueueSearchFilesTask extends AsyncTask<Void, Integer, List<FitFile>> {
        private final SearchOptions _config;
        private final GsCallback.a1<List<FitFile>> _callback;

        // _matcher.reset() is _not_ thread safe. Will need alternate approach when we make search parallel
        private final Matcher _matcher;

        private Snackbar _snackBar;
        private final Set<Matcher> _ignoredRegexDirs = new HashSet<>();
        private final Set<String> _ignoredExactDirs = new HashSet<>();

        public QueueSearchFilesTask(final SearchOptions config, final GsCallback.a1<List<FitFile>> callback) {
            _config = config;
            _callback = callback;

            _config.query = normalizeCase(_config.query, _config.isCaseSensitiveQuery);

            final List<String> invalidPatterns = new ArrayList<>();
            splitIgnored(config.ignoredDirectories, _config.isCaseSensitiveQuery, _ignoredExactDirs, _ignoredRegexDirs, invalidPatterns);
            splitIgnored(FileSearchEngine.defaultIgnoredDirs, _config.isCaseSensitiveQuery, _ignoredExactDirs, _ignoredRegexDirs, invalidPatterns);

            Pattern pattern = null;
            if (_config.isRegexQuery) {
                _config.query = expandGlobToRegex(_config.query);
                pattern = tryCompile(_config.query);
                if (pattern == null) {
                    invalidPatterns.add(_config.query);
                }
            }
            _matcher = pattern != null ? pattern.matcher("") : null;

            for (final String invalid : invalidPatterns) {
                showRegexError(invalid);
            }
        }

        private static void showRegexError(final String pattern) {
            final WeakReference<Activity> ref = activity.get();
            final Activity a = ref != null ? ref.get() : null;
            if (a != null) {
                final String errorMessage = a.getString(R.string.regex_can_not_be_compiled) + ": " + pattern;
                Toast.makeText(a, errorMessage, Toast.LENGTH_LONG).show();
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (_config.isRegexQuery && _matcher == null) {
                cancel(true);
                return;
            }
            bindSnackBar(_config.query);
        }

        public void bindSnackBar(String text) {
            if (!FileSearchEngine.isSearchExecuting.get()) {
                return;
            }

            try {
                final View view = activity.get().get().findViewById(android.R.id.content);
                _snackBar = Snackbar.make(view, text, Snackbar.LENGTH_INDEFINITE);
                _snackBar.addCallback(new Snackbar.Callback() {
                            @Override
                            public void onDismissed(Snackbar snackbar, int event) {
                                if (FileSearchEngine.isSearchExecuting.get()) {
                                    bindSnackBar(text);
                                }
                            }
                        })
                        .setAction(android.R.string.cancel, (v) -> {
                            _snackBar.dismiss();
                            cancel(true);
                        })
                        .show();
            } catch (Exception ignored) {
                cancel(true);
            }
        }

        @Override
        protected List<FitFile> doInBackground(final Void... ignored) {
            final List<SearchHit> hits = runSearch(
                    _config,
                    _matcher,
                    _ignoredExactDirs,
                    _ignoredRegexDirs,
                    this::isCancelled,
                    (v) -> publishProgress(v[0], v[1], v[2], v[3]),
                    this::openStream
            );
            final List<FitFile> result = toFitFiles(hits);
            GsCollectionUtils.keySort(result, f -> f.relPath.toLowerCase());
            return result;
        }

        private static List<FitFile> toFitFiles(final List<SearchHit> hits) {
            final List<FitFile> out = new ArrayList<>(hits.size());
            for (final SearchHit hit : hits) {
                List<Pair<String, Integer>> children = null;
                if (!hit.children.isEmpty()) {
                    children = new ArrayList<>(hit.children.size());
                    for (final LineMatch lm : hit.children) {
                        children.add(new Pair<>(lm.preview, lm.lineNumber));
                    }
                }
                out.add(new FitFile(hit.file, hit.relPath, hit.isDirectory, children));
            }
            return out;
        }

        private InputStream openStream(final File file) {
            try {
                if (isEncryptedFile(file.getName(), Build.VERSION.SDK_INT)) {
                    final byte[] encryptedContext = GsFileUtils.readCloseStreamWithSize(new FileInputStream(file), (int) file.length());
                    return new ByteArrayInputStream(JavaPasswordbasedCryption.getDecryptedText(encryptedContext, _config.password.clone()).getBytes(StandardCharsets.UTF_8));
                } else {
                    return new FileInputStream(file);
                }
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            if (_snackBar != null) {
                // _currentQueueLength, _currentSearchDepth, _result.size(), _countCheckedFiles
                _snackBar.setText("⭕" + values[2] + " || \uD83D\uDD0D" + values[0] + " || ⬇️ " + values[1] + " || \uD83D\uDC41️" + values[3] + "\n" + _config.query);
            }
        }

        @Override
        protected void onPostExecute(List<FitFile> ret) {
            super.onPostExecute(ret);
            FileSearchEngine.isSearchExecuting.set(false);
            if (_snackBar != null) {
                _snackBar.dismiss();
            }
            if (!isCancelled() && _callback != null) {
                try {
                    _callback.callback(ret);
                } catch (Exception ignored) {
                }
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            FileSearchEngine.isSearchExecuting.set(false);
        }
    }
}
