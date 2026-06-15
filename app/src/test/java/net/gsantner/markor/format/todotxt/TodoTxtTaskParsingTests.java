package net.gsantner.markor.format.todotxt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Tests for {@link TodoTxtTask} parsing and the {@link TodoTxtTask.SttTaskSimpleComparator}.
 */
public class TodoTxtTaskParsingTests {

    // ---------------------------------------------------------------------------------------
    // Priority parsing
    // ---------------------------------------------------------------------------------------

    @Test
    public void parsePriorityA() {
        assertThat(new TodoTxtTask("(A) task").getPriority()).isEqualTo('A');
    }

    @Test
    public void parsePriorityLowercase() {
        assertThat(new TodoTxtTask("(a) task").getPriority()).isEqualTo('A');
    }

    @Test
    public void parsePriorityZ() {
        assertThat(new TodoTxtTask("(Z) task").getPriority()).isEqualTo('Z');
    }

    @Test
    public void parseNoPriority() {
        assertThat(new TodoTxtTask("just a task").getPriority()).isEqualTo(TodoTxtTask.PRIORITY_NONE);
    }

    @Test
    public void parseNoPriorityWhenEmbedded() {
        // Priority must be at the start of the line
        assertThat(new TodoTxtTask("some (A) task").getPriority()).isEqualTo(TodoTxtTask.PRIORITY_NONE);
    }

    // ---------------------------------------------------------------------------------------
    // Done detection
    // ---------------------------------------------------------------------------------------

    @Test
    public void parseDoneLowercase() {
        assertThat(new TodoTxtTask("x 2024-01-15 completed task").isDone()).isTrue();
    }

    @Test
    public void parseDoneUppercase() {
        assertThat(new TodoTxtTask("X 2024-01-15 completed task").isDone()).isTrue();
    }

    @Test
    public void parseDoneWithoutDate() {
        assertThat(new TodoTxtTask("x completed task").isDone()).isTrue();
    }

    @Test
    public void parseNotDone() {
        assertThat(new TodoTxtTask("(A) active task").isDone()).isFalse();
    }

    @Test
    public void parseNotDoneWhenXNotAtStart() {
        assertThat(new TodoTxtTask("buy x-men comic").isDone()).isFalse();
    }

    // ---------------------------------------------------------------------------------------
    // Creation date parsing
    // ---------------------------------------------------------------------------------------

    @Test
    public void parseCreationDate() {
        assertThat(new TodoTxtTask("2024-03-15 buy milk").getCreationDate()).isEqualTo("2024-03-15");
    }

    @Test
    public void parseCreationDateWithPriority() {
        assertThat(new TodoTxtTask("(A) 2024-03-15 buy milk").getCreationDate()).isEqualTo("2024-03-15");
    }

    @Test
    public void parseNoCreationDate() {
        assertThat(new TodoTxtTask("(A) buy milk").getCreationDate()).isEmpty();
    }

    @Test
    public void parseNoCreationDateDefault() {
        assertThat(new TodoTxtTask("buy milk").getCreationDate("none")).isEqualTo("none");
    }

    // ---------------------------------------------------------------------------------------
    // Completion date parsing
    // ---------------------------------------------------------------------------------------

    @Test
    public void parseCompletionDate() {
        assertThat(new TodoTxtTask("x 2024-01-15 2024-01-10 done task").getCompletionDate()).isEqualTo("2024-01-15");
    }

    @Test
    public void parseNoCompletionDate() {
        assertThat(new TodoTxtTask("(A) active task").getCompletionDate()).isEmpty();
    }

    // ---------------------------------------------------------------------------------------
    // Due date parsing
    // ---------------------------------------------------------------------------------------

    @Test
    public void parseDueDate() {
        assertThat(new TodoTxtTask("task due:2024-12-31").getDueDate()).isEqualTo("2024-12-31");
    }

    @Test
    public void parseDueDateAtStart() {
        assertThat(new TodoTxtTask("due:2024-12-31 task").getDueDate()).isEqualTo("2024-12-31");
    }

    @Test
    public void parseNoDueDate() {
        assertThat(new TodoTxtTask("plain task").getDueDate()).isEmpty();
    }

    @Test
    public void parseNoDueDateDefault() {
        assertThat(new TodoTxtTask("plain task").getDueDate("none")).isEqualTo("none");
    }

