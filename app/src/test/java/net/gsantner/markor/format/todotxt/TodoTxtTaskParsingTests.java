package net.gsantner.markor.format.todotxt;

import static org.assertj.core.api.Assertions.assertThat;

import net.gsantner.markor.format.todotxt.TodoTxtTask.TodoDueState;

import org.junit.Test;

import java.util.Calendar;

/**
 * Coverage for todo.txt task-line parsing ({@link TodoTxtTask}): priority, done, dates, projects,
 * contexts, key:value, due status and the derived description.
 */
public class TodoTxtTaskParsingTests {

    private static String date(final int offsetDays) {
        final Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, offsetDays);
        return TodoTxtTask.DATEF_YYYY_MM_DD.format(c.getTime());
    }

    private static TodoTxtTask task(final String line) {
        return new TodoTxtTask(line);
    }

    @Test
    public void priority() {
        assertThat(task("(A) buy milk").getPriority()).isEqualTo('A');
        assertThat(task("(z) buy milk").getPriority()).isEqualTo('Z'); // normalized to upper case
        assertThat(task("buy milk").getPriority()).isEqualTo(TodoTxtTask.PRIORITY_NONE);
        // A priority marker requires the "(X) " form with a trailing space
        assertThat(task("(A)buy milk").getPriority()).isEqualTo(TodoTxtTask.PRIORITY_NONE);
    }

    @Test
    public void doneState() {
        assertThat(task("x 2020-01-01 buy milk").isDone()).isTrue();
        assertThat(task("x buy milk").isDone()).isTrue();
        assertThat(task("(A) buy milk").isDone()).isFalse();
        // A word merely starting with x is not "done"
        assertThat(task("xenophobia research").isDone()).isFalse();
    }

    @Test
    public void creationAndCompletionDates() {
        assertThat(task("(A) 2020-01-01 buy milk").getCreationDate()).isEqualTo("2020-01-01");
        assertThat(task("2020-01-01 buy milk").getCreationDate()).isEqualTo("2020-01-01");
        // "x <completion> <creation> ..." ordering
        final TodoTxtTask done = task("x 2020-02-02 2020-01-01 buy milk");
        assertThat(done.getCompletionDate()).isEqualTo("2020-02-02");
        assertThat(done.getCreationDate()).isEqualTo("2020-01-01");
        // No dates -> empty defaults
        assertThat(task("buy milk").getCreationDate()).isEqualTo("");
        assertThat(task("(A) 2020-01-01 buy milk").getCompletionDate()).isEqualTo("");
    }

    @Test
    public void dueDate() {
        assertThat(task("buy milk due:2020-12-31").getDueDate()).isEqualTo("2020-12-31");
        assertThat(task("buy milk due:2020-12-31 +shopping").getDueDate()).isEqualTo("2020-12-31");
        assertThat(task("buy milk").getDueDate()).isEqualTo("");
    }

    @Test
    public void projects() {
        assertThat(task("buy milk +home +shopping").getProjects()).containsExactly("home", "shopping");
        assertThat(task("buy milk").getProjects()).isEmpty();
    }

    @Test
    public void contexts() {
        assertThat(task("call mom @phone @family").getContexts()).containsExactly("phone", "family");
        assertThat(task("call mom").getContexts()).isEmpty();
    }

    @Test
    public void keyValuePairsDoNotCorruptOtherFields() {
        final TodoTxtTask t = task("(A) 2020-01-01 plan trip due:2020-12-31 foo:bar +travel");
        // due: is a recognized key:value and still resolves
        assertThat(t.getDueDate()).isEqualTo("2020-12-31");
        // Other structured fields parse alongside an arbitrary key:value
        assertThat(t.getPriority()).isEqualTo('A');
        assertThat(t.getCreationDate()).isEqualTo("2020-01-01");
        assertThat(t.getProjects()).containsExactly("travel");
        // key:value tokens are stripped from the description
        assertThat(t.getDescription()).doesNotContain("foo:bar").doesNotContain("due:2020-12-31");
    }

    @Test
    public void dueStatus() {
        assertThat(task("someday maybe").getDueStatus()).isEqualTo(TodoDueState.NONE);
        assertThat(task("water plants due:" + date(0)).getDueStatus()).isEqualTo(TodoDueState.TODAY);
        assertThat(task("pay rent due:" + date(-1)).getDueStatus()).isEqualTo(TodoDueState.OVERDUE);
        assertThat(task("book flight due:" + date(1)).getDueStatus()).isEqualTo(TodoDueState.FUTURE);
    }

    @Test
    public void descriptionStripsStructuredParts() {
        final TodoTxtTask t = task("(A) 2020-01-01 call mom +family @phone due:2020-12-31");
        final String description = t.getDescription();
        assertThat(description).contains("call mom");
        assertThat(description)
                .doesNotContain("(A)")
                .doesNotContain("2020-01-01")
                .doesNotContain("+family")
                .doesNotContain("@phone")
                .doesNotContain("due:2020-12-31");
    }
}
