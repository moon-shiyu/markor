package net.gsantner.markor.format.todotxt;

import android.content.Context;
import android.util.Pair;

import net.gsantner.markor.R;
import net.gsantner.markor.format.todotxt.TodoTxtTask.TodoDueState;
import net.gsantner.opoc.wrapper.GsCallback;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Filtering of todo.txt tasks: gathering selectable filter keys (projects / contexts / priorities /
 * due states) with their counts, and turning a selection into a query string.
 * <p>
 * This is the public entry point used by the UI. The two cross-cutting concerns it used to own are
 * delegated to focused classes:
 * <ul>
 *     <li>{@link TodoTxtQuery} — parsing and evaluating the query language ({@link #isMatchQuery}).</li>
 *     <li>{@link TodoTxtFilterStorage} — persisting saved filter views
 *     ({@link #saveFilter}/{@link #loadSavedFilters}/{@link #deleteFilterIndex}).</li>
 * </ul>
 */
public class TodoTxtFilter {

    public static final String STRING_NONE = "-";
    private static final String NULL_SENTINEL = "NULL SENTINEL";

    // Re-exported from TodoTxtFilterStorage so existing callers keep a single entry point.
    public static final String SAVED_TODO_VIEWS = TodoTxtFilterStorage.SAVED_TODO_VIEWS;
    public static final int MAX_RECENT_VIEWS = TodoTxtFilterStorage.MAX_RECENT_VIEWS;

    public enum TYPE {
        PROJECT, CONTEXT, PRIORITY, DUE
    }

    public static class SttFilterKey {
        public final String key;    // Name
        public final int count;     // How many exist
        public final String query;  // What to stick in the query

        private SttFilterKey(String k, int c, String q) {
            key = k;
            count = c;
            query = q;
        }
    }

    public static List<SttFilterKey> getKeys(final Context context, final List<TodoTxtTask> tasks, final TYPE type) {
        if (type == TYPE.PROJECT) {
            return getStringListKeys(tasks, TodoTxtTask::getProjects);
        } else if (type == TYPE.CONTEXT) {
            return getStringListKeys(tasks, TodoTxtTask::getContexts);
        } else if (type == TYPE.PRIORITY) {
            return getStringListKeys(tasks, t -> t.getPriority() == TodoTxtTask.PRIORITY_NONE ? null : Collections.singletonList(Character.toString(Character.toUpperCase(t.getPriority()))));
        } else if (type == TYPE.DUE) {
            return getDueKeys(context, tasks);
        } else {
            return Collections.emptyList();
        }
    }

    private static List<SttFilterKey> getStringListKeys(final List<TodoTxtTask> tasks, final GsCallback.r1<List<String>, TodoTxtTask> keyGetter) {
        final List<String> all = new ArrayList<>();
        for (final TodoTxtTask task : tasks) {
            if (!task.isDone()) {
                final List<String> tKeys = keyGetter.callback(task);
                all.addAll(tKeys == null || tKeys.isEmpty() ? Collections.singletonList(NULL_SENTINEL) : tKeys);
            }
        }

        final List<SttFilterKey> keys = new ArrayList<>();
        final Set<String> unique = new TreeSet<>(all);
        if (unique.remove(NULL_SENTINEL)) {
            keys.add(new SttFilterKey(STRING_NONE, Collections.frequency(all, NULL_SENTINEL), null));
        }
        for (final String key : unique) {
            keys.add(new SttFilterKey(key, Collections.frequency(all, key), key));
        }

        return keys;
    }

    public static List<SttFilterKey> getDueKeys(final Context context, final List<TodoTxtTask> tasks) {
        final List<TodoTxtTask.TodoDueState> all = new ArrayList<>();
        for (final TodoTxtTask task : tasks) {
            all.add(task.getDueStatus());
        }
        final List<SttFilterKey> keys = new ArrayList<>();
        keys.add(new SttFilterKey(context.getString(R.string.due_future), Collections.frequency(all, TodoDueState.FUTURE), TodoTxtQuery.QUERY_DUE_FUTURE));
        keys.add(new SttFilterKey(context.getString(R.string.due_today), Collections.frequency(all, TodoDueState.TODAY), TodoTxtQuery.QUERY_DUE_TODAY));
        keys.add(new SttFilterKey(context.getString(R.string.due_overdue), Collections.frequency(all, TodoDueState.OVERDUE), TodoTxtQuery.QUERY_DUE_OVERDUE));
        keys.add(new SttFilterKey(STRING_NONE, Collections.frequency(all, TodoDueState.NONE), null));

        return keys;
    }

    // Convert a set of query keys into a formatted query
    public static String makeQuery(final Collection<String> keys, final boolean isAnd, final TodoTxtFilter.TYPE type) {
        final String prefix;
        final String nullKey;
        if (type == TYPE.CONTEXT) {
            nullKey = "!@";
            prefix = "@";
        } else if (type == TYPE.PROJECT) {
            nullKey = "!+";
            prefix = "+";
        } else if (type == TYPE.PRIORITY) {
            nullKey = "!" + TodoTxtQuery.QUERY_PRIORITY_ANY;
            prefix = "pri:";
        } else { // type due
            nullKey = "!" + TodoTxtQuery.QUERY_DUE_ANY;
            prefix = "";
        }

        final List<String> adjusted = new ArrayList<>();
        for (final String key : keys) {
            if (key != null) {
                adjusted.add(prefix + key);
            } else {
                adjusted.add(nullKey);
            }
        }

        // We don't include done tasks by default
        return String.join(isAnd ? " & " : " | ", adjusted) + " & !" + TodoTxtQuery.QUERY_DONE;
    }

    // Query matching (delegated to the query-language engine)
    // -------------------------------------------------------------------------------------------

    public static boolean isMatchQuery(final TodoTxtTask task, final CharSequence query) {
        return TodoTxtQuery.isMatchQuery(task, query);
    }

    // Saved view persistence (delegated to storage)
    // -------------------------------------------------------------------------------------------

    public static void saveFilter(final Context context, final String title, final String query) {
        TodoTxtFilterStorage.saveFilter(context, title, query);
    }

    public static boolean deleteFilterIndex(final Context context, final int index) {
        return TodoTxtFilterStorage.deleteFilterIndex(context, index);
    }

    public static List<Pair<String, String>> loadSavedFilters(final Context context) {
        return TodoTxtFilterStorage.loadSavedFilters(context);
    }
}
