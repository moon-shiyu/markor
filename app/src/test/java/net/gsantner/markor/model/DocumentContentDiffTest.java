/*#######################################################
 *
 *   Maintained 2017-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import net.gsantner.opoc.util.GsFileUtils;

import org.junit.Test;

/**
 * Unit tests for {@link DocumentContentDiff}, the pure content-fingerprint logic that
 * backs {@link Document}'s change tracking.
 */
public class DocumentContentDiffTest {

    // -- length / crc components --------------------------------------------

    @Test
    public void length_nullIsZeroOthersAreCharCount() {
        assertEquals(0, DocumentContentDiff.length(null));
        assertEquals(0, DocumentContentDiff.length(""));
        assertEquals(3, DocumentContentDiff.length("abc"));
    }

    @Test
    public void crc_nullIsZero() {
        assertEquals(0L, DocumentContentDiff.crc(null));
    }

    @Test
    public void crc_matchesGsFileUtilsAndIsDeterministic() {
        assertEquals(GsFileUtils.crc32("hello world"), DocumentContentDiff.crc("hello world"));
        assertEquals(DocumentContentDiff.crc("hello world"), DocumentContentDiff.crc("hello world"));
    }

    @Test
    public void crc_differsForDifferentContent() {
        assertNotEquals(DocumentContentDiff.crc("abc"), DocumentContentDiff.crc("abd"));
    }

    // -- isSame -------------------------------------------------------------

    @Test
    public void isSame_nullNeverMatches() {
        assertFalse(DocumentContentDiff.isSame(null, 0, DocumentContentDiff.crc(null)));
        assertFalse(DocumentContentDiff.isSame(null, 0, 0L));
    }

    @Test
    public void isSame_trueForMatchingFingerprint() {
        final String content = "The quick brown fox";
        final int len = DocumentContentDiff.length(content);
        final long crc = DocumentContentDiff.crc(content);
        assertTrue(DocumentContentDiff.isSame(content, len, crc));
    }

    @Test
    public void isSame_falseWhenLengthDiffers() {
        final String content = "hello";
        final long crc = DocumentContentDiff.crc(content);
        assertFalse(DocumentContentDiff.isSame(content, 4, crc));
    }

    @Test
    public void isSame_falseWhenContentDiffersButLengthEqual() {
        // "hello" and "world" both have length 5 but different CRC -> the CRC guard catches it
        final String stored = "hello";
        final int len = DocumentContentDiff.length(stored);
        final long crc = DocumentContentDiff.crc(stored);
        assertTrue(DocumentContentDiff.isSame("hello", len, crc));
        assertFalse(DocumentContentDiff.isSame("world", len, crc));
    }
}
