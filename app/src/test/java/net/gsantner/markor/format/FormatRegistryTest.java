/*#######################################################
 *
 *   Maintained 2025 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.format;

import static org.assertj.core.api.Assertions.assertThat;

import net.gsantner.markor.format.asciidoc.AsciidocTextConverter;
import net.gsantner.markor.format.binary.EmbedBinaryTextConverter;
import net.gsantner.markor.format.csv.CsvTextConverter;
import net.gsantner.markor.format.keyvalue.KeyValueTextConverter;
import net.gsantner.markor.format.markdown.MarkdownTextConverter;
import net.gsantner.markor.format.orgmode.OrgmodeTextConverter;
import net.gsantner.markor.format.plaintext.PlaintextTextConverter;
import net.gsantner.markor.format.todotxt.TodoTxtTextConverter;
import net.gsantner.markor.format.wikitext.WikitextTextConverter;

import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tests for {@link FormatRegistry} — covers format metadata, lookup,
 * converter wiring, component-factory presence, and detection priority.
 * <p>
 * All tests are pure JVM (no Android framework needed) because they only
 * inspect the static {@link FormatRegistry#FORMATS} list and the
 * {@link FormatRegistry#findFormat(int)} helper.
 */
public class FormatRegistryTest {

    // -------------------------------------------------------------------------------------
    // Group A: FORMATS list structure
    // -------------------------------------------------------------------------------------

    public static class RegistryStructureTests {

        @Test
        public void formatsListIsNotEmpty() {
            assertThat(FormatRegistry.FORMATS).isNotNull();
            assertThat(FormatRegistry.FORMATS).isNotEmpty();
        }

        @Test
        public void formatsListHasExpectedSize() {
            // 9 real formats + 1 UNKNOWN sentinel
            assertThat(FormatRegistry.FORMATS).hasSize(10);
        }

        @Test
        public void firstEntryIsMarkdown() {
            FormatRegistry.Format first = FormatRegistry.FORMATS.get(0);
            assertThat(first.format).isEqualTo(FormatRegistry.FORMAT_MARKDOWN);
        }

        @Test
        public void lastEntryIsUnknown() {
            FormatRegistry.Format last = FormatRegistry.FORMATS.get(FormatRegistry.FORMATS.size() - 1);
            assertThat(last.format).isEqualTo(FormatRegistry.FORMAT_UNKNOWN);
            assertThat(last.converter).isNull();
        }

        @Test
        public void allFormatIdsAreUnique() {
            Set<Integer> seen = new HashSet<>();
            for (FormatRegistry.Format f : FormatRegistry.FORMATS) {
                assertThat(seen.add(f.format))
                        .as("Duplicate format ID: %d", f.format)
                        .isTrue();
            }
        }
    }

    // -------------------------------------------------------------------------------------
    // Group B: findFormat() lookup
    // -------------------------------------------------------------------------------------

    public static class FindFormatTests {

        @Test
        public void findMarkdown() {
            FormatRegistry.Format f = FormatRegistry.findFormat(FormatRegistry.FORMAT_MARKDOWN);
            assertThat(f).isNotNull();
            assertThat(f.format).isEqualTo(FormatRegistry.FORMAT_MARKDOWN);
        }

        @Test
        public void findTodoTxt() {
            FormatRegistry.Format f = FormatRegistry.findFormat(FormatRegistry.FORMAT_TODOTXT);
            assertThat(f).isNotNull();
            assertThat(f.format).isEqualTo(FormatRegistry.FORMAT_TODOTXT);
        }

        @Test
        public void findCsv() {
            FormatRegistry.Format f = FormatRegistry.findFormat(FormatRegistry.FORMAT_CSV);
            assertThat(f).isNotNull();
            assertThat(f.format).isEqualTo(FormatRegistry.FORMAT_CSV);
        }

        @Test
        public void findWikitext() {
            FormatRegistry.Format f = FormatRegistry.findFormat(FormatRegistry.FORMAT_WIKITEXT);
            assertThat(f).isNotNull();
            assertThat(f.format).isEqualTo(FormatRegistry.FORMAT_WIKITEXT);
        }

