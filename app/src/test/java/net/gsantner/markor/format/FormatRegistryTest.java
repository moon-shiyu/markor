/*#######################################################
 *
 *   Maintained 2018-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format;

import static net.gsantner.markor.format.FormatRegistry.CONVERTER_ASCIIDOC;
import static net.gsantner.markor.format.FormatRegistry.CONVERTER_CSV;
import static net.gsantner.markor.format.FormatRegistry.CONVERTER_EMBEDBINARY;
import static net.gsantner.markor.format.FormatRegistry.CONVERTER_KEYVALUE;
import static net.gsantner.markor.format.FormatRegistry.CONVERTER_MARKDOWN;
import static net.gsantner.markor.format.FormatRegistry.CONVERTER_ORGMODE;
import static net.gsantner.markor.format.FormatRegistry.CONVERTER_PLAINTEXT;
import static net.gsantner.markor.format.FormatRegistry.CONVERTER_TODOTXT;
import static net.gsantner.markor.format.FormatRegistry.CONVERTER_WIKITEXT;
import static net.gsantner.markor.format.FormatRegistry.FORMAT_ASCIIDOC;
import static net.gsantner.markor.format.FormatRegistry.FORMAT_CSV;
import static net.gsantner.markor.format.FormatRegistry.FORMAT_EMBEDBINARY;
import static net.gsantner.markor.format.FormatRegistry.FORMAT_KEYVALUE;
import static net.gsantner.markor.format.FormatRegistry.FORMAT_MARKDOWN;
import static net.gsantner.markor.format.FormatRegistry.FORMAT_ORGMODE;
import static net.gsantner.markor.format.FormatRegistry.FORMAT_PLAIN;
import static net.gsantner.markor.format.FormatRegistry.FORMAT_TODOTXT;
import static net.gsantner.markor.format.FormatRegistry.FORMAT_UNKNOWN;
import static net.gsantner.markor.format.FormatRegistry.FORMAT_WIKITEXT;
import static org.assertj.core.api.Assertions.assertThat;

import net.gsantner.markor.format.binary.EmbedBinaryTextConverter;
import net.gsantner.markor.format.todotxt.TodoTxtAutoTextFormatter;
import net.gsantner.markor.frontend.textview.AutoTextFormatter;
import net.gsantner.markor.frontend.textview.ListHandler;

import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for the format-registration layer ({@link FormatRegistry}).
 * <p>
 * These tests exercise the parts of the registry that are independent of a live Android
 * {@link android.content.Context}: format-detection ordering, converter selection, editor
 * auto-format selection, factory wiring per format, and the Markdown fallback.
 * <p>
 * Syntax highlighters and action buttons require a real Context / AppSettings to construct
 * (e.g. {@code AppSettings.get(context)} in the {@code ActionButtonBase} constructor), so they
 * are not instantiated here; instead we assert that every selectable format wires non-null
 * highlighter/action factories and that the correct format descriptor is resolved. Likewise,
 * {@code isFileSupported(...)} is only exercised with inputs recognized before the plaintext
 * converter, whose recognition reads {@code AppSettings.get(null)} and is therefore not
 * unit-testable without Android.
 */
public class FormatRegistryTest {

    private static FormatRegistry.AutoFormat autoFormatOf(final int formatId) {
        final FormatRegistry.Format fmt = FormatRegistry.getFormatById(formatId);
        assertThat(fmt).isNotNull();
        assertThat(fmt.autoFormatFactory).as("format %d should define an auto-format factory", formatId).isNotNull();
        return fmt.autoFormatFactory.create();
    }

    @Test
    public void detectionOrderIsStable() {
        final List<Integer> ids = new ArrayList<>();
        for (final FormatRegistry.Format f : FormatRegistry.FORMATS) {
            ids.add(f.format);
        }
        // Order is significant: it drives format detection by file extension / content heading.
        // FORMAT_UNKNOWN must remain the trailing catch-all.
        assertThat(ids).containsExactly(
                FORMAT_MARKDOWN,
                FORMAT_TODOTXT,
                FORMAT_CSV,
                FORMAT_WIKITEXT,
                FORMAT_KEYVALUE,
                FORMAT_ASCIIDOC,
                FORMAT_ORGMODE,
                FORMAT_EMBEDBINARY,
                FORMAT_PLAIN,
                FORMAT_UNKNOWN);
    }

