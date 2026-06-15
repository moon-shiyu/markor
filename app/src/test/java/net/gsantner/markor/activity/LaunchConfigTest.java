/*#######################################################
 *
 *   Maintained 2017-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.activity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

/**
 * Unit tests for the pure decision functions in {@link LaunchConfig}.
 * These methods have no Android dependencies and verify the launch-parameter
 * parsing logic that was extracted from DocumentActivity.handleLaunchingIntent.
 */
@SuppressWarnings("ALL")
public class LaunchConfigTest {

    // -----------------------------------------------------------------------
    // shouldStartInPreview
    // -----------------------------------------------------------------------

    @Test
    public void shouldStartInPreview_explicitFlag_returnsTrue() {
        assertThat(LaunchConfig.shouldStartInPreview(true, "notes.md")).isTrue();
    }

    @Test
    public void shouldStartInPreview_explicitFlagWithNullFilename_returnsTrue() {
        assertThat(LaunchConfig.shouldStartInPreview(true, null)).isTrue();
    }

    @Test
    public void shouldStartInPreview_indexHtml_returnsTrue() {
        assertThat(LaunchConfig.shouldStartInPreview(false, "index.html")).isTrue();
    }

    @Test
    public void shouldStartInPreview_indexMd_returnsTrue() {
        assertThat(LaunchConfig.shouldStartInPreview(false, "index.md")).isTrue();
    }

    @Test
    public void shouldStartInPreview_indexDot_returnsTrue() {
        assertThat(LaunchConfig.shouldStartInPreview(false, "index.")).isTrue();
    }

    @Test
    public void shouldStartInPreview_regularFile_returnsFalse() {
        assertThat(LaunchConfig.shouldStartInPreview(false, "notes.md")).isFalse();
    }

    @Test
    public void shouldStartInPreview_nullFilename_returnsFalse() {
        assertThat(LaunchConfig.shouldStartInPreview(false, null)).isFalse();
    }

    @Test
    public void shouldStartInPreview_indexInMiddle_returnsFalse() {
        assertThat(LaunchConfig.shouldStartInPreview(false, "my-index.html")).isFalse();
    }

    @Test
    public void shouldStartInPreview_indexUpperCase_returnsFalse() {
        assertThat(LaunchConfig.shouldStartInPreview(false, "Index.html")).isFalse();
    }

    @Test
    public void shouldStartInPreview_emptyFilename_returnsFalse() {
        assertThat(LaunchConfig.shouldStartInPreview(false, "")).isFalse();
    }

    @Test
    public void shouldStartInPreview_explicitTrueOverridesNonIndex() {
        assertThat(LaunchConfig.shouldStartInPreview(true, "readme.txt")).isTrue();
    }

    // -----------------------------------------------------------------------
    // resolveLineNumber
    // -----------------------------------------------------------------------

    @Test
    public void resolveLineNumber_explicitValue_returnsIt() {
        assertThat(LaunchConfig.resolveLineNumber(42, "99")).isEqualTo(42);
    }

    @Test
    public void resolveLineNumber_explicitZero_returnsZero() {
        assertThat(LaunchConfig.resolveLineNumber(0, "99")).isEqualTo(0);
    }

    @Test
    public void resolveLineNumber_explicitNegativeOne_returnsNegativeOne() {
        // -1 means "end of document"
        assertThat(LaunchConfig.resolveLineNumber(-1, "99")).isEqualTo(-1);
    }

    @Test
    public void resolveLineNumber_noExplicit_fallsBackToUriQuery() {
        assertThat(LaunchConfig.resolveLineNumber(null, "15")).isEqualTo(15);
    }

    @Test
    public void resolveLineNumber_noExplicit_uriQueryNegativeOne_returnsNegativeOne() {
        assertThat(LaunchConfig.resolveLineNumber(null, "-1")).isEqualTo(-1);
    }

    @Test
    public void resolveLineNumber_noExplicit_uriQueryZero_returnsZero() {
        assertThat(LaunchConfig.resolveLineNumber(null, "0")).isEqualTo(0);
    }

    @Test
    public void resolveLineNumber_noExplicit_noUri_returnsNull() {
        assertThat(LaunchConfig.resolveLineNumber(null, null)).isNull();
    }

    @Test
    public void resolveLineNumber_noExplicit_uriQueryInvalid_returnsNull() {
        assertThat(LaunchConfig.resolveLineNumber(null, "abc")).isNull();
    }

    @Test
    public void resolveLineNumber_noExplicit_uriQueryEmpty_returnsNull() {
        assertThat(LaunchConfig.resolveLineNumber(null, "")).isNull();
    }

    @Test
    public void resolveLineNumber_explicitTakesPriority_overUri() {
        // When both are present, the explicit extra wins
        assertThat(LaunchConfig.resolveLineNumber(10, "999")).isEqualTo(10);
    }

    @Test
    public void resolveLineNumber_explicitZero_uriQueryPresent_returnsZero() {
        assertThat(LaunchConfig.resolveLineNumber(0, "100")).isEqualTo(0);
    }

    @Test
    public void resolveLineNumber_noExplicit_uriQueryLargeNumber() {
        assertThat(LaunchConfig.resolveLineNumber(null, "999999")).isEqualTo(999999);
    }

    // -----------------------------------------------------------------------
    // LaunchConfig value object
    // -----------------------------------------------------------------------

    @Test
    public void constructor_storesFields() {
        final java.io.File file = new java.io.File("/tmp/test.md");
        final LaunchConfig config = new LaunchConfig(file, 5, true, LaunchConfig.Action.OPEN_DOCUMENT);
        assertThat(config.file).isEqualTo(file);
        assertThat(config.lineNumber).isEqualTo(5);
        assertThat(config.previewMode).isTrue();
        assertThat(config.action).isEqualTo(LaunchConfig.Action.OPEN_DOCUMENT);
    }

    @Test
    public void constructor_allowsNullFields() {
        final LaunchConfig config = new LaunchConfig(null, null, null, LaunchConfig.Action.UNSUPPORTED);
        assertThat(config.file).isNull();
        assertThat(config.lineNumber).isNull();
        assertThat(config.previewMode).isNull();
        assertThat(config.action).isEqualTo(LaunchConfig.Action.UNSUPPORTED);
    }

    @Test
    public void action_shareInto() {
        final LaunchConfig config = new LaunchConfig(null, null, null, LaunchConfig.Action.SHARE_INTO);
        assertThat(config.action).isEqualTo(LaunchConfig.Action.SHARE_INTO);
    }
}