        @Test
        public void findKeyValue() {
            FormatRegistry.Format f = FormatRegistry.findFormat(FormatRegistry.FORMAT_KEYVALUE);
            assertThat(f).isNotNull();
            assertThat(f.format).isEqualTo(FormatRegistry.FORMAT_KEYVALUE);
        }

        @Test
        public void findAsciidoc() {
            FormatRegistry.Format f = FormatRegistry.findFormat(FormatRegistry.FORMAT_ASCIIDOC);
            assertThat(f).isNotNull();
            assertThat(f.format).isEqualTo(FormatRegistry.FORMAT_ASCIIDOC);
        }

        @Test
        public void findOrgmode() {
            FormatRegistry.Format f = FormatRegistry.findFormat(FormatRegistry.FORMAT_ORGMODE);
            assertThat(f).isNotNull();
            assertThat(f.format).isEqualTo(FormatRegistry.FORMAT_ORGMODE);
        }

        @Test
        public void findEmbedBinary() {
            FormatRegistry.Format f = FormatRegistry.findFormat(FormatRegistry.FORMAT_EMBEDBINARY);
            assertThat(f).isNotNull();
            assertThat(f.format).isEqualTo(FormatRegistry.FORMAT_EMBEDBINARY);
        }

        @Test
        public void findPlaintext() {
            FormatRegistry.Format f = FormatRegistry.findFormat(FormatRegistry.FORMAT_PLAIN);
            assertThat(f).isNotNull();
            assertThat(f.format).isEqualTo(FormatRegistry.FORMAT_PLAIN);
        }

        @Test
        public void findUnknown() {
            FormatRegistry.Format f = FormatRegistry.findFormat(FormatRegistry.FORMAT_UNKNOWN);
            assertThat(f).isNotNull();
            assertThat(f.format).isEqualTo(FormatRegistry.FORMAT_UNKNOWN);
        }

        @Test
        public void findInvalidIdReturnsNull() {
            assertThat(FormatRegistry.findFormat(-1)).isNull();
            assertThat(FormatRegistry.findFormat(Integer.MAX_VALUE)).isNull();
            assertThat(FormatRegistry.findFormat(42)).isNull();
        }
    }

    // -------------------------------------------------------------------------------------
    // Group C: Converter type mapping
    // -------------------------------------------------------------------------------------

    public static class ConverterTypeTests {

        @Test
        public void markdownConverter() {
            assertThat(formatOf(FormatRegistry.FORMAT_MARKDOWN).converter)
                    .isInstanceOf(MarkdownTextConverter.class);
        }

        @Test
        public void todoTxtConverter() {
            assertThat(formatOf(FormatRegistry.FORMAT_TODOTXT).converter)
                    .isInstanceOf(TodoTxtTextConverter.class);
        }

        @Test
        public void csvConverter() {
            assertThat(formatOf(FormatRegistry.FORMAT_CSV).converter)
                    .isInstanceOf(CsvTextConverter.class);
        }

        @Test
        public void wikitextConverter() {
            assertThat(formatOf(FormatRegistry.FORMAT_WIKITEXT).converter)
                    .isInstanceOf(WikitextTextConverter.class);
        }

        @Test
        public void keyValueConverter() {
            assertThat(formatOf(FormatRegistry.FORMAT_KEYVALUE).converter)
                    .isInstanceOf(KeyValueTextConverter.class);
        }

        @Test
        public void asciidocConverter() {
            assertThat(formatOf(FormatRegistry.FORMAT_ASCIIDOC).converter)
                    .isInstanceOf(AsciidocTextConverter.class);
        }

        @Test
        public void orgmodeConverter() {
            assertThat(formatOf(FormatRegistry.FORMAT_ORGMODE).converter)
                    .isInstanceOf(OrgmodeTextConverter.class);
        }

        @Test
        public void embedBinaryConverter() {
            assertThat(formatOf(FormatRegistry.FORMAT_EMBEDBINARY).converter)
                    .isInstanceOf(EmbedBinaryTextConverter.class);
        }

        @Test
        public void plaintextConverter() {
            assertThat(formatOf(FormatRegistry.FORMAT_PLAIN).converter)
                    .isInstanceOf(PlaintextTextConverter.class);
        }

        @Test
        public void unknownConverterIsNull() {
            assertThat(formatOf(FormatRegistry.FORMAT_UNKNOWN).converter).isNull();
        }