    // ---------------------------------------------------------------------------------------
    // Projects parsing
    // ---------------------------------------------------------------------------------------

    @Test
    public void parseSingleProject() {
        assertThat(new TodoTxtTask("task +project1").getProjects()).containsExactly("project1");
    }

    @Test
    public void parseMultipleProjects() {
        assertThat(new TodoTxtTask("task +project1 +project2").getProjects())
                .containsExactly("project1", "project2");
    }

    @Test
    public void parseNoProjects() {
        assertThat(new TodoTxtTask("plain task").getProjects()).isEmpty();
    }

    // ---------------------------------------------------------------------------------------
    // Contexts parsing
    // ---------------------------------------------------------------------------------------

    @Test
    public void parseSingleContext() {
        assertThat(new TodoTxtTask("task @home").getContexts()).containsExactly("home");
    }

    @Test
    public void parseMultipleContexts() {
        assertThat(new TodoTxtTask("task @home @work").getContexts())
                .containsExactly("home", "work");
    }

    @Test
    public void parseNoContexts() {
        assertThat(new TodoTxtTask("plain task").getContexts()).isEmpty();
    }

    // ---------------------------------------------------------------------------------------
    // Key-value pairs
    // ---------------------------------------------------------------------------------------

    @Test
    public void parseKeyValuePairs() {
        final String text = "task due:2024-12-31 pri:A";
        final List<String> kvs = TodoTxtTask.parseAllMatches(text, TodoTxtTask.PATTERN_KEY_VALUE_PAIRS);
        assertThat(kvs).contains("due:2024-12-31", "pri:A");
    }

    // ---------------------------------------------------------------------------------------
    // Description
    // ---------------------------------------------------------------------------------------

    @Test
    public void parseDescriptionSimple() {
        assertThat(new TodoTxtTask("buy milk").getDescription().trim()).isEqualTo("buy milk");
    }

    @Test
    public void parseDescriptionStripsStructuredParts() {
        final String desc = new TodoTxtTask("(A) 2024-01-01 buy milk +groceries @store due:2024-12-31").getDescription();
        assertThat(desc.trim()).isEqualTo("buy milk");
    }

    // ---------------------------------------------------------------------------------------
    // Due status
    // ---------------------------------------------------------------------------------------

    @Test
    public void dueStatusNone() {
        assertThat(new TodoTxtTask("plain task").getDueStatus())
                .isEqualTo(TodoTxtTask.TodoDueState.NONE);
    }

    @Test
    public void dueStatusFuture() {
        assertThat(new TodoTxtTask("task due:9999-12-31").getDueStatus())
                .isEqualTo(TodoTxtTask.TodoDueState.FUTURE);
    }

    @Test
    public void dueStatusOverdue() {
        assertThat(new TodoTxtTask("task due:2000-01-01").getDueStatus())
                .isEqualTo(TodoTxtTask.TodoDueState.OVERDUE);
    }

    // ---------------------------------------------------------------------------------------
    // tasksToString
    // ---------------------------------------------------------------------------------------

    @Test
    public void tasksToStringJoinsWithNewline() {
        final List<TodoTxtTask> tasks = Arrays.asList(
                new TodoTxtTask("task one"),
                new TodoTxtTask("task two")
        );
        assertThat(TodoTxtTask.tasksToString(tasks)).isEqualTo("task one\ntask two");
    }

    @Test
    public void tasksToStringEmpty() {
        assertThat(TodoTxtTask.tasksToString(Arrays.asList())).isEmpty();
    }

    // ---------------------------------------------------------------------------------------
    // Static aggregate helpers
    // ---------------------------------------------------------------------------------------

    @Test
    public void getProjectsFromMultipleTasks() {
        final List<TodoTxtTask> tasks = Arrays.asList(
                new TodoTxtTask("task +alpha"),
                new TodoTxtTask("task +beta +alpha"),
                new TodoTxtTask("task +gamma")
        );
        assertThat(TodoTxtTask.getProjects(tasks)).containsExactly("alpha", "beta", "gamma");
    }

    @Test
    public void getContextsFromMultipleTasks() {
        final List<TodoTxtTask> tasks = Arrays.asList(
                new TodoTxtTask("task @home"),
                new TodoTxtTask("task @work @home"),
                new TodoTxtTask("task @errands")
        );
        assertThat(TodoTxtTask.getContexts(tasks)).containsExactly("errands", "home", "work");
    }

