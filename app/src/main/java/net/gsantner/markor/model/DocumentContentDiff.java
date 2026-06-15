/*#######################################################
 *
 *   Maintained 2017-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.model;

import net.gsantner.opoc.util.GsFileUtils;

/**
 * Pure, Android-free helpers for {@link Document}'s content change tracking.
 * <p>
 * A document keeps a lightweight "fingerprint" of the last loaded/saved content - the
 * character length plus a CRC32 - so it can cheaply detect whether the in-memory text
 * differs from what is on disk and skip unnecessary writes. This class isolates that
 * fingerprint math (the only part that is free of Android dependencies) so it can be
 * reviewed and unit tested independently. The fingerprint state itself still lives in
 * {@link Document} so its serialized form is unchanged.
 */
final class DocumentContentDiff {

    private DocumentContentDiff() {
    }

    /**
     * Length component of the fingerprint ({@code 0} for {@code null}).
     */
    static int length(final CharSequence s) {
        return s != null ? s.length() : 0;
    }

    /**
     * CRC32 component of the fingerprint ({@code 0} for {@code null}).
     */
    static long crc(final CharSequence s) {
        return s != null ? GsFileUtils.crc32(s) : 0;
    }

    /**
     * Whether {@code current} matches a previously captured fingerprint. The length is
     * compared first as a cheap guard before computing the CRC. A {@code null} input never
     * matches.
     */
    static boolean isSame(final CharSequence current, final int knownLength, final long knownCrc) {
        return current != null && current.length() == knownLength && knownCrc == GsFileUtils.crc32(current);
    }
}