        @Test
        public void converterSingletonsMatchFormatEntries() {
            // Each converter in FORMATS is the same object as the public static singleton
            assertThat(formatOf(FormatRegistry.FORMAT_MARKDOWN).converter).isSameAs(FormatRegistry.CONVERTER_MARKDOWN);
            assertThat(formatOf(FormatRegistry.FORMAT_TODOTXT).converter).isSameAs(FormatRegistry.CONVERTER_TODOTXT);
            assertThat(formatOf(FormatRegistry.FORMAT_CSV).converter).isSameAs(FormatRegistry.CONVERTER_CSV);
            assertThat(formatOf(FormatRegistry.FORMAT_WIKITEXT).converter).isSameAs(FormatRegistry.CONVERTER_WIKITEXT);
            assertThat(formatOf(FormatRegistry.FORMAT_KEYVALUE).converter).isSameAs(FormatRegistry.CONVERTER_KEYVALUE);
            assertThat(formatOf(FormatRegistry.FORMAT_ASCIIDOC).converter).isSameAs(FormatRegistry.CONVERTER_ASCIIDOC);
            assertThat(formatOf(FormatRegistry.FORMAT_ORGMODE).converter).isSameAs(FormatRegistry.CONVERTER_ORGMODE);
            assertThat(formatOf(FormatRegistry.FORMAT_EMBEDBINARY).converter).isSameAs(FormatRegistry.CONVERTER_EMBEDBINARY);
            assertThat(formatOf(FormatRegistry.FORMAT_PLAIN).converter).isSameAs(FormatRegistry.CONVERTER_PLAINTEXT);
        }

        private FormatRegistry.Format formatOf(int formatId) {
            FormatRegistry.Format f = FormatRegistry.findFormat(formatId);
            assertThat(f).as("Format not found for ID %d", formatId).isNotNull();
            return f;
        }
    }

    // -------------------------------------------------------------------------------------
    // Group D: Component factory null / non-null patterns
    // -------------------------------------------------------------------------------------

    public static class FactoryPresenceTests {

        @Test
        public void markdownHasAllFactories() {
            FormatRegistry.Format f = formatOf(FormatRegistry.FORMAT_MARKDOWN);
            assertThat(f.highlighterFactory).isNotNull();
            assertThat(f.actionButtonsFactory).isNotNull();
            assertThat(f.inputFilterFactory).isNotNull();
            assertThat(f.textWatcherFactory).isNotNull();
        }

        @Test
        public void todoTxtHasNoTextWatcher() {
            FormatRegistry.Format f = formatOf(FormatRegistry.FORMAT_TODOTXT);
            assertThat(f.highlighterFactory).isNotNull();
            assertThat(f.actionButtonsFactory).isNotNull();
            assertThat(f.inputFilterFactory).isNotNull();
            assertThat(f.textWatcherFactory).isNull();
        }

        @Test
        public void csvHasAllFactories() {
            FormatRegistry.Format f = formatOf(FormatRegistry.FORMAT_CSV);
            assertThat(f.highlighterFactory).isNotNull();
            assertThat(f.actionButtonsFactory).isNotNull();
            assertThat(f.inputFilterFactory).isNotNull();
            assertThat(f.textWatcherFactory).isNotNull();
        }

        @Test
        public void wikitextHasAllFactories() {
            FormatRegistry.Format f = formatOf(FormatRegistry.FORMAT_WIKITEXT);
            assertThat(f.highlighterFactory).isNotNull();
            assertThat(f.actionButtonsFactory).isNotNull();
            assertThat(f.inputFilterFactory).isNotNull();
            assertThat(f.textWatcherFactory).isNotNull();
        }

        @Test
        public void keyValueHasNoAutoFormat() {
            FormatRegistry.Format f = formatOf(FormatRegistry.FORMAT_KEYVALUE);
            assertThat(f.highlighterFactory).isNotNull();
            assertThat(f.actionButtonsFactory).isNotNull();
            assertThat(f.inputFilterFactory).isNull();
            assertThat(f.textWatcherFactory).isNull();
        }