    // ---------------------------------------------------------------------------------------
    // SttTaskSimpleComparator
    // ---------------------------------------------------------------------------------------

    @Test
    public void comparatorDoneTasksAtBottom() {
        final TodoTxtTask done = new TodoTxtTask("x 2024-01-01 done task");
        final TodoTxtTask active = new TodoTxtTask("(A) active task");
        final List<TodoTxtTask> tasks = Arrays.asList(done, active);
        TodoTxtTask.sortTasks(tasks, TodoTxtTask.SttTaskSimpleComparator.BY_PRIORITY, false);
        assertThat(tasks.get(0).getLine()).isEqualTo("(A) active task");
        assertThat(tasks.get(1).getLine()).isEqualTo("x 2024-01-01 done task");
    }

    @Test
    public void comparatorByPriority() {
        final TodoTxtTask a = new TodoTxtTask("(A) high priority");
        final TodoTxtTask c = new TodoTxtTask("(C) low priority");
        final List<TodoTxtTask> tasks = Arrays.asList(c, a);
        TodoTxtTask.sortTasks(tasks, TodoTxtTask.SttTaskSimpleComparator.BY_PRIORITY, false);
        assertThat(tasks.get(0).getPriority()).isEqualTo('A');
        assertThat(tasks.get(1).getPriority()).isEqualTo('C');
    }

    @Test
    public void comparatorByDueDate() {
        final TodoTxtTask early = new TodoTxtTask("task due:2024-01-01");
        final TodoTxtTask late = new TodoTxtTask("task due:2024-12-31");
        final List<TodoTxtTask> tasks = Arrays.asList(late, early);
        TodoTxtTask.sortTasks(tasks, TodoTxtTask.SttTaskSimpleComparator.BY_DUE_DATE, false);
        assertThat(tasks.get(0).getDueDate()).isEqualTo("2024-01-01");
        assertThat(tasks.get(1).getDueDate()).isEqualTo("2024-12-31");
    }

    @Test
    public void comparatorByCreationDate() {
        final TodoTxtTask older = new TodoTxtTask("2024-01-01 old task");
        final TodoTxtTask newer = new TodoTxtTask("2024-06-15 new task");
        final List<TodoTxtTask> tasks = Arrays.asList(newer, older);
        TodoTxtTask.sortTasks(tasks, TodoTxtTask.SttTaskSimpleComparator.BY_CREATION_DATE, false);
        assertThat(tasks.get(0).getCreationDate()).isEqualTo("2024-01-01");
    }

    @Test
    public void comparatorDescending() {
        final TodoTxtTask a = new TodoTxtTask("(A) high priority");
        final TodoTxtTask c = new TodoTxtTask("(C) low priority");
        final List<TodoTxtTask> tasks = Arrays.asList(a, c);
        TodoTxtTask.sortTasks(tasks, TodoTxtTask.SttTaskSimpleComparator.BY_PRIORITY, true);
        assertThat(tasks.get(0).getPriority()).isEqualTo('C');
        assertThat(tasks.get(1).getPriority()).isEqualTo('A');
    }

    @Test
    public void comparatorByLine() {
        final TodoTxtTask b = new TodoTxtTask("banana");
        final TodoTxtTask a = new TodoTxtTask("apple");
        final List<TodoTxtTask> tasks = Arrays.asList(b, a);
        TodoTxtTask.sortTasks(tasks, TodoTxtTask.SttTaskSimpleComparator.BY_LINE, false);
        assertThat(tasks.get(0).getLine()).isEqualTo("apple");
        assertThat(tasks.get(1).getLine()).isEqualTo("banana");
    }

    // ---------------------------------------------------------------------------------------
    // Regex pattern tests
    // ---------------------------------------------------------------------------------------

    @Test
    public void patternProjectsDontMatchEmailAddress() {
        // +project at start or after whitespace only
        assertThat(TodoTxtTask.parseAllMatches("email+tag@example.com", TodoTxtTask.PATTERN_PROJECTS)).isEmpty();
    }

    @Test
    public void patternContextsDontMatchEmailAddress() {
        assertThat(TodoTxtTask.parseAllMatches("user@domain.com", TodoTxtTask.PATTERN_CONTEXTS)).isEmpty();
    }