    @Test
    public void converterSelectionPerFormat() {
        assertThat(FormatRegistry.getFormatById(FORMAT_MARKDOWN).converter).isSameAs(CONVERTER_MARKDOWN);
        assertThat(FormatRegistry.getFormatById(FORMAT_TODOTXT).converter).isSameAs(CONVERTER_TODOTXT);
        assertThat(FormatRegistry.getFormatById(FORMAT_CSV).converter).isSameAs(CONVERTER_CSV);
        assertThat(FormatRegistry.getFormatById(FORMAT_WIKITEXT).converter).isSameAs(CONVERTER_WIKITEXT);
        assertThat(FormatRegistry.getFormatById(FORMAT_KEYVALUE).converter).isSameAs(CONVERTER_KEYVALUE);
        assertThat(FormatRegistry.getFormatById(FORMAT_ASCIIDOC).converter).isSameAs(CONVERTER_ASCIIDOC);
        assertThat(FormatRegistry.getFormatById(FORMAT_ORGMODE).converter).isSameAs(CONVERTER_ORGMODE);
        assertThat(FormatRegistry.getFormatById(FORMAT_EMBEDBINARY).converter).isSameAs(CONVERTER_EMBEDBINARY);
        assertThat(FormatRegistry.getFormatById(FORMAT_PLAIN).converter).isSameAs(CONVERTER_PLAINTEXT);
        // The detection-only sentinel has no converter.
        assertThat(FormatRegistry.getFormatById(FORMAT_UNKNOWN).converter).isNull();
    }

    @Test
    public void highlighterAndActionFactoriesPresentForSelectableFormats() {
        final int[] selectable = {
                FORMAT_MARKDOWN, FORMAT_TODOTXT, FORMAT_CSV, FORMAT_WIKITEXT, FORMAT_KEYVALUE,
                FORMAT_ASCIIDOC, FORMAT_ORGMODE, FORMAT_EMBEDBINARY, FORMAT_PLAIN,
        };
        for (final int id : selectable) {
            final FormatRegistry.Format fmt = FormatRegistry.getFormatById(id);
            assertThat(fmt.highlighterFactory).as("highlighter factory for format %d", id).isNotNull();
            assertThat(fmt.actionsFactory).as("actions factory for format %d", id).isNotNull();
        }
        // FORMAT_UNKNOWN is detection-only and intentionally carries no component factories.
        final FormatRegistry.Format unknown = FormatRegistry.getFormatById(FORMAT_UNKNOWN);
        assertThat(unknown.highlighterFactory).isNull();
        assertThat(unknown.actionsFactory).isNull();
        assertThat(unknown.autoFormatFactory).isNull();
    }

    @Test
    public void listAwareFormatsUseAutoTextFormatterAndListHandler() {
        final int[] listAware = {FORMAT_MARKDOWN, FORMAT_CSV, FORMAT_WIKITEXT, FORMAT_ASCIIDOC, FORMAT_ORGMODE, FORMAT_PLAIN};
        for (final int id : listAware) {
            final FormatRegistry.AutoFormat autoFormat = autoFormatOf(id);
            assertThat(autoFormat.inputFilter).as("input filter for format %d", id).isInstanceOf(AutoTextFormatter.class);
            assertThat(autoFormat.textWatcher).as("text watcher for format %d", id).isInstanceOf(ListHandler.class);
        }
    }

    @Test
    public void todoTxtUsesDateInputFilterWithoutTextWatcher() {
        final FormatRegistry.AutoFormat autoFormat = autoFormatOf(FORMAT_TODOTXT);
        assertThat(autoFormat.inputFilter).isInstanceOf(TodoTxtAutoTextFormatter.class);
        assertThat(autoFormat.textWatcher).isNull();
    }

