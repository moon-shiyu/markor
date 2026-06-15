/*#######################################################
 *
 *   Maintained 2017-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * Unit tests for the pure launch-parameter parsing extracted from
 * {@code DocumentActivity.handleLaunchingIntent}.
 */
public class DocumentLaunchParamsTest {

    // -- resolveStartLine ---------------------------------------------------

    @Test
    public void startLine_lineExtraTakesPrecedence() {
        // An explicit extra wins and is returned as-is (including -1 = "end of document")
        assertEquals(Integer.valueOf(5), DocumentLaunchParams.resolveStartLine(true, 5, null));
        assertEquals(Integer.valueOf(-1), DocumentLaunchParams.resolveStartLine(true, -1, null));
        // ... even when a query parameter is also present
        assertEquals(Integer.valueOf(5), DocumentLaunchParams.resolveStartLine(true, 5, "12"));
    }

    @Test
    public void startLine_fallsBackToQueryParam() {
        assertEquals(Integer.valueOf(12), DocumentLaunchParams.resolveStartLine(false, -1, "12"));
        assertEquals(Integer.valueOf(0), DocumentLaunchParams.resolveStartLine(false, -1, "0"));
    }

    @Test
    public void startLine_invalidQueryParamDefaultsToMinusOne() {
        assertEquals(Integer.valueOf(-1), DocumentLaunchParams.resolveStartLine(false, -1, "not-a-number"));
        assertEquals(Integer.valueOf(-1), DocumentLaunchParams.resolveStartLine(false, -1, ""));
    }

    @Test
    public void startLine_noneRequestedIsNull() {
        assertNull(DocumentLaunchParams.resolveStartLine(false, -1, null));
    }

    // -- resolveStartInPreview ----------------------------------------------

    @Test
    public void startInPreview_trueWhenPreviewExtraSet() {
        assertEquals(Boolean.TRUE, DocumentLaunchParams.resolveStartInPreview(true, "notes.md"));
    }

    @Test
    public void startInPreview_trueForIndexFiles() {
        assertEquals(Boolean.TRUE, DocumentLaunchParams.resolveStartInPreview(false, "index.md"));
        assertEquals(Boolean.TRUE, DocumentLaunchParams.resolveStartInPreview(false, "index.html"));
        assertEquals(Boolean.TRUE, DocumentLaunchParams.resolveStartInPreview(false, "index."));
    }

    @Test
    public void startInPreview_nullForNonIndexNonPreview() {
        assertNull(DocumentLaunchParams.resolveStartInPreview(false, "notes.md"));
        // Must START with "index." - a substring match or missing dot is not enough
        assertNull(DocumentLaunchParams.resolveStartInPreview(false, "myindex.md"));
        assertNull(DocumentLaunchParams.resolveStartInPreview(false, "index"));
    }

    @Test
    public void startInPreview_previewExtraWinsOverNonIndexName() {
        assertEquals(Boolean.TRUE, DocumentLaunchParams.resolveStartInPreview(true, "notes.md"));
    }
}