    @Test
    public void patternProjectAtStartOfLine() {
        assertThat(TodoTxtTask.parseAllMatches("+project task", TodoTxtTask.PATTERN_PROJECTS))
                .containsExactly("project");
    }

    @Test
    public void patternContextAtStartOfLine() {
        assertThat(TodoTxtTask.parseAllMatches("@context task", TodoTxtTask.PATTERN_CONTEXTS))
                .containsExactly("context");
    }

    @Test
    public void doneTaskWithPriorityAndCompletionDate() {
        final TodoTxtTask task = new TodoTxtTask("x 2024-01-15 (B) 2024-01-10 completed +project @work");
        assertThat(task.isDone()).isTrue();
        assertThat(task.getCompletionDate()).isEqualTo("2024-01-15");
        assertThat(task.getPriority()).isEqualTo('B');
        assertThat(task.getProjects()).containsExactly("project");
        assertThat(task.getContexts()).containsExactly("work");
    }

    @Test
    public void taskWithMultipleKeyValuePairs() {
        final TodoTxtTask task = new TodoTxtTask("task due:2024-12-31 pri:A rec:1w");
        final List<String> kvs = TodoTxtTask.parseAllMatches(task.getLine(), TodoTxtTask.PATTERN_KEY_VALUE_PAIRS);
        assertThat(kvs).hasSize(3);
        assertThat(kvs).contains("due:2024-12-31", "pri:A", "rec:1w");
    }

    // ---------------------------------------------------------------------------------------
    // Parsing edge cases
    // ---------------------------------------------------------------------------------------

    @Test
    public void parseEmptyLine() {
        final TodoTxtTask task = new TodoTxtTask("");
        assertThat(task.isDone()).isFalse();
        assertThat(task.getPriority()).isEqualTo(TodoTxtTask.PRIORITY_NONE);
        assertThat(task.getProjects()).isEmpty();
        assertThat(task.getContexts()).isEmpty();
        assertThat(task.getDueDate()).isEmpty();
    }

    @Test
    public void parseWhitespaceOnly() {
        final TodoTxtTask task = new TodoTxtTask("   ");
        assertThat(task.isDone()).isFalse();
        assertThat(task.getPriority()).isEqualTo(TodoTxtTask.PRIORITY_NONE);
        assertThat(task.getProjects()).isEmpty();
        assertThat(task.getContexts()).isEmpty();
    }

    @Test
    public void parseLineWithOnlyPriority() {
        final TodoTxtTask task = new TodoTxtTask("(A)");
        assertThat(task.getPriority()).isEqualTo('A');
    }

    // ---------------------------------------------------------------------------------------
    // Static aggregate helpers (extended)
    // ---------------------------------------------------------------------------------------

    @Test
    public void getPrioritiesFromMultipleTasks() {
        final List<TodoTxtTask> tasks = Arrays.asList(
                new TodoTxtTask("(A) task1"),
                new TodoTxtTask("(C) task2"),
                new TodoTxtTask("(B) task3"),
                new TodoTxtTask("no priority task")
        );
        final List<Character> priorities = TodoTxtTask.getPriorities(tasks);
        assertThat(priorities).containsExactly('A', 'B', 'C', TodoTxtTask.PRIORITY_NONE);
    }

    @Test
    public void getDueStatesFromMultipleTasks() {
        final List<TodoTxtTask> tasks = Arrays.asList(
                new TodoTxtTask("task due:9999-12-31"),
                new TodoTxtTask("task due:2000-01-01"),
                new TodoTxtTask("plain task")
        );
        final List<TodoTxtTask.TodoDueState> states = TodoTxtTask.getDueStates(tasks);
        assertThat(states).containsExactly(
                TodoTxtTask.TodoDueState.NONE,
                TodoTxtTask.TodoDueState.OVERDUE,
                TodoTxtTask.TodoDueState.FUTURE);
    }

    // ---------------------------------------------------------------------------------------
    // SttTaskSimpleComparator — BY_CONTEXT
    // ---------------------------------------------------------------------------------------