        @Test
        public void asciidocHasAllFactories() {
            FormatRegistry.Format f = formatOf(FormatRegistry.FORMAT_ASCIIDOC);
            assertThat(f.highlighterFactory).isNotNull();
            assertThat(f.actionButtonsFactory).isNotNull();
            assertThat(f.inputFilterFactory).isNotNull();
            assertThat(f.textWatcherFactory).isNotNull();
        }

        @Test
        public void orgmodeHasAllFactories() {
            FormatRegistry.Format f = formatOf(FormatRegistry.FORMAT_ORGMODE);
            assertThat(f.highlighterFactory).isNotNull();
            assertThat(f.actionButtonsFactory).isNotNull();
            assertThat(f.inputFilterFactory).isNotNull();
            assertThat(f.textWatcherFactory).isNotNull();
        }

        @Test
        public void embedBinaryHasNoAutoFormat() {
            FormatRegistry.Format f = formatOf(FormatRegistry.FORMAT_EMBEDBINARY);
            assertThat(f.highlighterFactory).isNotNull();
            assertThat(f.actionButtonsFactory).isNotNull();
            assertThat(f.inputFilterFactory).isNull();
            assertThat(f.textWatcherFactory).isNull();
        }

        @Test
        public void plaintextHasAllFactories() {
            FormatRegistry.Format f = formatOf(FormatRegistry.FORMAT_PLAIN);
            assertThat(f.highlighterFactory).isNotNull();
            assertThat(f.actionButtonsFactory).isNotNull();
            assertThat(f.inputFilterFactory).isNotNull();
            assertThat(f.textWatcherFactory).isNotNull();
        }

        @Test
        public void unknownHasNoFactories() {
            FormatRegistry.Format f = formatOf(FormatRegistry.FORMAT_UNKNOWN);
            assertThat(f.highlighterFactory).isNull();
            assertThat(f.actionButtonsFactory).isNull();
            assertThat(f.inputFilterFactory).isNull();
            assertThat(f.textWatcherFactory).isNull();
        }

        private FormatRegistry.Format formatOf(int formatId) {
            FormatRegistry.Format f = FormatRegistry.findFormat(formatId);
            assertThat(f).as("Format not found for ID %d", formatId).isNotNull();
            return f;
        }
    }

    // -------------------------------------------------------------------------------------
    // Group E: Default extension metadata
    // -------------------------------------------------------------------------------------

    public static class DefaultExtensionTests {

        @Test
        public void markdownExtension() {
            assertThat(formatOf(FormatRegistry.FORMAT_MARKDOWN).defaultExtensionWithDot).isEqualTo(".md");
        }

        @Test
        public void todoTxtExtension() {
            assertThat(formatOf(FormatRegistry.FORMAT_TODOTXT).defaultExtensionWithDot).isEqualTo(".todo.txt");
        }

        @Test
        public void csvExtension() {
            assertThat(formatOf(FormatRegistry.FORMAT_CSV).defaultExtensionWithDot).isEqualTo(".csv");
        }

        @Test
        public void wikitextExtension() {
            assertThat(formatOf(FormatRegistry.FORMAT_WIKITEXT).defaultExtensionWithDot).isEqualTo(".txt");
        }

        @Test
        public void keyValueExtension() {
            assertThat(formatOf(FormatRegistry.FORMAT_KEYVALUE).defaultExtensionWithDot).isEqualTo(".json");
        }

        @Test
        public void asciidocExtension() {
            assertThat(formatOf(FormatRegistry.FORMAT_ASCIIDOC).defaultExtensionWithDot).isEqualTo(".adoc");
        }

        @Test
        public void orgmodeExtension() {
            assertThat(formatOf(FormatRegistry.FORMAT_ORGMODE).defaultExtensionWithDot).isEqualTo(".org");
        }

        @Test
        public void embedBinaryExtension() {
            assertThat(formatOf(FormatRegistry.FORMAT_EMBEDBINARY).defaultExtensionWithDot).isEqualTo(".jpg");
        }

        @Test
        public void plaintextExtension() {
            assertThat(formatOf(FormatRegistry.FORMAT_PLAIN).defaultExtensionWithDot).isEqualTo(".txt");
        }

        @Test
        public void unknownExtensionIsEmpty() {
            assertThat(formatOf(FormatRegistry.FORMAT_UNKNOWN).defaultExtensionWithDot).isEmpty();
        }

