/*#######################################################
 *
 *   Maintained 2018-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.activity;

import static org.assertj.core.api.Assertions.assertThat;

import net.gsantner.opoc.wrapper.GsCallback;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

public class ShareIntoUtilsTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    // Echoes each part unchanged - lets us test the token/insertion logic without date formatting
    private static final GsCallback.s2<String, Long> ECHO = (part, time) -> part;

    // -------------------------------------------------------------------------
    // sanitize
    // -------------------------------------------------------------------------

    @Test
    public void sanitize_removesTrackingParamKeepsOthers() {
        final String result = ShareIntoUtils.sanitize("https://example.com/page?utm_source=news&id=42");
        assertThat(result).isEqualTo("https://example.com/page?id=42");
    }

    @Test
    public void sanitize_keepsNonTrackingParams() {
        final String input = "https://example.com/page?id=42&q=hello";
        assertThat(ShareIntoUtils.sanitize(input)).isEqualTo(input);
    }

    @Test
    public void sanitize_removesFbclid() {
        final String result = ShareIntoUtils.sanitize("https://example.com/p?fbclid=XYZ");
        assertThat(result).doesNotContain("fbclid").startsWith("https://example.com/p");
    }

    @Test
    public void sanitize_amazonAlsoDropsQid() {
        final String result = ShareIntoUtils.sanitize("https://www.amazon.com/dp/B001?qid=123&keywords=book");
        assertThat(result).doesNotContain("qid=123").contains("keywords=book");
    }

    @Test
    public void sanitize_nonAmazonKeepsQid() {
        // qid is only dropped for amazon links
        final String input = "https://example.com/p?qid=123";
        assertThat(ShareIntoUtils.sanitize(input)).contains("qid=123");
    }

    // -------------------------------------------------------------------------
    // composeShareText
    // -------------------------------------------------------------------------

    @Test
    public void composeShareText_urlWithSubjectPrefixesTitleAndSanitizes() {
        final String result = ShareIntoUtils.composeShareText("My Article", "https://e.com/a", true);
        assertThat(result).isEqualTo("My Article https://e.com/a");
    }

    @Test
    public void composeShareText_urlWithoutSubjectSanitizesAndKeepsNoTitle() {
        final String result = ShareIntoUtils.composeShareText(null, "https://e.com/a?utm_source=x", true);
        assertThat(result).doesNotContain("utm_source").startsWith("https://e.com/a");
    }

    @Test
    public void composeShareText_nonUrlDropsSubjectAndTrims() {
        // When the text is not a URL the subject is dropped and only the trimmed text is used
        final String result = ShareIntoUtils.composeShareText("Subject", "  just some text  ", false);
        assertThat(result).isEqualTo("just some text");
    }

    @Test
    public void composeShareText_nullTextYieldsEmpty() {
        assertThat(ShareIntoUtils.composeShareText("Subj", null, false)).isEmpty();
    }

    // -------------------------------------------------------------------------
    // splitLastSpace
    // -------------------------------------------------------------------------

    @Test
    public void splitLastSpace_titleAndTarget() {
        final String[] tt = ShareIntoUtils.splitLastSpace("my title /path/file.txt");
        assertThat(tt).containsExactly("my title", "/path/file.txt");
    }

    @Test
    public void splitLastSpace_noSpaceHasNullTitle() {
        final String[] tt = ShareIntoUtils.splitLastSpace("single");
        assertThat(tt[0]).isNull();
        assertThat(tt[1]).isEqualTo("single");
    }

    @Test
    public void splitLastSpace_trimsBeforeSplitting() {
        final String[] tt = ShareIntoUtils.splitLastSpace("  trailing space target  ");
        assertThat(tt).containsExactly("trailing space", "target");
    }

    // -------------------------------------------------------------------------
    // getLinePath
    // -------------------------------------------------------------------------

    @Test
    public void getLinePath_existingFileWithTitle() throws Exception {
        final File file = tempFolder.newFile("note.txt");
        final ShareIntoUtils.Link link = ShareIntoUtils.getLinePath("Title " + file.getAbsolutePath());
        assertThat(link).isNotNull();
        assertThat(link.title).isEqualTo("Title");
        assertThat(link.file.getAbsolutePath()).isEqualTo(file.getAbsolutePath());
    }

    @Test
    public void getLinePath_existingFileNoSpaceUsesFileName() throws Exception {
        final File file = tempFolder.newFile("note.txt");
        final ShareIntoUtils.Link link = ShareIntoUtils.getLinePath(file.getAbsolutePath());
        assertThat(link).isNotNull();
        assertThat(link.title).isEqualTo(file.getName());
    }

    @Test
    public void getLinePath_nonExistingFileReturnsNull() {
        assertThat(ShareIntoUtils.getLinePath("Title /no/such/file_zzz_98765.txt")).isNull();
    }

    // -------------------------------------------------------------------------
    // interpolateTemplate
    // -------------------------------------------------------------------------

    @Test
    public void interpolateTemplate_insertsSharedAtToken() {
        final String result = ShareIntoUtils.interpolateTemplate("PRE {{text}} POST", "X", "{{text}}", ECHO, 0L);
        assertThat(result).isEqualTo("PRE X POST");
    }

    @Test
    public void interpolateTemplate_emptyPrefixYieldsSharedOnly() {
        assertThat(ShareIntoUtils.interpolateTemplate("", "X", "{{text}}", ECHO, 0L)).isEqualTo("X");
    }

    @Test
    public void interpolateTemplate_prefixWithoutTokenAppendsShared() {
        assertThat(ShareIntoUtils.interpolateTemplate("NoToken", "X", "{{text}}", ECHO, 0L)).isEqualTo("NoTokenX");
    }

    @Test
    public void interpolateTemplate_appliesDateFormatterToEachPartWithTime() {
        final GsCallback.s2<String, Long> fmt = (part, time) -> part.replace("TIME", "T" + time);
        final String result = ShareIntoUtils.interpolateTemplate("{{text}} at TIME", "X", "{{text}}", fmt, 5L);
        assertThat(result).isEqualTo("X at T5");
    }

    // -------------------------------------------------------------------------
    // wrapForFormat
    // -------------------------------------------------------------------------

    @Test
    public void wrapForFormat_todoTxtPrefixesDateAndCollapsesNewlines() {
        assertThat(ShareIntoUtils.wrapForFormat("a\n\nb", true, "2026-06-15")).isEqualTo("2026-06-15 a b");
    }

    @Test
    public void wrapForFormat_nonTodoTxtAddsLeadingNewline() {
        assertThat(ShareIntoUtils.wrapForFormat("hello world", false, "2026-06-15")).isEqualTo("\nhello world");
    }

    @Test
    public void wrapForFormat_todoTxtCollapsesMultipleLines() {
        assertThat(ShareIntoUtils.wrapForFormat("a\nb\nc", true, "D")).isEqualTo("D a b c");
    }
}