    @Test
    public void comparatorByContext_Alphabetical() {
        final TodoTxtTask alpha = new TodoTxtTask("task @alpha");
        final TodoTxtTask beta = new TodoTxtTask("task @beta");
        final List<TodoTxtTask> tasks = Arrays.asList(beta, alpha);
        TodoTxtTask.sortTasks(tasks, TodoTxtTask.SttTaskSimpleComparator.BY_CONTEXT, false);
        assertThat(tasks.get(0).getContexts()).containsExactly("alpha");
        assertThat(tasks.get(1).getContexts()).containsExactly("beta");
    }

    @Test
    public void comparatorByContext_NoContextsLast() {
        final TodoTxtTask plain = new TodoTxtTask("plain task");
        final TodoTxtTask withCtx = new TodoTxtTask("task @alpha");
        final List<TodoTxtTask> tasks = Arrays.asList(plain, withCtx);
        TodoTxtTask.sortTasks(tasks, TodoTxtTask.SttTaskSimpleComparator.BY_CONTEXT, false);
        assertThat(tasks.get(0).getContexts()).containsExactly("alpha");
        assertThat(tasks.get(1).getContexts()).isEmpty();
    }

    // ---------------------------------------------------------------------------------------
    // SttTaskSimpleComparator — BY_PROJECT
    // ---------------------------------------------------------------------------------------

    @Test
    public void comparatorByProject_Alphabetical() {
        final TodoTxtTask alpha = new TodoTxtTask("task +alpha");
        final TodoTxtTask beta = new TodoTxtTask("task +beta");
        final List<TodoTxtTask> tasks = Arrays.asList(beta, alpha);
        TodoTxtTask.sortTasks(tasks, TodoTxtTask.SttTaskSimpleComparator.BY_PROJECT, false);
        assertThat(tasks.get(0).getProjects()).containsExactly("alpha");
        assertThat(tasks.get(1).getProjects()).containsExactly("beta");
    }

    @Test
    public void comparatorByProject_NoProjectsLast() {
        final TodoTxtTask plain = new TodoTxtTask("plain task");
        final TodoTxtTask withProj = new TodoTxtTask("task +alpha");
        final List<TodoTxtTask> tasks = Arrays.asList(plain, withProj);
        TodoTxtTask.sortTasks(tasks, TodoTxtTask.SttTaskSimpleComparator.BY_PROJECT, false);
        assertThat(tasks.get(0).getProjects()).containsExactly("alpha");
        assertThat(tasks.get(1).getProjects()).isEmpty();
    }

    // ---------------------------------------------------------------------------------------
    // SttTaskSimpleComparator — BY_DESCRIPTION
    // ---------------------------------------------------------------------------------------

    @Test
    public void comparatorByDescription_Alphabetical() {
        final TodoTxtTask apples = new TodoTxtTask("buy apples");
        final TodoTxtTask bananas = new TodoTxtTask("buy bananas");
        final List<TodoTxtTask> tasks = Arrays.asList(bananas, apples);
        TodoTxtTask.sortTasks(tasks, TodoTxtTask.SttTaskSimpleComparator.BY_DESCRIPTION, false);
        assertThat(tasks.get(0).getLine()).isEqualTo("buy apples");
        assertThat(tasks.get(1).getLine()).isEqualTo("buy bananas");
    }

    // ---------------------------------------------------------------------------------------
    // SttTaskSimpleComparator — tiebreakers
    // ---------------------------------------------------------------------------------------

    @Test
    public void comparatorTiebreaker_ByDueDate() {
        final TodoTxtTask early = new TodoTxtTask("(A) task due:2024-01-01");
        final TodoTxtTask late = new TodoTxtTask("(A) task due:2024-12-31");
        final List<TodoTxtTask> tasks = Arrays.asList(late, early);
        TodoTxtTask.sortTasks(tasks, TodoTxtTask.SttTaskSimpleComparator.BY_PRIORITY, false);
        assertThat(tasks.get(0).getDueDate()).isEqualTo("2024-01-01");
        assertThat(tasks.get(1).getDueDate()).isEqualTo("2024-12-31");
    }

    @Test
    public void comparatorTiebreaker_ByPriority() {
        final TodoTxtTask highPri = new TodoTxtTask("(A) 2024-01-01 task");
        final TodoTxtTask lowPri = new TodoTxtTask("(C) 2024-01-01 task");
        final List<TodoTxtTask> tasks = Arrays.asList(lowPri, highPri);
        TodoTxtTask.sortTasks(tasks, TodoTxtTask.SttTaskSimpleComparator.BY_CREATION_DATE, false);
        assertThat(tasks.get(0).getPriority()).isEqualTo('A');
        assertThat(tasks.get(1).getPriority()).isEqualTo('C');
    }

