package net.gsantner.markor.format.todotxt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.Test;

/**
 * Tests for {@link TodoTxtQueryEvaluator} — the query expression engine.
 * Covers:
 * <ul>
 *   <li>{@link TodoTxtQueryEvaluator#parseQuery} — element evaluation against tasks</li>
 *   <li>{@link TodoTxtQueryEvaluator#evaluateExpression} — boolean expression evaluation</li>
 *   <li>{@link TodoTxtQueryEvaluator#isMatchQuery} — end-to-end query matching</li>
 * </ul>
 */
public class TodoTxtQueryEvaluatorTests {

    private String strip(final String in) {
        return in.replace(" ", "");
    }

    // ---------------------------------------------------------------------------------------
    // parseQuery — individual element evaluation
    // ---------------------------------------------------------------------------------------

    @Test
    public void parseQueryPriorityAny() {
        assertThat(TodoTxtQueryEvaluator.parseQuery(
                new TodoTxtTask("(A) task"), "pri"))
                .isEqualTo("T");
    }

    @Test
    public void parseQueryPriorityAnyFalse() {
        assertThat(TodoTxtQueryEvaluator.parseQuery(
                new TodoTxtTask("no priority task"), "pri"))
                .isEqualTo("F");
    }

    @Test
    public void parseQueryPrioritySpecific() {
        assertThat(TodoTxtQueryEvaluator.parseQuery(
                new TodoTxtTask("(A) task"), "pri:A"))
                .isEqualTo("T");
    }

    @Test
    public void parseQueryPrioritySpecificFalse() {
        assertThat(TodoTxtQueryEvaluator.parseQuery(
                new TodoTxtTask("(B) task"), "pri:A"))
                .isEqualTo("F");
    }

    @Test
    public void parseQueryDoneTrue() {
        assertThat(TodoTxtQueryEvaluator.parseQuery(
                new TodoTxtTask("x 2024-01-01 done task"), "done"))
                .isEqualTo("T");
    }

    @Test
    public void parseQueryDoneFalse() {
        assertThat(TodoTxtQueryEvaluator.parseQuery(
                new TodoTxtTask("(A) active task"), "done"))
                .isEqualTo("F");
    }

    @Test
    public void parseQueryDueToday() {
        final String today = TodoTxtTask.getToday();
        assertThat(TodoTxtQueryEvaluator.parseQuery(
                new TodoTxtTask("task due:" + today), "due="))
                .isEqualTo("T");
    }

    @Test
    public void parseQueryDueTodayFalse() {
        assertThat(TodoTxtQueryEvaluator.parseQuery(
                new TodoTxtTask("task due:9999-12-31"), "due="))
                .isEqualTo("F");
    }

    @Test
    public void parseQueryDueOverdue() {
        assertThat(TodoTxtQueryEvaluator.parseQuery(
                new TodoTxtTask("task due:2000-01-01"), "due<"))
                .isEqualTo("T");
    }

    @Test
    public void parseQueryDueOverdueFalse() {
        assertThat(TodoTxtQueryEvaluator.parseQuery(
                new TodoTxtTask("task due:9999-12-31"), "due<"))
                .isEqualTo("F");
    }

    @Test
    public void parseQueryDueFuture() {
        assertThat(TodoTxtQueryEvaluator.parseQuery(
                new TodoTxtTask("task due:9999-12-31"), "due>"))
                .isEqualTo("T");
    }

    @Test
    public void parseQueryDueFutureFalse() {
        assertThat(TodoTxtQueryEvaluator.parseQuery(
                new TodoTxtTask("task due:2000-01-01"), "due>"))
                .isEqualTo("F");
    }

    @Test
    public void parseQueryDueAny() {
        assertThat(TodoTxtQueryEvaluator.parseQuery(
                new TodoTxtTask("task due:2024-06-15"), "due"))
                .isEqualTo("T");
    }

    @Test
    public void parseQueryDueAnyFalse() {
        assertThat(TodoTxtQueryEvaluator.parseQuery(
                new TodoTxtTask("plain task"), "due"))
                .isEqualTo("F");
    }

