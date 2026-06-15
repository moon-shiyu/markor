/*#######################################################
 *
 *   Maintained 2017-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.activity;

import net.gsantner.opoc.format.GsTextUtils;

/**
 * Pure, Android-free parsing of the launch parameters used by {@link DocumentActivity}
 * when opening a document. Extracted from {@code DocumentActivity.handleLaunchingIntent}
 * so the "which line / start in preview" decisions can be reviewed and unit tested in
 * isolation. The Intent-reading glue (reading extras and the {@code Uri} query) stays in
 * the activity; this class only contains the decision logic.
 */
public final class DocumentLaunchParams {

    private DocumentLaunchParams() {
    }

    /**
     * Resolve the start line for a document open request.
     * <p>
     * Precedence (mirrors the original inline logic exactly):
     * <ol>
     *     <li>An explicit {@code EXTRA_FILE_LINE_NUMBER} extra wins and is returned as-is
     *     (including {@code -1}, which downstream treats as "end of document").</li>
     *     <li>Otherwise a {@code ?line=} query parameter is parsed, defaulting to {@code -1}
     *     when it is not a valid integer.</li>
     *     <li>Otherwise {@code null} (let the fragment pick the last edit position).</li>
     * </ol>
     *
     * @param hasLineExtra   whether the intent carries the line-number extra
     * @param lineExtra      the value of the line-number extra (read with default {@code -1})
     * @param queryLineParam the {@code line} query parameter of the intent data, or {@code null}
     * @return the resolved start line, or {@code null} when none was requested
     */
    public static Integer resolveStartLine(
            final boolean hasLineExtra,
            final int lineExtra,
            final String queryLineParam
    ) {
        if (hasLineExtra) {
            return lineExtra;
        } else if (queryLineParam != null) {
            return GsTextUtils.tryParseInt(queryLineParam, -1);
        }
        return null;
    }

    /**
     * Resolve whether the document should open directly in preview mode.
     * <p>
     * Returns {@link Boolean#TRUE} when the preview extra was set or the file is an
     * {@code index.*} file; otherwise {@code null} so the fragment falls back to the
     * persisted per-file preview state. Mirrors the original inline logic exactly.
     *
     * @param doPreviewExtra value of {@code EXTRA_DO_PREVIEW} (read with default {@code false})
     * @param fileName       the file name being opened
     * @return {@link Boolean#TRUE} to force preview, or {@code null} to let the fragment decide
     */
    public static Boolean resolveStartInPreview(
            final boolean doPreviewExtra,
            final String fileName
    ) {
        if (doPreviewExtra || (fileName != null && fileName.startsWith("index."))) {
            return Boolean.TRUE;
        }
        return null;
    }
}
