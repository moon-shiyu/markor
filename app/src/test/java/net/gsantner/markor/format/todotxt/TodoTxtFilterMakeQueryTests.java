package net.gsantner.markor.format.todotxt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/**
 * Tests for {@link TodoTxtFilter#makeQuery} — the query-string builder.
 */
public class TodoTxtFilterMakeQueryTests {

    @Test
    public void makeQuery_ProjectKeys_AndJoined() {
        assertThat(TodoTxtFilter.makeQuery(Arrays.asList("work", "home"), true, TodoTxtFilter.TYPE.PROJECT))
                .isEqualTo("+work & +home & !done");
    }

    @Test
    public void makeQuery_ContextKeys_OrJoined() {
        assertThat(TodoTxtFilter.makeQuery(Arrays.asList("work", "home"), false, TodoTxtFilter.TYPE.CONTEXT))
                .isEqualTo("@work | @home & !done");
    }

    @Test
    public void makeQuery_PriorityKeys() {
        assertThat(TodoTxtFilter.makeQuery(Arrays.asList("A", "B"), true, TodoTxtFilter.TYPE.PRIORITY))
                .isEqualTo("pri:A & pri:B & !done");
    }

    @Test
    public void makeQuery_DueKeys() {
        assertThat(TodoTxtFilter.makeQuery(Collections.singletonList("2024-01-01"), true, TodoTxtFilter.TYPE.DUE))
                .isEqualTo("2024-01-01 & !done");
    }

    @Test
    public void makeQuery_NullKey_ProjectProducesNegation() {
        final ArrayList<String> keys = new ArrayList<>();
        keys.add(null);
        assertThat(TodoTxtFilter.makeQuery(keys, true, TodoTxtFilter.TYPE.PROJECT))
                .isEqualTo("!+ & !done");
    }

    @Test
    public void makeQuery_NullKey_ContextProducesNegation() {
        final ArrayList<String> keys = new ArrayList<>();
        keys.add(null);
        assertThat(TodoTxtFilter.makeQuery(keys, true, TodoTxtFilter.TYPE.CONTEXT))
                .isEqualTo("!@ & !done");
    }

    @Test
    public void makeQuery_NullKey_PriorityProducesNegation() {
        final ArrayList<String> keys = new ArrayList<>();
        keys.add(null);
        assertThat(TodoTxtFilter.makeQuery(keys, true, TodoTxtFilter.TYPE.PRIORITY))
                .isEqualTo("!pri & !done");
    }

    @Test
    public void makeQuery_NullKey_DueProducesNegation() {
        final ArrayList<String> keys = new ArrayList<>();
        keys.add(null);
        assertThat(TodoTxtFilter.makeQuery(keys, true, TodoTxtFilter.TYPE.DUE))
                .isEqualTo("!due & !done");
    }

    @Test
    public void makeQuery_MixedNullAndNonNullKeys() {
        final ArrayList<String> keys = new ArrayList<>();
        keys.add("work");
        keys.add(null);
        keys.add("home");
        assertThat(TodoTxtFilter.makeQuery(keys, false, TodoTxtFilter.TYPE.PROJECT))
                .isEqualTo("+work | !+ | +home & !done");
    }

    @Test
    public void makeQuery_EmptyCollection() {
        // Empty collection produces a leading space before " & !done"
        assertThat(TodoTxtFilter.makeQuery(Collections.emptyList(), true, TodoTxtFilter.TYPE.PROJECT))
                .isEqualTo(" & !done");
    }

    @Test
    public void makeQuery_SingleKey() {
        assertThat(TodoTxtFilter.makeQuery(Collections.singletonList("urgent"), true, TodoTxtFilter.TYPE.PROJECT))
                .isEqualTo("+urgent & !done");
    }

    @Test
    public void makeQuery_AlwaysAppendsNotDone() {
        final String result = TodoTxtFilter.makeQuery(Collections.singletonList("anything"), false, TodoTxtFilter.TYPE.DUE);
        assertThat(result).endsWith(" & !done");
    }
}