    @Test
    public void parseQueryContextAny() {
        assertThat(TodoTxtQueryEvaluator.parseQuery(
                new TodoTxtTask("task @home"), "@"))
                .isEqualTo("T");
    }

    @Test
    public void parseQueryContextAnyFalse() {
        assertThat(TodoTxtQueryEvaluator.parseQuery(
                new TodoTxtTask("plain task"), "@"))
                .isEqualTo("F");
    }

    @Test
    public void parseQueryContextSpecific() {
        assertThat(TodoTxtQueryEvaluator.parseQuery(
                new TodoTxtTask("task @work"), "@work"))
                .isEqualTo("T");
    }

    @Test
    public void parseQueryContextSpecificFalse() {
        assertThat(TodoTxtQueryEvaluator.parseQuery(
                new TodoTxtTask("task @home"), "@work"))
                .isEqualTo("F");
    }

    @Test
    public void parseQueryProjectAny() {
        assertThat(TodoTxtQueryEvaluator.parseQuery(
                new TodoTxtTask("task +myproject"), "+"))
                .isEqualTo("T");
    }

    @Test
    public void parseQueryProjectAnyFalse() {
        assertThat(TodoTxtQueryEvaluator.parseQuery(
                new TodoTxtTask("plain task"), "+"))
                .isEqualTo("F");
    }

    @Test
    public void parseQueryProjectSpecific() {
        assertThat(TodoTxtQueryEvaluator.parseQuery(
                new TodoTxtTask("task +myproject"), "+myproject"))
                .isEqualTo("T");
    }

    @Test
    public void parseQueryProjectSpecificFalse() {
        assertThat(TodoTxtQueryEvaluator.parseQuery(
                new TodoTxtTask("task +other"), "+myproject"))
                .isEqualTo("F");
    }

    @Test
    public void parseQueryStringFallback() {
        assertThat(TodoTxtQueryEvaluator.parseQuery(
                new TodoTxtTask("buy milk at store"), "milk"))
                .isEqualTo("T");
    }

    @Test
    public void parseQueryStringFallbackCaseInsensitive() {
        assertThat(TodoTxtQueryEvaluator.parseQuery(
                new TodoTxtTask("Buy Milk at store"), "milk"))
                .isEqualTo("T");
    }

    @Test
    public void parseQueryStringFallbackNoMatch() {
        assertThat(TodoTxtQueryEvaluator.parseQuery(
                new TodoTxtTask("buy bread"), "milk"))
                .isEqualTo("F");
    }

    // ---------------------------------------------------------------------------------------
    // parseQuery — compound expressions with operators
    // ---------------------------------------------------------------------------------------

    @Test
    public void parseQueryWithAndOperator() {
        final TodoTxtTask task = new TodoTxtTask("(A) task @home due:9999-12-31");
        assertThat(TodoTxtQueryEvaluator.parseQuery(task, "pri:A & @home"))
                .isEqualTo(strip("T & T"));
    }

    @Test
    public void parseQueryWithOrOperator() {
        final TodoTxtTask task = new TodoTxtTask("task @home");
        assertThat(TodoTxtQueryEvaluator.parseQuery(task, "@work | @home"))
                .isEqualTo(strip("F | T"));
    }

    @Test
    public void parseQueryWithNotOperator() {
        final TodoTxtTask task = new TodoTxtTask("active task");
        assertThat(TodoTxtQueryEvaluator.parseQuery(task, "!done"))
                .isEqualTo(strip("!F"));
    }

    @Test
    public void parseQueryWithParentheses() {
        final TodoTxtTask task = new TodoTxtTask("(A) task +work due:9999-12-31");
        final String query = "(pri:A | pri:B) & +work";
        assertThat(TodoTxtQueryEvaluator.parseQuery(task, query))
                .isEqualTo(strip("(T | F) & T"));
    }

