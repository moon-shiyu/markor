/*#######################################################
 *
 *   Maintained 2025 by gsantner
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.frontend.filesearch;

import static org.assertj.core.api.Assertions.assertThat;

import android.os.Build;

import net.gsantner.markor.frontend.filesearch.FileSearchEngine.SearchHit;
import net.gsantner.markor.frontend.filesearch.FileSearchEngine.SearchOptions;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import other.de.stanetz.jpencconverter.JavaPasswordbasedCryption;

/**
 * Pure JVM tests for the framework-free search core of {@link FileSearchEngine}.
 * <p>
 * The project has neither Robolectric nor {@code testOptions.unitTests.returnDefaultValues}, so any
 * {@code android.*} method/constructor call would throw "Method not mocked". These tests therefore
 * exercise only the extracted helpers and {@link FileSearchEngine#runSearch} (which call into
 * {@code GsFileUtils.isTextFile}/{@code isSymbolicLink} along pure code paths for {@code .md} files
 * and inject cancellation/progress/stream callbacks). The only android references below are the
 * compile-time constants {@code Build.VERSION_CODES.M} and
 * {@code JavaPasswordbasedCryption.DEFAULT_ENCRYPTION_EXTENSION}, which are inlined by javac.
 */
public class FileSearchEngineTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    // -------------------------------------------------------------------------
    // Query normalization / compilation
    // -------------------------------------------------------------------------

    @Test
    public void caseNormalization() {
        assertThat(FileSearchEngine.normalizeCase("ABC", false)).isEqualTo("abc");
        assertThat(FileSearchEngine.normalizeCase("ABC", true)).isEqualTo("ABC");
    }

    @Test
    public void globExpansionAndCompilation() {
        assertThat(FileSearchEngine.expandGlobToRegex("a*b")).isEqualTo("a.*b");
        // A '*' that already follows a '.' is left untouched (the (?<![.]) guard).
        assertThat(FileSearchEngine.expandGlobToRegex("a.*b")).isEqualTo("a.*b");
        assertThat(FileSearchEngine.expandGlobToRegex("*x")).isEqualTo(".*x");

        assertThat(FileSearchEngine.tryCompile("a.*b")).isNotNull();
        assertThat(FileSearchEngine.tryCompile("[")).isNull();
    }

    // -------------------------------------------------------------------------
    // Name matching
    // -------------------------------------------------------------------------

    @Test
    public void nameMatching() {
        // Non-regex: substring containment (inputs are already normalized by the caller).
        assertThat(FileSearchEngine.matchesName("readme.md", "me", null, false)).isTrue();
        assertThat(FileSearchEngine.matchesName("readme.md", "xyz", null, false)).isFalse();

        // Regex: a FULL match (Matcher.matches), not a substring find.
        final Matcher m = Pattern.compile("a.*b").matcher("");
        assertThat(FileSearchEngine.matchesName("axxb", "a.*b", m, true)).isTrue();
        assertThat(FileSearchEngine.matchesName("xaxxb", "a.*b", m, true)).isFalse();
    }

    // -------------------------------------------------------------------------
    // Content line preview
    // -------------------------------------------------------------------------

    @Test
    public void linePreviewFormatting() {
        // Short line (< previewLength) is returned verbatim.
        assertThat(FileSearchEngine.matchLinePreview("hello world", "world", null, false, false, true, 100))
                .isEqualTo("hello world");

        // No match -> null.
        assertThat(FileSearchEngine.matchLinePreview("abc", "xyz", null, false, false, true, 100))
                .isNull();

        // Match but preview disabled -> empty string (still a hit, just no preview text).
        assertThat(FileSearchEngine.matchLinePreview("hello world", "world", null, false, false, false, 100))
                .isEqualTo("");

        // Regex match on a case-insensitive line: the match is found on the lower-cased line,
        // but the returned preview preserves the ORIGINAL casing.
        final Matcher m = Pattern.compile("bar").matcher("");
        assertThat(FileSearchEngine.matchLinePreview("foo BAR baz", "bar", m, true, false, true, 100))
                .isEqualTo("foo BAR baz");
    }

    @Test
    public void longLinePreviewIsCenteredAndEllipsized() {
        final StringBuilder pad = new StringBuilder();
        for (int i = 0; i < 80; i++) {
            pad.append('x');
        }
        final String longLine = pad + "needle" + pad; // 166 chars, match in the middle

        final String preview = FileSearchEngine.matchLinePreview(longLine, "needle", null, false, false, true, 100);

        assertThat(preview).isNotNull();
        assertThat(preview).startsWith("\u2026 ").endsWith(" \u2026").contains("needle");
        assertThat(preview.length()).isLessThan(longLine.length());
    }

    // -------------------------------------------------------------------------
    // Ignored directories
    // -------------------------------------------------------------------------

    @Test
    public void ignoredDirectorySplittingAndMatching() {
        final Set<String> exact = new HashSet<>();
        final Set<Matcher> regex = new HashSet<>();
        final List<String> invalid = new ArrayList<>();

        // Quoted -> exact; glob/regex -> compiled matcher.
        FileSearchEngine.splitIgnored(Arrays.asList("\"node_modules\"", "build*", ".*cache"), false, exact, regex, invalid);
        // Mirrors FileSearchEngine.defaultIgnoredDirs (which is private, so it is replicated here).
        FileSearchEngine.splitIgnored(Arrays.asList("^\\.git$", "^\\.tmp$", ".*[Tt]humb.*"), false, exact, regex, invalid);

        assertThat(invalid).isEmpty();

        // Names arrive lower-cased from the walker when the search is case-insensitive.
        assertThat(FileSearchEngine.isIgnored("node_modules", exact, regex)).isTrue(); // exact
        assertThat(FileSearchEngine.isIgnored("build123", exact, regex)).isTrue();     // build*
        assertThat(FileSearchEngine.isIgnored("mycache", exact, regex)).isTrue();      // .*cache
        assertThat(FileSearchEngine.isIgnored(".git", exact, regex)).isTrue();         // default
        assertThat(FileSearchEngine.isIgnored("thumbs", exact, regex)).isTrue();       // default (case folded)
        assertThat(FileSearchEngine.isIgnored("src", exact, regex)).isFalse();
    }

    @Test
    public void invalidIgnorePatternIsCollected() {
        final Set<String> exact = new HashSet<>();
        final Set<Matcher> regex = new HashSet<>();
        final List<String> invalid = new ArrayList<>();

        FileSearchEngine.splitIgnored(Arrays.asList("["), false, exact, regex, invalid);

        assertThat(invalid).containsExactly("[");
        assertThat(regex).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Encryption gate
    // -------------------------------------------------------------------------

    @Test
    public void encryptedFileGate() {
        final String ext = JavaPasswordbasedCryption.DEFAULT_ENCRYPTION_EXTENSION;
        assertThat(FileSearchEngine.isEncryptedFile("a" + ext, Build.VERSION_CODES.M)).isTrue();
        assertThat(FileSearchEngine.isEncryptedFile("a" + ext, Build.VERSION_CODES.M - 1)).isFalse();
        assertThat(FileSearchEngine.isEncryptedFile("a.txt", Build.VERSION_CODES.M)).isFalse();
    }

    // -------------------------------------------------------------------------
    // Recursive search: depth limit
    // -------------------------------------------------------------------------

    @Test
    public void searchDepthIsHonored() throws IOException {
        final File root = tmp.getRoot();
        new File(root, "a.md").createNewFile();
        final File sub1 = new File(root, "sub1");
        assertThat(sub1.mkdir()).isTrue();
        new File(sub1, "b.md").createNewFile();
        final File sub2 = new File(sub1, "sub2");
        assertThat(sub2.mkdir()).isTrue();
        new File(sub2, "c.md").createNewFile();

        final String b = "sub1" + File.separator + "b.md";
        final String c = "sub1" + File.separator + "sub2" + File.separator + "c.md";

        assertThat(relPaths(runNameSearch(depthOptions(root, 1))))
                .containsExactlyInAnyOrder("a.md");
        assertThat(relPaths(runNameSearch(depthOptions(root, 2))))
                .containsExactlyInAnyOrder("a.md", b);
        assertThat(relPaths(runNameSearch(depthOptions(root, 3))))
                .containsExactlyInAnyOrder("a.md", b, c);
    }

    // -------------------------------------------------------------------------
    // Recursive search: content matching
    // -------------------------------------------------------------------------

    @Test
    public void contentSearchPopulatesLineMatches() throws IOException {
        final File root = tmp.getRoot();
        new File(root, "note.md").createNewFile(); // .md -> isTextFile resolves by extension (pure)

        final SearchOptions o = contentOptions(root, "secret");

        final boolean[] opened = {false};
        final List<SearchHit> hits = FileSearchEngine.runSearch(
                o, null, new HashSet<>(), new HashSet<>(),
                () -> false, v -> {},
                f -> {
                    opened[0] = true;
                    return stream("top secret line\nanother line\n");
                });

        assertThat(opened[0]).isTrue();
        assertThat(hits).hasSize(1);
        final SearchHit hit = hits.get(0);
        assertThat(hit.relPath).isEqualTo("note.md");
        assertThat(hit.isDirectory).isFalse();
        assertThat(hit.children).hasSize(1);
        assertThat(hit.children.get(0).preview).isEqualTo("top secret line");
        assertThat(hit.children.get(0).lineNumber).isEqualTo(0);
    }

    @Test
    public void onlyFirstContentMatchLimitsToOneLine() throws IOException {
        final File root = tmp.getRoot();
        new File(root, "note.md").createNewFile();

        final SearchOptions first = contentOptions(root, "secret");
        first.isOnlyFirstContentMatch = true;
        final List<SearchHit> firstHits = FileSearchEngine.runSearch(
                first, null, new HashSet<>(), new HashSet<>(),
                () -> false, v -> {},
                f -> stream("secret one\nsecret two\n"));
        assertThat(firstHits).hasSize(1);
        assertThat(firstHits.get(0).children).hasSize(1);

        final SearchOptions all = contentOptions(root, "secret");
        all.isOnlyFirstContentMatch = false;
        final List<SearchHit> allHits = FileSearchEngine.runSearch(
                all, null, new HashSet<>(), new HashSet<>(),
                () -> false, v -> {},
                f -> stream("secret one\nsecret two\n"));
        assertThat(allHits).hasSize(1);
        assertThat(allHits.get(0).children).hasSize(2);
    }

    @Test
    public void previewDisabledYieldsEmptyPreview() throws IOException {
        final File root = tmp.getRoot();
        new File(root, "note.md").createNewFile();

        final SearchOptions o = contentOptions(root, "secret");
        o.isShowMatchPreview = false;

        final List<SearchHit> hits = FileSearchEngine.runSearch(
                o, null, new HashSet<>(), new HashSet<>(),
                () -> false, v -> {},
                f -> stream("top secret line\n"));

        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).children).hasSize(1);
        assertThat(hits.get(0).children.get(0).preview).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Recursive search: encrypted file process vs skip branch (via injected stream)
    // -------------------------------------------------------------------------

    @Test
    public void encryptedNameContentSearchUsesInjectedStream() throws IOException {
        final File root = tmp.getRoot();
        final String name = "note.md" + JavaPasswordbasedCryption.DEFAULT_ENCRYPTION_EXTENSION; // note.md.jenc
        new File(root, name).createNewFile(); // isTextFile strips .jenc -> .md -> text (pure)

        // Process branch: content search ON -> the injected (decrypting) stream is consulted.
        final SearchOptions on = contentOptions(root, "secret");
        final boolean[] opened = {false};
        final List<SearchHit> onHits = FileSearchEngine.runSearch(
                on, null, new HashSet<>(), new HashSet<>(),
                () -> false, v -> {},
                f -> {
                    opened[0] = true;
                    return stream("decrypted secret payload\n");
                });
        assertThat(opened[0]).isTrue();
        assertThat(onHits).hasSize(1);
        assertThat(onHits.get(0).relPath).isEqualTo(name);
        assertThat(onHits.get(0).children).hasSize(1);

        // Skip branch: content search OFF -> stream is never opened; the name still matches.
        final SearchOptions off = depthOptions(root, 1);
        off.query = "note";
        final boolean[] openedOff = {false};
        final List<SearchHit> offHits = FileSearchEngine.runSearch(
                off, null, new HashSet<>(), new HashSet<>(),
                () -> false, v -> {},
                f -> {
                    openedOff[0] = true;
                    return stream("ignored\n");
                });
        assertThat(openedOff[0]).isFalse();
        assertThat(offHits).hasSize(1);
        assertThat(offHits.get(0).relPath).isEqualTo(name);
        assertThat(offHits.get(0).children).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Name-only search options (query "md" matches the .md test files, not the sub-dirs). */
    private static SearchOptions depthOptions(final File root, final int maxDepth) {
        final SearchOptions o = new SearchOptions();
        o.rootSearchDir = root;
        o.query = "md";
        o.isRegexQuery = false;
        o.isCaseSensitiveQuery = false;
        o.isSearchInContent = false;
        o.isOnlyFirstContentMatch = false;
        o.isShowMatchPreview = true;
        o.maxSearchDepth = maxDepth;
        return o;
    }

    private static SearchOptions contentOptions(final File root, final String query) {
        final SearchOptions o = depthOptions(root, 1);
        o.query = query;
        o.isSearchInContent = true;
        return o;
    }

    /** Runs a name-only search; the stream provider must never be invoked in this mode. */
    private static List<SearchHit> runNameSearch(final SearchOptions o) {
        return FileSearchEngine.runSearch(
                o, null, new HashSet<>(), new HashSet<>(),
                () -> false, v -> {},
                f -> {
                    throw new IllegalStateException("openStream must not be called for a name-only search");
                });
    }

    private static List<String> relPaths(final List<SearchHit> hits) {
        final List<String> out = new ArrayList<>(hits.size());
        for (final SearchHit hit : hits) {
            out.add(hit.relPath);
        }
        return out;
    }

    private static InputStream stream(final String contents) {
        return new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8));
    }
}
