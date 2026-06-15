package net.gsantner.markor.format.todotxt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.Test;

import java.util.Calendar;

/**
 * Behavioral coverage for the todo.txt query language ({@link TodoTxtQuery}).
 * Complements {@link TodoTxtQuerySyntaxTests}, which focuses on the parse/evaluate primitives.
 */
public class TodoTxtQueryTests {

    private static String strip(final String in) {
        return in.replace(" ", "");
    }

    // A yyyy-MM-dd date relative to today, so due-status tests stay deterministic.
    private static String date(final int offsetDays) {
        final Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, offsetDays);
        return TodoTxtTask.DATEF_YYYY_MM_DD.format(c.getTime());
    }

    private static boolean match(final String line, final String query) {
        return TodoTxtQuery.isMatchQuery(new TodoTxtTask(line), query);
    }

    @Test
    public void matchPriority() {
        final String task = "(A) call mom +family @phone";
        assertThat(match(task, "pri")).isTrue();         // has any priority
        assertThat(match(task, "pri:A")).isTrue();       // exact priority
        assertThat(match(task, "pri:B")).isFalse();      // wrong priority
        assertThat(match("plain task", "pri")).isFalse();// no priority
        assertThat(match("plain task", "!pri")).isTrue();
    }

    @Test
    public void matchProjectAndContext() {
        final String task = "(A) call mom +family @phone";
        assertThat(match(task, "+family")).isTrue();
        assertThat(match(task, "+work")).isFalse();
        assertThat(match(task, "@phone")).isTrue();
        assertThat(match(task, "@home")).isFalse();
        // Default fallback is a case-insensitive substring match on the whole line
        assertThat(match(task, "mom")).isTrue();
        assertThat(match(task, "dad")).isFalse();
    }

    @Test
    public void matchEmptyProjectAndContext() {
        final String withBoth = "(A) call mom +family @phone";
        final String withNeither = "(B) plain task";

        // "+" / "@" mean "has any project / context"
        assertThat(match(withBoth, "+")).isTrue();
        assertThat(match(withBoth, "@")).isTrue();
        assertThat(match(withNeither, "+")).isFalse();
        assertThat(match(withNeither, "@")).isFalse();

        // "!+" / "!@" mean "has no project / context" (the filter "none" buckets)
        assertThat(match(withNeither, "!+")).isTrue();
        assertThat(match(withNeither, "!@")).isTrue();
        assertThat(match(withBoth, "!+")).isFalse();
        assertThat(match(withBoth, "!@")).isFalse();
    }

    @Test
    public void matchDueStates() {
        final String today = "water plants due:" + date(0);
        final String overdue = "pay rent due:" + date(-1);
        final String future = "book flight due:" + date(1);
        final String none = "someday maybe";

        assertThat(match(today, TodoTxtQuery.QUERY_DUE_TODAY)).isTrue();
        assertThat(match(today, TodoTxtQuery.QUERY_DUE_OVERDUE)).isFalse();
        assertThat(match(today, TodoTxtQuery.QUERY_DUE_FUTURE)).isFalse();

        assertThat(match(overdue, TodoTxtQuery.QUERY_DUE_OVERDUE)).isTrue();
        assertThat(match(overdue, TodoTxtQuery.QUERY_DUE_TODAY)).isFalse();

        assertThat(match(future, TodoTxtQuery.QUERY_DUE_FUTURE)).isTrue();
        assertThat(match(future, TodoTxtQuery.QUERY_DUE_TODAY)).isFalse();

        // "due" matches any due date; tasks without one are excluded
        assertThat(match(today, TodoTxtQuery.QUERY_DUE_ANY)).isTrue();
        assertThat(match(overdue, TodoTxtQuery.QUERY_DUE_ANY)).isTrue();
        assertThat(match(future, TodoTxtQuery.QUERY_DUE_ANY)).isTrue();
        assertThat(match(none, TodoTxtQuery.QUERY_DUE_ANY)).isFalse();
        assertThat(match(none, "!" + TodoTxtQuery.QUERY_DUE_ANY)).isTrue();
    }

    @Test
    public void matchDoneTasks() {
        final String done = "x 2020-01-01 buy milk +groceries";
        final String open = "(A) buy milk +groceries";

        assertThat(match(done, TodoTxtQuery.QUERY_DONE)).isTrue();
        assertThat(match(open, TodoTxtQuery.QUERY_DONE)).isFalse();
        assertThat(match(open, "!" + TodoTxtQuery.QUERY_DONE)).isTrue();

        // Structured parts still parse on a done task
        assertThat(match(done, "+groceries")).isTrue();
        // The default "exclude done" suffix used by makeQuery
        assertThat(match(open, "+groceries & !done")).isTrue();
        assertThat(match(done, "+groceries & !done")).isFalse();
    }

    @Test
    public void booleanOperatorsAndGrouping() {
        final String task = "(A) call mom +family @phone";

        assertThat(match(task, "+family & @phone")).isTrue();
        assertThat(match(task, "+family & @home")).isFalse();
        assertThat(match(task, "+work | @phone")).isTrue();
        assertThat(match(task, "!+work")).isTrue();
        assertThat(match(task, "!+family")).isFalse();
        assertThat(match(task, "!(+work)")).isTrue();
        assertThat(match(task, "(pri:A | pri:Z) & @phone")).isTrue();
        assertThat(match(task, "(pri:Z | pri:Y) | @home")).isFalse();
        assertThat(match(task, "!(pri:A & @home) & +family")).isTrue();
    }

    @Test
    public void parseElementsIntoExpression() {
        final TodoTxtTask task = new TodoTxtTask("(A) call mom +family @phone");
        // + present -> T, @work absent -> F, !+ -> negation of "has project"
        assertThat(TodoTxtQuery.parseQuery(task, "+ & @work | !+"))
                .isEqualTo(strip("T & F | !T"));
        // Grouping and the done keyword are preserved structurally
        assertThat(TodoTxtQuery.parseQuery(task, "(pri:A | pri:B) & !done"))
                .isEqualTo(strip("(T | F) & !F"));
    }

    @Test
    public void invalidQueriesReturnFalse() {
        final String task = "(A) call mom +family @phone";
        // Malformed queries must be swallowed (return false), never throw out of isMatchQuery
        assertThat(match(task, "pri:A & (")).isFalse();
        assertThat(match(task, "(pri:A")).isFalse();
        assertThat(match(task, "pri:A )")).isFalse();
        assertThat(match(task, "pri:A & & pri:B")).isFalse();
        assertThat(match(task, "")).isFalse();
    }

    @Test
    public void evaluateExpressionThrowsOnMalformed() {
        assertThatThrownBy(() -> TodoTxtQuery.evaluateExpression(strip("T &")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TodoTxtQuery.evaluateExpression(strip("(T")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TodoTxtQuery.evaluateExpression(strip("T F")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