    @Test
    public void parseQueryComplexExpression() {
        final String query = "(pri:A | pri:B | pri:C) & !+work & due< & due> | !+ | @";

        assertThat(TodoTxtQueryEvaluator.parseQuery(
                new TodoTxtTask("(A) 2000-01-01 go to +work"), query))
                .isEqualTo(strip("(T | F | F) & !T & F & F | !T | F"));

        assertThat(TodoTxtQueryEvaluator.parseQuery(
                new TodoTxtTask("(B) 2000-01-01 go to +work due:2000-01-01"), query))
                .isEqualTo(strip("(F | T | F) & !T & T & F | !T | F"));

        assertThat(TodoTxtQueryEvaluator.parseQuery(
                new TodoTxtTask("(D) 2000-01-01 go to @work due:9999-01-01"), query))
                .isEqualTo(strip("(F | F | F) & !F & F & T| !F | T"));
    }

    // ---------------------------------------------------------------------------------------
    // evaluateExpression — pure boolean logic
    // ---------------------------------------------------------------------------------------

    @Test
    public void evalTrue() {
        assertThat(TodoTxtQueryEvaluator.evaluateExpression("T")).isTrue();
    }

    @Test
    public void evalFalse() {
        assertThat(TodoTxtQueryEvaluator.evaluateExpression("F")).isFalse();
    }

    @Test
    public void evalNotTrue() {
        assertThat(TodoTxtQueryEvaluator.evaluateExpression("!T")).isFalse();
    }

    @Test
    public void evalNotFalse() {
        assertThat(TodoTxtQueryEvaluator.evaluateExpression("!F")).isTrue();
    }

    @Test
    public void evalNotParenthesized() {
        assertThat(TodoTxtQueryEvaluator.evaluateExpression("!(F)")).isTrue();
    }

    @Test
    public void evalNotInsideParens() {
        assertThat(TodoTxtQueryEvaluator.evaluateExpression("(!F)")).isTrue();
    }

    @Test
    public void evalNestedNotParens() {
        assertThat(TodoTxtQueryEvaluator.evaluateExpression("(!(F))")).isTrue();
    }

    @Test
    public void evalDoubleNegation() {
        assertThat(TodoTxtQueryEvaluator.evaluateExpression("!(!(F))")).isFalse();
    }

    @Test
    public void evalOr() {
        assertThat(TodoTxtQueryEvaluator.evaluateExpression(strip("T | F"))).isTrue();
    }

    @Test
    public void evalAnd() {
        assertThat(TodoTxtQueryEvaluator.evaluateExpression(strip("T & F"))).isFalse();
    }

    @Test
    public void evalOrChain() {
        assertThat(TodoTxtQueryEvaluator.evaluateExpression(strip("T | T | T | T | F"))).isTrue();
        assertThat(TodoTxtQueryEvaluator.evaluateExpression(strip("F | F | F | F | T"))).isTrue();
    }

    @Test
    public void evalAndChain() {
        assertThat(TodoTxtQueryEvaluator.evaluateExpression(strip("T & T & T & T"))).isTrue();
        assertThat(TodoTxtQueryEvaluator.evaluateExpression(strip("T & T & F & T"))).isFalse();
    }

    @Test
    public void evalComplexNested() {
        assertThat(TodoTxtQueryEvaluator.evaluateExpression(strip("!(T | F) & (T | F)"))).isFalse();
        assertThat(TodoTxtQueryEvaluator.evaluateExpression(strip("!(T | F) | (T | F)"))).isTrue();
    }

    @Test
    public void evalDeeplyNested() {
        assertThat(TodoTxtQueryEvaluator.evaluateExpression(
                strip("!(T | F | F) & (T | F) | (F & (!T) | T)")))
                .isTrue();
    }

    @Test
    public void evalMultipleNegation() {
        assertThat(TodoTxtQueryEvaluator.evaluateExpression("!!!!!!F")).isFalse();
        assertThat(TodoTxtQueryEvaluator.evaluateExpression("!!!!!!T")).isTrue();
    }