    // ---------------------------------------------------------------------------------------
    // SttTaskSimpleComparator — descending
    // ---------------------------------------------------------------------------------------

    @Test
    public void comparatorByContext_Descending() {
        final TodoTxtTask alpha = new TodoTxtTask("task @alpha");
        final TodoTxtTask beta = new TodoTxtTask("task @beta");
        final List<TodoTxtTask> tasks = Arrays.asList(alpha, beta);
        TodoTxtTask.sortTasks(tasks, TodoTxtTask.SttTaskSimpleComparator.BY_CONTEXT, true);
        assertThat(tasks.get(0).getContexts()).containsExactly("beta");
        assertThat(tasks.get(1).getContexts()).containsExactly("alpha");
    }

    // ---------------------------------------------------------------------------------------
    // SttTaskSimpleComparator — mutation safety (validates defensive copy fix)
    // ---------------------------------------------------------------------------------------

    @Test
    public void comparatorByContext_DoesNotMutateTaskContexts() {
        final TodoTxtTask task = new TodoTxtTask("@beta @alpha");
        assertThat(task.getContexts()).containsExactly("beta", "alpha");

        final List<TodoTxtTask> tasks = Arrays.asList(task, new TodoTxtTask("@z"));
        TodoTxtTask.sortTasks(tasks, TodoTxtTask.SttTaskSimpleComparator.BY_CONTEXT, false);

        // After sort, the original task's contexts should still be in original order
        assertThat(task.getContexts()).containsExactly("beta", "alpha");
    }

    @Test
    public void comparatorByProject_DoesNotMutateTaskProjects() {
        final TodoTxtTask task = new TodoTxtTask("+beta +alpha");
        assertThat(task.getProjects()).containsExactly("beta", "alpha");

        final List<TodoTxtTask> tasks = Arrays.asList(task, new TodoTxtTask("+z"));
        TodoTxtTask.sortTasks(tasks, TodoTxtTask.SttTaskSimpleComparator.BY_PROJECT, false);

        // After sort, the original task's projects should still be in original order
        assertThat(task.getProjects()).containsExactly("beta", "alpha");
    }

    // ---------------------------------------------------------------------------------------
    // SttTaskSimpleComparator — done ordering and case
    // ---------------------------------------------------------------------------------------

    @Test
    public void comparatorDoneTasksAlwaysLast_RegardlessOfField() {
        final TodoTxtTask done = new TodoTxtTask("x (A) done task");
        final TodoTxtTask active = new TodoTxtTask("plain active task");
        final List<TodoTxtTask> tasks = Arrays.asList(done, active);
        TodoTxtTask.sortTasks(tasks, TodoTxtTask.SttTaskSimpleComparator.BY_PRIORITY, false);
        assertThat(tasks.get(0).isDone()).isFalse();
        assertThat(tasks.get(1).isDone()).isTrue();
    }

    @Test
    public void comparatorByLine_CaseInsensitive() {
        final TodoTxtTask upper = new TodoTxtTask("Apple");
        final TodoTxtTask lower = new TodoTxtTask("banana");
        final List<TodoTxtTask> tasks = Arrays.asList(lower, upper);
        TodoTxtTask.sortTasks(tasks, TodoTxtTask.SttTaskSimpleComparator.BY_LINE, false);
        assertThat(tasks.get(0).getLine()).isEqualTo("Apple");
        assertThat(tasks.get(1).getLine()).isEqualTo("banana");
    }

    @Test
    public void comparatorByCreationDate_NoDateLast() {
        final TodoTxtTask withDate = new TodoTxtTask("2024-01-01 task with date");
        final TodoTxtTask noDate = new TodoTxtTask("task without date");
        final List<TodoTxtTask> tasks = Arrays.asList(noDate, withDate);
        TodoTxtTask.sortTasks(tasks, TodoTxtTask.SttTaskSimpleComparator.BY_CREATION_DATE, false);
        assertThat(tasks.get(0).getCreationDate()).isEqualTo("2024-01-01");
        assertThat(tasks.get(1).getCreationDate()).isEmpty();
    }
}
