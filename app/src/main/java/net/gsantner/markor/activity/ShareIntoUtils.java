/*#######################################################
 *
 *   Maintained 2018-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.activity;

import net.gsantner.opoc.wrapper.GsCallback;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Android-free helpers for the "Share Into" feature: parsing shared text,
 * stripping tracking parameters, splitting line title/target, interpolating the
 * share template and wrapping content per target format.
 * <p>
 * This class intentionally avoids any Android framework dependency so the logic
 * can be unit tested on the plain JVM. Pieces that intrinsically depend on the
 * Android framework (e.g. {@code android.util.Patterns.WEB_URL} URL detection)
 * are kept in {@link DocumentShareIntoFragment} and fed into these helpers as
 * already-computed values / callbacks.
 */
public final class ShareIntoUtils {

    private ShareIntoUtils() {
    }

    /**
     * Result of splitting a line into an optional title and a file target.
     */
    public static final class Link {
        public final String title;
        public final File file;

        public Link(final String title, final File file) {
            this.title = title;
            this.file = file;
        }
    }

    /**
     * Remove common tracking/affiliate GET parameters from a link.
     */
    public static String sanitize(final String link) {
        String dropGetParams = "utm_|source|si|__mk_|ref|sprefix|crid|partner|promo|ad_sub|gclid|fbclid|msclkid|dib";
        if (link.contains("amazon.")) {
            dropGetParams += "|qid|sr";
        }

        return link.replaceAll("(?m)(?<=&|\\?)(" + dropGetParams + ").*?(&|$|\\s|\\))", "");
    }

    /**
     * Build the text that pre-populates the Share Into editor from the shared
     * subject and text. When the shared text is a web URL the subject is used as
     * a prefix and the URL is sanitized; otherwise only the (trimmed) text is
     * used and the subject is dropped (matching the original behavior).
     *
     * @param subject       raw subject (e.g. {@code Intent.EXTRA_SUBJECT}), may be null
     * @param rawText       raw text (e.g. {@code Intent.EXTRA_TEXT}), may be null
     * @param rawTextIsWebUrl whether the trimmed {@code rawText} matches a web URL
     */
    public static String composeShareText(final String subject, final String rawText, final boolean rawTextIsWebUrl) {
        String title = subject;
        if (title != null) {
            title = title.trim() + " ";
        }

        String link = rawText != null ? rawText.trim() : "";

        if (rawTextIsWebUrl) {
            link = (title != null ? title : "") + sanitize(link);
        }

        return link;
    }

    /**
     * Split a line into an optional title and a target at the last space.
     * Returns a two element array: index 0 is the title (null when the line
     * contains no space), index 1 is the target (path or URL candidate).
     */
    public static String[] splitLastSpace(final CharSequence line) {
        final String trimmed = line.toString().trim();
        final int si = trimmed.lastIndexOf(" ");
        final String target = si == -1 ? trimmed : trimmed.substring(si + 1);
        final String title = si == -1 ? null : trimmed.substring(0, si);
        return new String[]{title, target};
    }

    /**
     * If the target portion of the line is an existing file, return its title
     * and file. The title defaults to the file name when the line has no space.
     */
    public static Link getLinePath(final CharSequence line) {
        final String[] tt = splitLastSpace(line);
        final File file = new File(tt[1]);
        if (file.exists()) {
            final String title = tt[0] != null ? tt[0] : file.getName();
            return new Link(title, file);
        }
        return null;
    }

    /**
     * Interpolate the share prefix template, replacing the text token with the
     * shared content and resolving date/time placeholders via {@code dateFmt}.
     *
     * @param prefix    the configured prefix, may contain {@code textToken} and date placeholders
     * @param shared    the shared content to insert at the token position
     * @param textToken the token marking where the shared content goes (e.g. {@code {{text}}})
     * @param dateFmt   resolves a single template part against the given time -> formatted string
     * @param timeMs    the timestamp used for date/time interpolation
     */
    public static String interpolateTemplate(
            final String prefix,
            final String shared,
            final String textToken,
            final GsCallback.s2<String, Long> dateFmt,
            final long timeMs
    ) {
        final List<String> parts = new ArrayList<>(Arrays.asList(prefix.split(Pattern.quote(textToken))));

        // Interpolate parts
        for (int i = 0; i < parts.size(); i++) {
            parts.set(i, dateFmt.callback(parts.get(i), timeMs));
        }

        // Put the shared text in the right place
        parts.add(parts.isEmpty() ? 0 : 1, shared);

        final StringBuilder sb = new StringBuilder();
        for (final String part : parts) {
            sb.append(part);
        }
        return sb.toString();
    }

    /**
     * Wrap the formatted share content for the target format. For todo.txt the
     * content is prefixed with the creation date and collapsed onto one line;
     * for any other format a leading newline is added.
     */
    public static String wrapForFormat(final String formatted, final boolean isTodoTxt, final String today) {
        if (isTodoTxt) {
            return today + " " + formatted.replaceAll("\\n+", " ");
        } else {
            return "\n" + formatted;
        }
    }
}
