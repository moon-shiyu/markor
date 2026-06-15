/*#######################################################
 *
 *   Maintained 2018-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.widget;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import java.util.List;

public class TodoWidgetDataTest {

    @Test
    public void parseTaskDescriptions_nullContentYieldsEmptyList() {
        assertThat(TodoWidgetData.parseTaskDescriptions(null)).isEmpty();
    }

    @Test
    public void parseTaskDescriptions_plainLinesMapToThemselves() {
        final List<String> result = TodoWidgetData.parseTaskDescriptions("Buy milk\nCall mom");
        assertThat(result).containsExactly("Buy milk", "Call mom");
    }

    @Test
    public void parseTaskDescriptions_stripsTodoTxtMetadataFromDescription() {
        final List<String> result = TodoWidgetData.parseTaskDescriptions("(A) 2026-06-15 Buy milk +shopping @home key:val");
        assertThat(result).hasSize(1);
        final String description = result.get(0);
        assertThat(description)
                .contains("Buy milk")
                .doesNotContain("(A)")
                .doesNotContain("2026-06-15")
                .doesNotContain("+shopping")
                .doesNotContain("@home")
                .doesNotContain("key:val");
    }

    @Test
    public void parseTaskDescriptions_keepsOneEntryPerLineIncludingBlank() {
        final List<String> result = TodoWidgetData.parseTaskDescriptions("task1\n\ntask2");
        assertThat(result).hasSize(3);
        assertThat(result.get(0)).contains("task1");
        assertThat(result.get(2)).contains("task2");
    }
}