    @Test
    public void evalOperatorPrecedenceAndBindsTighter() {
        // AND binds tighter: T | T | (T & F) = T | T | F = F (last op evaluated left to right: F)
        assertThat(TodoTxtQueryEvaluator.evaluateExpression(strip("T | T | T & F"))).isFalse();
    }

    @Test
    public void evalOperatorPrecedenceMixed() {
        // F & F & F | T & T → F | T = T
        assertThat(TodoTxtQueryEvaluator.evaluateExpression(strip("F & F & F | T & T"))).isTrue();
    }

    // ---------------------------------------------------------------------------------------
    // evaluateExpression — malformed expressions
    // ---------------------------------------------------------------------------------------

    @Test
    public void evalMalformedEmpty() {
        assertThatThrownBy(() -> TodoTxtQueryEvaluator.evaluateExpression(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void evalMalformedMismatchedParens() {
        assertThatThrownBy(() -> TodoTxtQueryEvaluator.evaluateExpression("(T"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void evalMalformedDoubleOperator() {
        assertThatThrownBy(() -> TodoTxtQueryEvaluator.evaluateExpression(strip("T & & F")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void evalMalformedTrailingOperator() {
        assertThatThrownBy(() -> TodoTxtQueryEvaluator.evaluateExpression(strip("T &")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---------------------------------------------------------------------------------------
    // isMatchQuery — end-to-end
    // ---------------------------------------------------------------------------------------

    @Test
    public void isMatchQuerySimpleTrue() {
        assertThat(TodoTxtQueryEvaluator.isMatchQuery(
                new TodoTxtTask("(A) buy milk"), "pri:A"))
                .isTrue();
    }

    @Test
    public void isMatchQuerySimpleFalse() {
        assertThat(TodoTxtQueryEvaluator.isMatchQuery(
                new TodoTxtTask("(B) buy milk"), "pri:A"))
                .isFalse();
    }

    @Test
    public void isMatchQueryAndExpression() {
        assertThat(TodoTxtQueryEvaluator.isMatchQuery(
                new TodoTxtTask("(A) task @work due:9999-12-31"),
                "pri:A & @work & due>"))
                .isTrue();
    }

    @Test
    public void isMatchQueryOrExpression() {
        assertThat(TodoTxtQueryEvaluator.isMatchQuery(
                new TodoTxtTask("task @home"),
                "@work | @home"))
                .isTrue();
    }

    @Test
    public void isMatchQueryNotExpression() {
        assertThat(TodoTxtQueryEvaluator.isMatchQuery(
                new TodoTxtTask("active task"),
                "!done"))
                .isTrue();
    }

    @Test
    public void isMatchQueryParenthesized() {
        assertThat(TodoTxtQueryEvaluator.isMatchQuery(
                new TodoTxtTask("(A) task +project"),
                "(pri:A | pri:B) & +project"))
                .isTrue();
    }

    @Test
    public void isMatchQueryDoneTasks() {
        assertThat(TodoTxtQueryEvaluator.isMatchQuery(
                new TodoTxtTask("x 2024-01-01 done task"),
                "done"))
                .isTrue();
        assertThat(TodoTxtQueryEvaluator.isMatchQuery(
                new TodoTxtTask("(A) active task"),
                "done"))
                .isFalse();
    }

    @Test
    public void isMatchQueryExcludesDoneTasks() {
        // Common pattern: filter non-done tasks with a specific project
        assertThat(TodoTxtQueryEvaluator.isMatchQuery(
                new TodoTxtTask("x 2024-01-01 (A) task +work"),
                "+work & !done"))
                .isTrue();
        assertThat(TodoTxtQueryEvaluator.isMatchQuery(
                new TodoTxtTask("active task +work"),
                "+work & !done"))
                .isTrue();
        assertThat(TodoTxtQueryEvaluator.isMatchQuery(
                new TodoTxtTask("active task +other"),
                "+work & !done"))
                .isFalse();
    }

    @Test
    public void isMatchQueryDueTodayWithProject() {
        final String today = TodoTxtTask.getToday();
        assertThat(TodoTxtQueryEvaluator.isMatchQuery(
                new TodoTxtTask("task +work due:" + today),
                "due= & +work"))
                .isTrue();
    }

    @Test
    public void isMatchQueryDueOverdueWithContext() {
        assertThat(TodoTxtQueryEvaluator.isMatchQuery(
                new TodoTxtTask("task @work due:2000-01-01"),
                "due< & @work"))
                .isTrue();
    }

    @Test
    public void isMatchQueryDueFuture() {
        assertThat(TodoTxtQueryEvaluator.isMatchQuery(
                new TodoTxtTask("task due:9999-12-31"),
                "due>"))
                .isTrue();
    }

    @Test
    public void isMatchQueryStringMatch() {
        assertThat(TodoTxtQueryEvaluator.isMatchQuery(
                new TodoTxtTask("buy organic milk at the store"),
                "organic"))
                .isTrue();
    }

    @Test
    public void isMatchQueryMalformedReturnsFalse() {
        // Malformed expressions should not throw, just return false
        assertThat(TodoTxtQueryEvaluator.isMatchQuery(
                new TodoTxtTask("task"),
                "(&&)"))
                .isFalse();
    }

    @Test
    public void isMatchQueryEmptyProjectAndContext() {
        // Task with no projects: + should be false, !+ should be true
        assertThat(TodoTxtQueryEvaluator.isMatchQuery(
                new TodoTxtTask("plain task"),
                "!+"))
                .isTrue();
        // Task with no contexts: @ should be false, !@ should be true
        assertThat(TodoTxtQueryEvaluator.isMatchQuery(
                new TodoTxtTask("plain task"),
                "!@"))
                .isTrue();
    }

    @Test
    public void isMatchQueryCompletedTaskDoneStatus() {
        final TodoTxtTask done = new TodoTxtTask("x 2024-01-01 (A) 2023-12-01 task +work @home due:2024-01-01");
        assertThat(TodoTxtQueryEvaluator.isMatchQuery(done, "done")).isTrue();
        assertThat(TodoTxtQueryEvaluator.isMatchQuery(done, "!done")).isFalse();
        assertThat(TodoTxtQueryEvaluator.isMatchQuery(done, "pri:A")).isTrue();
        assertThat(TodoTxtQueryEvaluator.isMatchQuery(done, "+work")).isTrue();
        assertThat(TodoTxtQueryEvaluator.isMatchQuery(done, "@home")).isTrue();
    }

    @Test
    public void isMatchQueryComplexNestedBoolean() {
        final TodoTxtTask task = new TodoTxtTask("(A) 2024-01-01 important task +urgent @work due:9999-12-31");
        assertThat(TodoTxtQueryEvaluator.isMatchQuery(task,
                "(pri:A | pri:B) & (+urgent | +important) & @work & due>"))
                .isTrue();
    }

    // ---------------------------------------------------------------------------------------
    // Parentheses edge cases
    // ---------------------------------------------------------------------------------------

    @Test
    public void parseQuery_NestedParentheses() {
        assertThat(TodoTxtQueryEvaluator.isMatchQuery(
                new TodoTxtTask("(A) task +work"), "((pri:A))"))
                .isTrue();
    }

    @Test
    public void parseQuery_EmptyParentheses_MalformedReturnsFalse() {
        assertThat(TodoTxtQueryEvaluator.isMatchQuery(
                new TodoTxtTask("task"), "()"))
                .isFalse();
    }

    // ---------------------------------------------------------------------------------------
    // NOT operator edge cases
    // ---------------------------------------------------------------------------------------

    @Test
    public void isMatchQuery_NotNotPriority() {
        assertThat(TodoTxtQueryEvaluator.isMatchQuery(
                new TodoTxtTask("(A) task"), "!!pri:A"))
                .isTrue();
    }

    @Test
    public void isMatchQuery_NotProject_NoProject() {
        assertThat(TodoTxtQueryEvaluator.isMatchQuery(
                new TodoTxtTask("plain task"), "!+work"))
                .isTrue();
    }

    @Test
    public void isMatchQuery_NotContext_NoContext() {
        assertThat(TodoTxtQueryEvaluator.isMatchQuery(
                new TodoTxtTask("plain task"), "!@home"))
                .isTrue();
    }

    // ---------------------------------------------------------------------------------------
    // Empty project/context matching
    // ---------------------------------------------------------------------------------------

    @Test
    public void isMatchQuery_AnyProject_TaskHasNone() {
        assertThat(TodoTxtQueryEvaluator.isMatchQuery(
                new TodoTxtTask("plain task"), "+"))
                .isFalse();
    }

    @Test
    public void isMatchQuery_AnyContext_TaskHasNone() {
        assertThat(TodoTxtQueryEvaluator.isMatchQuery(
                new TodoTxtTask("plain task"), "@"))
                .isFalse();
    }

    @Test
    public void isMatchQuery_NotAnyProject_TaskHasProject() {
        assertThat(TodoTxtQueryEvaluator.isMatchQuery(
                new TodoTxtTask("task +work"), "!+"))
                .isFalse();
    }

    // ---------------------------------------------------------------------------------------
    // Due state cross-checks
    // ---------------------------------------------------------------------------------------

    @Test
    public void isMatchQuery_DueToday_OverdueReturnsFalse() {
        assertThat(TodoTxtQueryEvaluator.isMatchQuery(
                new TodoTxtTask("task due:2000-01-01"), "due="))
                .isFalse();
    }

    @Test
    public void isMatchQuery_DueOverdue_FutureReturnsFalse() {
        assertThat(TodoTxtQueryEvaluator.isMatchQuery(
                new TodoTxtTask("task due:9999-12-31"), "due<"))
                .isFalse();
    }

    // ---------------------------------------------------------------------------------------
    // Done task combined filters
    // ---------------------------------------------------------------------------------------

    @Test
    public void isMatchQuery_DoneTask_CombinedFilters() {
        final TodoTxtTask doneTask = new TodoTxtTask("x 2024-01-01 task +work");
        assertThat(TodoTxtQueryEvaluator.isMatchQuery(doneTask, "done")).isTrue();
        assertThat(TodoTxtQueryEvaluator.isMatchQuery(doneTask, "+work")).isTrue();
        assertThat(TodoTxtQueryEvaluator.isMatchQuery(doneTask, "+work & !done")).isFalse();
    }

    // ---------------------------------------------------------------------------------------
    // Malformed expressions
    // ---------------------------------------------------------------------------------------

    @Test
    public void isMatchQuery_MalformedTripleBang() {
        assertThat(TodoTxtQueryEvaluator.isMatchQuery(
                new TodoTxtTask("task"), "!!!"))
                .isFalse();
    }

    @Test
    public void isMatchQuery_MalformedParensWithOp() {
        assertThat(TodoTxtQueryEvaluator.isMatchQuery(
                new TodoTxtTask("task"), "(&)"))
                .isFalse();
    }

    // ---------------------------------------------------------------------------------------
    // String fallback with special characters
    // ---------------------------------------------------------------------------------------

    @Test
    public void isMatchQuery_StringMatchWithRegexSpecialChars() {
        // The fallback uses String.contains (not regex), so .* is treated literally
        assertThat(TodoTxtQueryEvaluator.isMatchQuery(
                new TodoTxtTask("buy organic milk"), "organic"))
                .isTrue();
        // The parentheses in "(organic)" are preprocessed into syntax chars,
        // so the inner "organic" token is still evaluated as a string match
        assertThat(TodoTxtQueryEvaluator.isMatchQuery(
                new TodoTxtTask("buy (organic) milk"), "organic"))
                .isTrue();
    }

    @Test
    public void isMatchQuery_EmptyQueryString() {
        // Empty query produces an empty expression which is malformed → false
        assertThat(TodoTxtQueryEvaluator.isMatchQuery(
                new TodoTxtTask("task"), ""))
                .isFalse();
    }
}
