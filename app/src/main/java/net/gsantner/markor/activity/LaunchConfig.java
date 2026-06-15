/*#######################################################
 *
 *   Maintained 2017-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.activity;

import java.io.File;

/**
 * Immutable value object representing the parsed launch configuration
 * for opening a document. Separates intent-parsing decisions from
 * Activity/Fragment lifecycle so they can be tested and reviewed
 * independently.
 *
 * <p>Created by {@link DocumentActivity#resolveLaunchConfig} from an
 * incoming {@link android.content.Intent}.</p>
 */
public class LaunchConfig {

    public enum Action {
        OPEN_DOCUMENT,
        SHARE_INTO,
        UNSUPPORTED
    }

    /** The resolved file to open, or {@code null} when the file cannot be determined. */
    public final File file;

    /**
     * The line number to navigate to, or {@code null} when no line
     * was specified. A value of {@code -1} means "go to end of document".
     */
    public final Integer lineNumber;

    /**
     * Whether the document should initially open in preview mode,
     * or {@code null} to let the Fragment decide from saved state.
     */
    public final Boolean previewMode;

    /** The resolved action to take for this launch. */
    public final Action action;

    public LaunchConfig(File file, Integer lineNumber, Boolean previewMode, Action action) {
        this.file = file;
        this.lineNumber = lineNumber;
        this.previewMode = previewMode;
        this.action = action;
    }

    // -----------------------------------------------------------------------
    // Pure decision functions (no Android dependencies)
    // -----------------------------------------------------------------------

    /**
     * Decide whether a document should start in preview mode.
     * Returns {@code true} when:
     * <ul>
     *   <li>{@code explicitPreview} is {@code true} (e.g. EXTRA_DO_PREVIEW), or</li>
     *   <li>the file name starts with "index." (e.g. index.html, index.md).</li>
     * </ul>
     *
     * @param explicitPreview whether an explicit preview flag was set
     * @param fileName        the file name (not the full path), may be {@code null}
     * @return {@code true} if preview mode should be activated
     */
    public static boolean shouldStartInPreview(boolean explicitPreview, String fileName) {
        if (explicitPreview) {
            return true;
        }
        return fileName != null && fileName.startsWith("index.");
    }

    /**
     * Resolve the starting line number from an explicit extra and a
     * URI query-parameter fallback.
     *
     * @param explicitLineNumber  the value from EXTRA_FILE_LINE_NUMBER, or {@code null}
     *                            when the extra was not present
     * @param uriLineQueryParameter the value of the "line" query parameter from the
     *                            intent URI, or {@code null} when not present
     * @return the resolved line number, or {@code null} if neither source provides one.
     *         A return value of {@code -1} means "end of document".
     */
    public static Integer resolveLineNumber(Integer explicitLineNumber, String uriLineQueryParameter) {
        if (explicitLineNumber != null) {
            return explicitLineNumber;
        }
        if (uriLineQueryParameter != null) {
            try {
                return Integer.parseInt(uriLineQueryParameter);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
