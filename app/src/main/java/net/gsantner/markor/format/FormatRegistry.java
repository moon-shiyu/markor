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
import androidx.annotation.Nullable;
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
import net.gsantner.opoc.util.GsCollectionUtils;
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

    // Per-format component factories. Highlighters and action buttons depend on a live Context /
    // AppSettings / Document, so unlike the (stateless, shared) converters they cannot be cached
    // and are created lazily through these factories when a format is selected.
    public interface HighlighterFactory {
        SyntaxHighlighterBase create(AppSettings appSettings, Document document);
    }

    public interface ActionsFactory {
        ActionButtonBase create(Context context, Document document);
    }

    public interface AutoFormatFactory {
        AutoFormat create();
    }

    /**
     * Editor auto-formatting bundle for a format. The {@link InputFilter} reacts while the user
     * types (e.g. continuing a list on Enter) and the {@link TextWatcher} reacts to text changes
     * (e.g. renumbering ordered lists). Both are optional and are kept together because, for the
     * list-based formats, they are derived from the same set of prefix patterns.
     */
    public static final class AutoFormat {
        public final @Nullable InputFilter inputFilter;
        public final @Nullable TextWatcher textWatcher;

        public AutoFormat(final @Nullable InputFilter inputFilter, final @Nullable TextWatcher textWatcher) {
            this.inputFilter = inputFilter;
            this.textWatcher = textWatcher;
        }
    }

    // Common auto-format wiring shared by the list-aware formats (Markdown, CSV, AsciiDoc,
    // Plaintext, Wikitext, Orgmode): an AutoTextFormatter input filter plus a ListHandler text
    // watcher, both built from the same prefix patterns.
    private static AutoFormatFactory patternAutoFormat(final AutoTextFormatter.FormatPatterns patterns) {
        return () -> new AutoFormat(new AutoTextFormatter(patterns), new ListHandler(patterns));
    }

    public static class Format {
        public final @StringRes int format, name;
        public final String defaultExtensionWithDot;
        public final TextConverterBase converter;
        // Null for the FORMAT_UNKNOWN sentinel, which only participates in format detection.
        public final @Nullable HighlighterFactory highlighterFactory;
        public final @Nullable ActionsFactory actionsFactory;
        // Null when a format has no editor auto-formatting (e.g. KeyValue, EmbedBinary).
        public final @Nullable AutoFormatFactory autoFormatFactory;

        public Format(@StringRes final int a_format, @StringRes final int a_name, final String a_defaultFileExtension, final TextConverterBase a_converter,
                      final @Nullable HighlighterFactory a_highlighterFactory, final @Nullable ActionsFactory a_actionsFactory, final @Nullable AutoFormatFactory a_autoFormatFactory) {
            format = a_format;
            name = a_name;
            defaultExtensionWithDot = a_defaultFileExtension;
            converter = a_converter;
            highlighterFactory = a_highlighterFactory;
            actionsFactory = a_actionsFactory;
            autoFormatFactory = a_autoFormatFactory;
        }

        // Metadata-only descriptor used for detection-only entries such as FORMAT_UNKNOWN.
        public Format(@StringRes final int a_format, @StringRes final int a_name, final String a_defaultFileExtension, final TextConverterBase a_converter) {
            this(a_format, a_name, a_defaultFileExtension, a_converter, null, null, null);
        }
    }

    // Order here is used to **determine** format by it's file extension and/or content heading.
    // Each entry also carries the component factories used when the format is opened in the editor,
    // so adding a format or changing the detection order only touches this single table.
    public static final List<Format> FORMATS = Arrays.asList(
            new Format(FormatRegistry.FORMAT_MARKDOWN, R.string.markdown, ".md", CONVERTER_MARKDOWN,
                    (as, doc) -> new MarkdownSyntaxHighlighter(as),
                    (ctx, doc) -> new MarkdownActionButtons(ctx, doc),
                    patternAutoFormat(MarkdownReplacePatternGenerator.formatPatterns)),
            new Format(FormatRegistry.FORMAT_TODOTXT, R.string.todo_txt, ".todo.txt", CONVERTER_TODOTXT,
                    (as, doc) -> new TodoTxtSyntaxHighlighter(as),
                    (ctx, doc) -> new TodoTxtActionButtons(ctx, doc),
                    () -> new AutoFormat(new TodoTxtAutoTextFormatter(), null)),
            new Format(FormatRegistry.FORMAT_CSV, R.string.csv, ".csv", CONVERTER_CSV,
                    (as, doc) -> new CsvSyntaxHighlighter(as),
                    // TODO k3b ???? CSV currently reuses the plaintext action buttons.
                    (ctx, doc) -> new PlaintextActionButtons(ctx, doc),
                    patternAutoFormat(MarkdownReplacePatternGenerator.formatPatterns)),
            new Format(FormatRegistry.FORMAT_WIKITEXT, R.string.wikitext, ".txt", CONVERTER_WIKITEXT,
                    (as, doc) -> new WikitextSyntaxHighlighter(as),
                    (ctx, doc) -> new WikitextActionButtons(ctx, doc),
                    patternAutoFormat(WikitextReplacePatternGenerator.formatPatterns)),
            new Format(FormatRegistry.FORMAT_KEYVALUE, R.string.key_value, ".json", CONVERTER_KEYVALUE,
                    (as, doc) -> new KeyValueSyntaxHighlighter(as),
                    (ctx, doc) -> new PlaintextActionButtons(ctx, doc),
                    null),
            new Format(FormatRegistry.FORMAT_ASCIIDOC, R.string.asciidoc, ".adoc", CONVERTER_ASCIIDOC,
                    (as, doc) -> new AsciidocSyntaxHighlighter(as),
                    (ctx, doc) -> new AsciidocActionButtons(ctx, doc),
                    patternAutoFormat(MarkdownReplacePatternGenerator.formatPatterns)),
            new Format(FormatRegistry.FORMAT_ORGMODE, R.string.orgmode, ".org", CONVERTER_ORGMODE,
                    (as, doc) -> new OrgmodeSyntaxHighlighter(as),
                    (ctx, doc) -> new OrgmodeActionButtons(ctx, doc),
                    patternAutoFormat(OrgmodeReplacePatternGenerator.formatPatterns)),
            new Format(FormatRegistry.FORMAT_EMBEDBINARY, R.string.embed_binary, ".jpg", CONVERTER_EMBEDBINARY,
                    (as, doc) -> new PlaintextSyntaxHighlighter(as),
                    (ctx, doc) -> new PlaintextActionButtons(ctx, doc),
                    null),
            new Format(FormatRegistry.FORMAT_PLAIN, R.string.plaintext, ".txt", CONVERTER_PLAINTEXT,
                    (as, doc) -> new PlaintextSyntaxHighlighter(as, doc.extension),
                    (ctx, doc) -> new PlaintextActionButtons(ctx, doc),
                    patternAutoFormat(MarkdownReplacePatternGenerator.formatPatterns)),
            new Format(FormatRegistry.FORMAT_UNKNOWN, R.string.none, "", null)
    );

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

    /**
     * Look up the {@link Format} descriptor whose id equals {@code formatId}, or {@code null} when
     * no entry matches. The result is the raw table entry; {@link #resolveFormat(int)} additionally
     * applies the Markdown fallback used when opening a document.
     */
    public static @Nullable Format getFormatById(final int formatId) {
        return GsCollectionUtils.selectFirst(FORMATS, f -> f.format == formatId);
    }

    /**
     * Resolve a format id to the descriptor that should drive the editor. Unknown ids, the
     * FORMAT_UNKNOWN sentinel, and detection-only entries (no component factories) all fall back to
     * Markdown, mirroring the previous {@code default} switch branch. Never returns {@code null}.
     */
    public static @NonNull Format resolveFormat(final int formatId) {
        final Format format = getFormatById(formatId);
        if (format != null && format.highlighterFactory != null) {
            return format;
        }
        return getFormatById(FORMAT_MARKDOWN);
    }

    public static FormatRegistry getFormat(final int formatId, @NonNull final Context context, final Document document) {
        final AppSettings appSettings = AppSettings.get(context);
        final Format fmt = resolveFormat(formatId);
        final AutoFormat autoFormat = fmt.autoFormatFactory != null ? fmt.autoFormatFactory.create() : null;

        final FormatRegistry format = new FormatRegistry();
        format._formatId = fmt.format;
        format._converter = fmt.converter;
        format._highlighter = fmt.highlighterFactory.create(appSettings, document);
        format._textActions = fmt.actionsFactory.create(context, document);
        format._autoFormatInputFilter = autoFormat != null ? autoFormat.inputFilter : null;
        format._autoFormatTextWatcher = autoFormat != null ? autoFormat.textWatcher : null;
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