        private FormatRegistry.Format formatOf(int formatId) {
            FormatRegistry.Format f = FormatRegistry.findFormat(formatId);
            assertThat(f).as("Format not found for ID %d", formatId).isNotNull();
            return f;
        }
    }

    // -------------------------------------------------------------------------------------
    // Group F: Format detection priority (order of FORMATS list)
    // -------------------------------------------------------------------------------------

    public static class DetectionPriorityTests {

        @Test
        public void formatPriorityOrderIsPreserved() {
            List<FormatRegistry.Format> formats = FormatRegistry.FORMATS;
            int[] expectedOrder = {
                    FormatRegistry.FORMAT_MARKDOWN,
                    FormatRegistry.FORMAT_TODOTXT,
                    FormatRegistry.FORMAT_CSV,
                    FormatRegistry.FORMAT_WIKITEXT,
                    FormatRegistry.FORMAT_KEYVALUE,
                    FormatRegistry.FORMAT_ASCIIDOC,
                    FormatRegistry.FORMAT_ORGMODE,
                    FormatRegistry.FORMAT_EMBEDBINARY,
                    FormatRegistry.FORMAT_PLAIN,
                    FormatRegistry.FORMAT_UNKNOWN,
            };

            assertThat(formats).hasSize(expectedOrder.length);
            for (int i = 0; i < expectedOrder.length; i++) {
                assertThat(formats.get(i).format)
                        .as("Entry at index %d should have format ID %d but has %d",
                                i, expectedOrder[i], formats.get(i).format)
                        .isEqualTo(expectedOrder[i]);
            }
        }

        @Test
        public void todoTxtComesBeforeWikitext() {
            // Important: both can match .txt files, TodoTxt must be checked first
            int todoIdx = indexOf(FormatRegistry.FORMAT_TODOTXT);
            int wikiIdx = indexOf(FormatRegistry.FORMAT_WIKITEXT);
            assertThat(todoIdx).isLessThan(wikiIdx);
        }

        @Test
        public void plaintextIsSecondToLast() {
            // Plaintext is the catch-all, must come after all specific formats
            List<FormatRegistry.Format> formats = FormatRegistry.FORMATS;
            int plainIdx = indexOf(FormatRegistry.FORMAT_PLAIN);
            int unknownIdx = indexOf(FormatRegistry.FORMAT_UNKNOWN);
            assertThat(plainIdx).isEqualTo(formats.size() - 2);
            assertThat(unknownIdx).isEqualTo(formats.size() - 1);
        }

        @Test
        public void markdownIsFirst() {
            assertThat(FormatRegistry.FORMATS.get(0).format).isEqualTo(FormatRegistry.FORMAT_MARKDOWN);
        }

        private int indexOf(int formatId) {
            List<FormatRegistry.Format> formats = FormatRegistry.FORMATS;
            for (int i = 0; i < formats.size(); i++) {
                if (formats.get(i).format == formatId) return i;
            }
            throw new AssertionError("Format ID " + formatId + " not found in FORMATS list");
        }
    }

    // -------------------------------------------------------------------------------------
    // Group G: Default-to-Markdown fallback invariants
    // -------------------------------------------------------------------------------------

    public static class DefaultFallbackTests {

        @Test
        public void markdownIsFindable() {
            // Critical: getFormat() falls back to FORMAT_MARKDOWN when ID is not found.
            // This only works if FORMAT_MARKDOWN is present in the list.
            assertThat(FormatRegistry.findFormat(FormatRegistry.FORMAT_MARKDOWN)).isNotNull();
        }

        @Test
        public void markdownHasConverter() {
            assertThat(FormatRegistry.findFormat(FormatRegistry.FORMAT_MARKDOWN).converter).isNotNull();
        }

        @Test
        public void markdownHasAllComponentFactories() {
            FormatRegistry.Format md = FormatRegistry.findFormat(FormatRegistry.FORMAT_MARKDOWN);
            assertThat(md.highlighterFactory).isNotNull();
            assertThat(md.actionButtonsFactory).isNotNull();
            assertThat(md.inputFilterFactory).isNotNull();
            assertThat(md.textWatcherFactory).isNotNull();
        }
    }
}
