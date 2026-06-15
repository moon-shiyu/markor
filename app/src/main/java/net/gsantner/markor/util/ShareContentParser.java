/*#######################################################
 *
 *   Maintained 2017-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.util;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure-Java utility for parsing shared content: link detection, URL sanitization,
 * and share-text extraction.
 * <p>
 * Production code must call {@link #init(Pattern)} with {@code android.util.Patterns.WEB_URL}
 * before using link-related methods. Test code calls {@link #resetForTesting()} which
 * installs a pure-Java fallback pattern.
 */
public class ShareContentParser {

    private static Pattern _urlPattern;

    /** Fallback pattern for use in unit tests (pure Java, no Android dependency). */
    private static final Pattern FALLBACK_URL = Pattern.compile(
            "(?i)(https?://)" +
                    "(?:[\\w.-]+@)?" +
                    "([\\w.-]+(?:\\.\\w{2,}))" +
                    "(?::\\d{1,5})?" +
                    "([/\\w.~:?#\\[\\]@!$&'()*+,;=%-]*)"
    );

    /**
     * Initialize with the platform URL pattern (call from production code).
     *
     * @param platformUrlPattern {@code android.util.Patterns.WEB_URL}
     */
    public static void init(final Pattern platformUrlPattern) {
        _urlPattern = platformUrlPattern;
    }

    /**
     * Install the pure-Java fallback pattern for unit testing.
     */
    public static void resetForTesting() {
        _urlPattern = FALLBACK_URL;
    }

    // -----------------------------------------------------------------------
    // URL sanitization
    // -----------------------------------------------------------------------

    /**
     * Strip common tracking parameters from a URL string.
     */
    public static String sanitize(final String link) {
        String dropGetParams = "utm_|source|si|__mk_|ref|sprefix|crid|partner|promo|ad_sub|gclid|fbclid|msclkid|dib";
        if (link.contains("amazon.")) {
            dropGetParams += "|qid|sr";
        }
        return link.replaceAll("(?m)(?<=&|\\?)(" + dropGetParams + ").*?(&|$|\\s|\\))", "");
    }

    // -----------------------------------------------------------------------
    // Share text extraction
    // -----------------------------------------------------------------------

    /**
     * Extract displayable share text from an incoming share's subject and text extras.
     * If the text looks like a URL, the subject is prepended as a title and tracking
     * parameters are stripped.
     *
     * @param subject {@code Intent.EXTRA_SUBJECT} (may be null)
     * @param text    {@code Intent.EXTRA_TEXT} (may be null)
     * @return the formatted share text, never null
     */
    public static String extractShareText(final String subject, final String text) {
        String title = subject;
        if (title != null) {
            title = title.trim() + " ";
        }

        String link = text != null ? text.trim() : "";

        if (_urlPattern != null && _urlPattern.matcher(link).matches()) {
            link = (title != null ? title : "") + sanitize(link);
        }

        return link;
    }

    // -----------------------------------------------------------------------
    // Per-line link / path detection
    // -----------------------------------------------------------------------

    /**
     * Parse a line in {@code "Title /absolute/path"} format.
     *
     * @return {@code String[]{title, path}} if the path exists on disk, otherwise null
     */
    public static String[] getLinePath(final CharSequence line) {
        final String trimmed = line.toString().trim();
        final int si = trimmed.lastIndexOf(" ");
        final String path = si == -1 ? trimmed : trimmed.substring(si + 1);
        final File file = new File(path);
        if (file.exists()) {
            final String title = si == -1 ? file.getName() : trimmed.substring(0, si);
            return new String[]{title, path};
        }
        return null;
    }

    /**
     * Parse a line in {@code "Title https://..."} format.
     *
     * @return {@code String[]{title, url}} if the last token matches a web URL, otherwise null
     */
    public static String[] getLineLink(final CharSequence line) {
        if (_urlPattern == null) {
            return null;
        }
        final String trimmed = line.toString().trim();
        final int si = trimmed.lastIndexOf(" ");
        final String path = si == -1 ? trimmed : trimmed.substring(si + 1);
        if (_urlPattern.matcher(path).matches()) {
            final String title = si == -1 ? getLinkTitle(path) : trimmed.substring(0, si);
            return new String[]{title, path};
        }
        return null;
    }

    /**
     * Check whether any line in the text contains a recognizable link or file path.
     */
    public static boolean hasLinks(final CharSequence text) {
        if (text == null) {
            return false;
        }
        final String str = text.toString();
        int start = 0;
        for (int i = 0; i <= str.length(); i++) {
            if (i == str.length() || str.charAt(i) == '\n') {
                final CharSequence line = str.subSequence(start, i);
                if (getLinePath(line) != null || getLineLink(line) != null) {
                    return true;
                }
                start = i + 1;
            }
        }
        return false;
    }

    /**
     * Extract the domain name from a URL to use as a link title.
     */
    public static String getLinkTitle(final String link) {
        if (_urlPattern == null) {
            return "";
        }
        final Matcher m = _urlPattern.matcher(link);
        if (m.matches()) {
            final String domain = extractDomain(m);
            return (domain != null && domain.endsWith("."))
                    ? domain.substring(0, domain.length() - 1) : domain;
        }
        return "";
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    /**
     * Extract the domain group from a URL matcher. Handles both the Android
     * {@code Patterns.WEB_URL} convention (group 4) and the fallback pattern (group 2).
     */
    private static String extractDomain(final Matcher m) {
        String domain = m.groupCount() >= 4 ? m.group(4) : null;
        if (domain == null && m.groupCount() >= 2) {
            domain = m.group(2);
        }
        return domain;
    }
}