    @Test
    public void keyValueAndEmbedBinaryHaveNoAutoFormat() {
        assertThat(FormatRegistry.getFormatById(FORMAT_KEYVALUE).autoFormatFactory).isNull();
        assertThat(FormatRegistry.getFormatById(FORMAT_EMBEDBINARY).autoFormatFactory).isNull();
    }

    @Test
    public void unknownAndUnrecognizedFormatsResolveToMarkdown() {
        assertThat(FormatRegistry.resolveFormat(FORMAT_UNKNOWN).format).isEqualTo(FORMAT_MARKDOWN);
        // An id that is not present in the table also falls back to Markdown (old switch default).
        assertThat(FormatRegistry.resolveFormat(0x7FFFFFFF).format).isEqualTo(FORMAT_MARKDOWN);
        assertThat(FormatRegistry.resolveFormat(FORMAT_MARKDOWN).converter).isSameAs(CONVERTER_MARKDOWN);
        // Known formats resolve to themselves.
        assertThat(FormatRegistry.resolveFormat(FORMAT_CSV).format).isEqualTo(FORMAT_CSV);
        assertThat(FormatRegistry.resolveFormat(FORMAT_ORGMODE).format).isEqualTo(FORMAT_ORGMODE);
    }

    @Test
    public void fileExtensionRecognitionUnchanged() {
        assertThat(CONVERTER_MARKDOWN.isFileOutOfThisFormat(new File("note.md"))).isTrue();
        assertThat(CONVERTER_TODOTXT.isFileOutOfThisFormat(new File("todo.txt"))).isTrue();
        assertThat(CONVERTER_CSV.isFileOutOfThisFormat(new File("data.csv"))).isTrue();
        assertThat(CONVERTER_WIKITEXT.isFileOutOfThisFormat(new File("page.wikitext"))).isTrue();
        assertThat(CONVERTER_KEYVALUE.isFileOutOfThisFormat(new File("config.json"))).isTrue();
        assertThat(CONVERTER_ASCIIDOC.isFileOutOfThisFormat(new File("doc.adoc"))).isTrue();
        assertThat(CONVERTER_ORGMODE.isFileOutOfThisFormat(new File("notes.org"))).isTrue();
        assertThat(CONVERTER_EMBEDBINARY.isFileOutOfThisFormat(new File("photo.jpg"))).isTrue();

        // A few unambiguous cross-format negatives (extension-list based, no plaintext fallback).
        assertThat(CONVERTER_CSV.isFileOutOfThisFormat(new File("note.md"))).isFalse();
        assertThat(CONVERTER_EMBEDBINARY.isFileOutOfThisFormat(new File("note.md"))).isFalse();
        assertThat(CONVERTER_ORGMODE.isFileOutOfThisFormat(new File("data.csv"))).isFalse();
    }

    @Test
    public void embedBinaryConverterOwnsBinaryExtensions() {
        final FormatRegistry.Format embed = FormatRegistry.getFormatById(FORMAT_EMBEDBINARY);
        assertThat(embed.converter).isInstanceOf(EmbedBinaryTextConverter.class);
        // The textOnly branch of isFileSupported() skips exactly this converter instance.
        assertThat(embed.converter.isFileOutOfThisFormat(new File("clip.mp4"))).isTrue();
        assertThat(embed.converter.isFileOutOfThisFormat(new File("song.mp3"))).isTrue();
    }

    @Test
    public void isFileSupportedForFormatsRecognizedBeforePlaintext() {
        // Limited to inputs claimed before the plaintext converter; plaintext recognition reads
        // AppSettings.get(null) and cannot run without Android.
        assertThat(FormatRegistry.isFileSupported(new File("note.md"))).isTrue();
        assertThat(FormatRegistry.isFileSupported(new File("data.csv"))).isTrue();
        assertThat(FormatRegistry.isFileSupported(new File("photo.jpg"))).isTrue();
    }

    @Test
    public void externalFileDetectionUnchanged() {
        assertThat(FormatRegistry.isExternalFile(new File("document.pdf"))).isTrue();
        assertThat(FormatRegistry.isExternalFile(new File("document.PDF"))).isTrue();
        assertThat(FormatRegistry.isExternalFile(new File("note.md"))).isFalse();
    }
}
