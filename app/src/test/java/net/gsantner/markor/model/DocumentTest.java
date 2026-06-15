/*#######################################################
 *
 *   Maintained 2017-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.model;

import static org.assertj.core.api.Assertions.assertThat;

import net.gsantner.opoc.util.GsFileUtils;

import org.junit.Test;

/**
 * Unit tests for {@link Document} utility methods that have no Android
 * framework dependencies.
 *
 * <p><b>Testing limitations:</b> The {@code Document} constructor depends on
 * {@code FormatRegistry.FORMATS}, which references Android resource IDs
 * ({@code R.string.*}). This prevents direct instantiation of {@code Document}
 * in JVM unit tests. Methods like {@code isEncrypted(File)} depend on
 * {@code Build.VERSION.SDK_INT}, which returns 0 under the Android stub jar.
 * These require instrumented tests or Robolectric to exercise fully.</p>
 *
 * <p>This test class covers the pure-Java static helpers and the CRC-32
 * content-comparison logic that underpins change tracking.</p>
 */
@SuppressWarnings("ALL")
public class DocumentTest {

    // -----------------------------------------------------------------------
    // Document.getMaskedContent – masks sensitive data for debug reports
    // -----------------------------------------------------------------------

    @Test
    public void getMaskedContent_masksHttpUrls() {
        final String result = Document.getMaskedContent("visit http://example.com today");
        assertThat(result).contains("https://");
        assertThat(result).doesNotContain("http://example");
    }

    @Test
    public void getMaskedContent_masksHttpsUrls() {
        final String result = Document.getMaskedContent("visit https://secret.io/path");
        assertThat(result).contains("https://");
        // The host/path should be masked to 'a' characters
        assertThat(result).doesNotContain("secret");
    }

    @Test
    public void getMaskedContent_masksWordCharacters() {
        final String result = Document.getMaskedContent("Hello World 123");
        // All \w chars become 'a', spaces preserved
        assertThat(result).isEqualTo("aaaaa aaaaa aaa");
    }

    @Test
    public void getMaskedContent_preservesNewlines() {
        final String result = Document.getMaskedContent("line1\nline2");
        assertThat(result).isEqualTo("aaaaa\naaaaa");
    }

    @Test
    public void getMaskedContent_emptyString() {
        assertThat(Document.getMaskedContent("")).isEqualTo("");
    }

    @Test
    public void getMaskedContent_urlAtEndOfLine() {
        final String result = Document.getMaskedContent("see https://x.com");
        assertThat(result).startsWith("aaa https://");
        assertThat(result).doesNotContain("x.com");
    }

    @Test
    public void getMaskedContent_multipleUrls() {
        final String input = "http://a.com and https://b.org";
        final String result = Document.getMaskedContent(input);
        // Both URLs should have protocol preserved, host masked
        assertThat(result).doesNotContain("a.com");
        assertThat(result).doesNotContain("b.org");
    }

    // -----------------------------------------------------------------------
    // CRC-32 content comparison – the basis of Document.isContentSame()
    // and Document.setContentHash(). Verifies the change-tracking primitive.
    // -----------------------------------------------------------------------

    @Test
    public void crc32_sameContent_sameHash() {
        final String content = "Hello, World!";
        assertThat(GsFileUtils.crc32(content)).isEqualTo(GsFileUtils.crc32(content));
    }

    @Test
    public void crc32_differentContent_differentHash() {
        assertThat(GsFileUtils.crc32("Hello")).isNotEqualTo(GsFileUtils.crc32("World"));
    }

    @Test
    public void crc32_emptyString_hasConsistentHash() {
        assertThat(GsFileUtils.crc32("")).isEqualTo(GsFileUtils.crc32(""));
    }

    @Test
    public void crc32_unicodeContent_differentFromAscii() {
        assertThat(GsFileUtils.crc32("café")).isNotEqualTo(GsFileUtils.crc32("cafe"));
    }

    @Test
    public void crc32_whitespaceMatters() {
        assertThat(GsFileUtils.crc32("hello ")).isNotEqualTo(GsFileUtils.crc32("hello"));
    }

    @Test
    public void crc32_newlineMatters() {
        assertThat(GsFileUtils.crc32("hello\n")).isNotEqualTo(GsFileUtils.crc32("hello"));
    }

    /**
     * Simulates the Document.isContentSame() logic:
     * same length + same CRC-32 → content considered unchanged.
     */
    @Test
    public void contentComparison_sameLengthAndCrc_meansSame() {
        final String a = "test content";
        final String b = "test content";
        assertThat(a.length()).isEqualTo(b.length());
        assertThat(GsFileUtils.crc32(a)).isEqualTo(GsFileUtils.crc32(b));
    }

    @Test
    public void contentComparison_differentLength_meansDifferent() {
        final String a = "short";
        final String b = "much longer content";
        assertThat(a.length()).isNotEqualTo(b.length());
    }

    // -----------------------------------------------------------------------
    // GsFileUtils pure helpers used by Document
    // -----------------------------------------------------------------------

    @Test
    public void getFilenameExtension_markdown() {
        assertThat(GsFileUtils.getFilenameExtension("notes.md")).isEqualTo(".md");
    }

    @Test
    public void getFilenameExtension_encrypted() {
        assertThat(GsFileUtils.getFilenameExtension("secret.md.jenc")).isEqualTo(".jenc");
    }

    @Test
    public void getFilenameExtension_noExtension() {
        assertThat(GsFileUtils.getFilenameExtension("Makefile")).isEqualTo("");
    }

    @Test
    public void getFilenameExtension_indexFile() {
        assertThat(GsFileUtils.getFilenameExtension("index.html")).isEqualTo(".html");
    }

    @Test
    public void getNameWithoutExtension_markdown() {
        assertThat(GsFileUtils.getNameWithoutExtension("notes.md")).isEqualTo("notes");
    }

    @Test
    public void getNameWithoutExtension_encrypted() {
        assertThat(GsFileUtils.getNameWithoutExtension("secret.md.jenc")).isEqualTo("secret.md");
    }

    @Test
    public void getNameWithoutExtension_indexFile() {
        assertThat(GsFileUtils.getNameWithoutExtension("index.html")).isEqualTo("index");
    }

    // -----------------------------------------------------------------------
    // Document filename utilities
    // NOTE: filenameFromContent() and normalizeFilename() use
    // android.text.TextUtils.isEmpty() and cannot run in JVM unit tests.
    // They are verified via instrumented tests or manual testing.
    // -----------------------------------------------------------------------
}
