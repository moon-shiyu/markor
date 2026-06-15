/*#######################################################
 *
 *   Maintained 2018-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format;

import android.content.Context;
import android.text.InputFilter;
import android.text.TextWatcher;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.gsantner.markor.R;
import net.gsantner.markor.format.asciidoc.AsciidocActionButtons;
import net.gsantner.markor.format.asciidoc.AsciidocSyntaxHighlighter;
import net.gsantner.markor.format.asciidoc.AsciidocTextConverter;
import net.gsantner.markor.format.binary.EmbedBinaryTextConverter;
import net.gsantner.markor.format.csv.CsvSyntaxHighlighter;
import net.gsantner.markor.format.csv.CsvTextConverter;
import net.gsantner.markor.format.keyvalue.KeyValueSyntaxHighlighter;
import net.gsantner.markor.format.keyvalue.KeyValueTextConverter;
import net.gsantner.markor.format.markdown.MarkdownActionButtons;
import net.gsantner.markor.format.markdown.MarkdownReplacePatternGenerator;
import net.gsantner.markor.format.markdown.MarkdownSyntaxHighlighter;
import net.gsantner.markor.format.markdown.MarkdownTextConverter;
import net.gsantner.markor.format.orgmode.OrgmodeActionButtons;
import net.gsantner.markor.format.orgmode.OrgmodeReplacePatternGenerator;
import net.gsantner.markor.format.orgmode.OrgmodeSyntaxHighlighter;
import net.gsantner.markor.format.orgmode.OrgmodeTextConverter;
import net.gsantner.markor.format.plaintext.PlaintextActionButtons;
import net.gsantner.markor.format.plaintext.PlaintextSyntaxHighlighter;
import net.gsantner.markor.format.plaintext.PlaintextTextConverter;
import net.gsantner.markor.format.todotxt.TodoTxtActionButtons;
import net.gsantner.markor.format.todotxt.TodoTxtAutoTextFormatter;
import net.gsantner.markor.format.todotxt.TodoTxtSyntaxHighlighter;
import net.gsantner.markor.format.todotxt.TodoTxtTextConverter;
import net.gsantner.markor.format.wikitext.WikitextActionButtons;
import net.gsantner.markor.format.wikitext.WikitextReplacePatternGenerator;
import net.gsantner.markor.format.wikitext.WikitextSyntaxHighlighter;
import net.gsantner.markor.format.wikitext.WikitextTextConverter;
import net.gsantner.markor.frontend.textview.AutoTextFormatter;
import net.gsantner.markor.frontend.textview.ListHandler;
import net.gsantner.markor.frontend.textview.SyntaxHighlighterBase;
import net.gsantner.markor.model.AppSettings;
import net.gsantner.markor.model.Document;
import net.gsantner.opoc.util.GsFileUtils;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FormatRegistry {
    public static final int FORMAT_UNKNOWN = 0;
    public static final int FORMAT_WIKITEXT = R.string.action_format_wikitext;
    public static final int FORMAT_MARKDOWN = R.string.action_format_markdown;
    public static final int FORMAT_CSV = R.string.action_format_csv;
    public static final int FORMAT_PLAIN = R.string.action_format_plaintext;
    public static final int FORMAT_ASCIIDOC = R.string.action_format_asciidoc;
    public static final int FORMAT_TODOTXT = R.string.action_format_todotxt;
    public static final int FORMAT_KEYVALUE = R.string.action_format_keyvalue;
    public static final int FORMAT_EMBEDBINARY = R.string.action_format_embedbinary;
    public static final int FORMAT_ORGMODE = R.string.action_format_orgmode;

    public final static MarkdownTextConverter CONVERTER_MARKDOWN = new MarkdownTextConverter();
    public final static WikitextTextConverter CONVERTER_WIKITEXT = new WikitextTextConverter();
    public final static TodoTxtTextConverter CONVERTER_TODOTXT = new TodoTxtTextConverter();
    public final static KeyValueTextConverter CONVERTER_KEYVALUE = new KeyValueTextConverter();
    public final static CsvTextConverter CONVERTER_CSV = new CsvTextConverter();
    public final static PlaintextTextConverter CONVERTER_PLAINTEXT = new PlaintextTextConverter();
    public final static AsciidocTextConverter CONVERTER_ASCIIDOC = new AsciidocTextConverter();
    public final static EmbedBinaryTextConverter CONVERTER_EMBEDBINARY = new EmbedBinaryTextConverter();
    public final static OrgmodeTextConverter CONVERTER_ORGMODE = new OrgmodeTextConverter();

    // File extensions that are known not to be supported by Markor
    private static final List<String> EXTERNAL_FILE_EXTENSIONS = Collections.singletonList(".pdf");

    // -----------------------------------------------------------------------------------------
    // Component factory interfaces
    // -----------------------------------------------------------------------------------------

    @FunctionalInterface
    public interface HighlighterFactory {
        SyntaxHighlighterBase create(AppSettings appSettings, Document document);
    }

    @FunctionalInterface
    public interface ActionButtonsFactory {
        ActionButtonBase create(Context context, Document document);
    }

    @FunctionalInterface
    public interface InputFilterFactory {
        InputFilter create();
    }

    @FunctionalInterface
    public interface TextWatcherFactory {
        TextWatcher create();
    }

    // -----------------------------------------------------------------------------------------
    // Format definition — combines metadata with component factories
    // -----------------------------------------------------------------------------------------

    public static class Format {
        public final @StringRes int format, name;
        public final String defaultExtensionWithDot;
        public final TextConverterBase converter;
        public final HighlighterFactory highlighterFactory;
        public final ActionButtonsFactory actionButtonsFactory;
        public final InputFilterFactory inputFilterFactory;
        public final TextWatcherFactory textWatcherFactory;

        public Format(@StringRes final int a_format, @StringRes final int a_name,
                      final String a_defaultFileExtension, final TextConverterBase a_converter,
                      final HighlighterFactory a_highlighterFactory,
                      final ActionButtonsFactory a_actionButtonsFactory,
                      final InputFilterFactory a_inputFilterFactory,
                      final TextWatcherFactory a_textWatcherFactory) {
            format = a_format;
            name = a_name;
            defaultExtensionWithDot = a_defaultFileExtension;
            converter = a_converter;
            highlighterFactory = a_highlighterFactory;
            actionButtonsFactory = a_actionButtonsFactory;
            inputFilterFactory = a_inputFilterFactory;
            textWatcherFactory = a_textWatcherFactory;
        }

        /** Backward-compatible constructor for metadata-only usage. */
        public Format(@StringRes final int a_format, @StringRes final int a_name,
                      final String a_defaultFileExtension, final TextConverterBase a_converter) {
            this(a_format, a_name, a_defaultFileExtension, a_converter, null, null, null, null);
        }
    }

    // Order here is used to **determine** format by its file extension and/or content heading.
    // Each entry carries both metadata and component factories so that adding a new format
    // requires only appending to this list — no separate switch statement to maintain.
    public static final List<Format> FORMATS = Arrays.asList(
            new Format(FORMAT_MARKDOWN, R.string.markdown, ".md", CONVERTER_MARKDOWN,
                    (as, d) -> new MarkdownSyntaxHighlighter(as),
                    (c, d) -> new MarkdownActionButtons(c, d),
                    () -> new AutoTextFormatter(MarkdownReplacePatternGenerator.formatPatterns),
                    () -> new ListHandler(MarkdownReplacePatternGenerator.formatPatterns)),

            new Format(FORMAT_TODOTXT, R.string.todo_txt, ".todo.txt", CONVERTER_TODOTXT,
                    (as, d) -> new TodoTxtSyntaxHighlighter(as),
                    (c, d) -> new TodoTxtActionButtons(c, d),
                    () -> new TodoTxtAutoTextFormatter(),
                    null),

            new Format(FORMAT_CSV, R.string.csv, ".csv", CONVERTER_CSV,
                    (as, d) -> new CsvSyntaxHighlighter(as),
                    (c, d) -> new PlaintextActionButtons(c, d),
                    () -> new AutoTextFormatter(MarkdownReplacePatternGenerator.formatPatterns),
                    () -> new ListHandler(MarkdownReplacePatternGenerator.formatPatterns)),

            new Format(FORMAT_WIKITEXT, R.string.wikitext, ".txt", CONVERTER_WIKITEXT,
                    (as, d) -> new WikitextSyntaxHighlighter(as),
                    (c, d) -> new WikitextActionButtons(c, d),
                    () -> new AutoTextFormatter(WikitextReplacePatternGenerator.formatPatterns),
                    () -> new ListHandler(WikitextReplacePatternGenerator.formatPatterns)),

            new Format(FORMAT_KEYVALUE, R.string.key_value, ".json", CONVERTER_KEYVALUE,
                    (as, d) -> new KeyValueSyntaxHighlighter(as),
                    (c, d) -> new PlaintextActionButtons(c, d),
                    null,
                    null),

            new Format(FORMAT_ASCIIDOC, R.string.asciidoc, ".adoc", CONVERTER_ASCIIDOC,
                    (as, d) -> new AsciidocSyntaxHighlighter(as),
                    (c, d) -> new AsciidocActionButtons(c, d),
                    () -> new AutoTextFormatter(MarkdownReplacePatternGenerator.formatPatterns),
                    () -> new ListHandler(MarkdownReplacePatternGenerator.formatPatterns)),

            new Format(FORMAT_ORGMODE, R.string.orgmode, ".org", CONVERTER_ORGMODE,
                    (as, d) -> new OrgmodeSyntaxHighlighter(as),
                    (c, d) -> new OrgmodeActionButtons(c, d),
                    () -> new AutoTextFormatter(OrgmodeReplacePatternGenerator.formatPatterns),
                    () -> new ListHandler(OrgmodeReplacePatternGenerator.formatPatterns)),

            new Format(FORMAT_EMBEDBINARY, R.string.embed_binary, ".jpg", CONVERTER_EMBEDBINARY,
                    (as, d) -> new PlaintextSyntaxHighlighter(as),
                    (c, d) -> new PlaintextActionButtons(c, d),
                    null,
                    null),

            new Format(FORMAT_PLAIN, R.string.plaintext, ".txt", CONVERTER_PLAINTEXT,
                    (as, d) -> new PlaintextSyntaxHighlighter(as, d.extension),
                    (c, d) -> new PlaintextActionButtons(c, d),
                    () -> new AutoTextFormatter(MarkdownReplacePatternGenerator.formatPatterns),
                    () -> new ListHandler(MarkdownReplacePatternGenerator.formatPatterns)),

            new Format(FORMAT_UNKNOWN, R.string.none, "", null)
    );

    /**
     * Look up a {@link Format} entry by its format ID.
     *
     * @param formatId one of the FORMAT_* constants
     * @return the matching Format, or {@code null} if not found
     */
    public static Format findFormat(final int formatId) {
        for (final Format f : FORMATS) {
            if (f.format == formatId) {
                return f;
            }
        }
        return null;
    }

    public static boolean isFileSupported(final File file, final boolean... textOnly) {
        final boolean textonly = textOnly != null && textOnly.length > 0 && textOnly[0];
        if (file != null) {
            for (final Format format : FORMATS) {
                if (textonly && format.converter instanceof EmbedBinaryTextConverter) {
                    continue;
                }
                if (format.converter != null && format.converter.isFileOutOfThisFormat(file)) {
                    return true;
                }
            }
        }
        return false;
    }

    public interface TextFormatApplier {
        void applyTextFormat(int textFormatId);
    }

    public static FormatRegistry getFormat(int formatId, @NonNull final Context context, final Document document) {
        final FormatRegistry format = new FormatRegistry();
        final AppSettings appSettings = AppSettings.get(context);

        Format def = findFormat(formatId);
        if (def == null) {
            formatId = FORMAT_MARKDOWN;
            def = findFormat(FORMAT_MARKDOWN);
        }

        format._formatId = formatId;
        format._converter = def.converter;
        format._highlighter = def.highlighterFactory != null
                ? def.highlighterFactory.create(appSettings, document) : null;
        format._textActions = def.actionButtonsFactory != null
                ? def.actionButtonsFactory.create(context, document) : null;
        format._autoFormatInputFilter = def.inputFilterFactory != null
                ? def.inputFilterFactory.create() : null;
        format._autoFormatTextWatcher = def.textWatcherFactory != null
                ? def.textWatcherFactory.create() : null;

        return format;
    }

    private ActionButtonBase _textActions;
    private SyntaxHighlighterBase _highlighter;
    private TextConverterBase _converter;
    private InputFilter _autoFormatInputFilter;
    private TextWatcher _autoFormatTextWatcher;
    private int _formatId;

    public ActionButtonBase getActions() {
        return _textActions;
    }

    public TextWatcher getAutoFormatTextWatcher() {
        return _autoFormatTextWatcher;
    }

    public InputFilter getAutoFormatInputFilter() {
        return _autoFormatInputFilter;
    }

    public SyntaxHighlighterBase getHighlighter() {
        return _highlighter;
    }

    public TextConverterBase getConverter() {
        return _converter;
    }

    public int getFormatId() {
        return _formatId;
    }

    public static boolean isExternalFile(final File file) {
        final String ext = GsFileUtils.getFilenameExtension(file).toLowerCase();
        return EXTERNAL_FILE_EXTENSIONS.contains(ext);
    }
}
