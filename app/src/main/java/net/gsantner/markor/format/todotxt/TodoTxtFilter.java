/*#######################################################
 *
 * SPDX-FileCopyrightText: 2017-2025 Gregor Santner <gsantner AT mailbox DOT org>
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
 * Written 2017-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
#########################################################*/
package net.gsantner.markor.format.todotxt;

import android.content.Context;
import android.util.Pair;

import androidx.annotation.VisibleForTesting;

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
 * Central facade for todo.txt filtering.
 * <p>
 * Owns the query-keyword constants, the {@link TYPE} enum, {@link SttFilterKey},
 * and the filter-key statistics methods ({@link #getKeys}, {@link #makeQuery}).
 * <p>
 * Query evaluation delegates to {@link TodoTxtQueryEvaluator}.
 * Saved-view persistence delegates to {@link TodoTxtSavedViews}.
 */
public class TodoTxtFilter {

    // Special query keywords — canonical definitions live in TodoTxtQueryEvaluator;
    // these aliases preserve binary/source compatibility for any external caller.
    public static final String QUERY_PRIORITY_ANY = TodoTxtQueryEvaluator.QUERY_PRIORITY_ANY;
    public static final String QUERY_DUE_TODAY = TodoTxtQueryEvaluator.QUERY_DUE_TODAY;
    public static final String QUERY_DUE_OVERDUE = TodoTxtQueryEvaluator.QUERY_DUE_OVERDUE;
    public static final String QUERY_DUE_FUTURE = TodoTxtQueryEvaluator.QUERY_DUE_FUTURE;
    public static final String QUERY_DUE_ANY = TodoTxtQueryEvaluator.QUERY_DUE_ANY;
    public static final String QUERY_DONE = TodoTxtQueryEvaluator.QUERY_DONE;

    // ----------------------------------------------------------------------------------------
    public static final String STRING_NONE = "-";

    public enum TYPE {
        PROJECT, CONTEXT, PRIORITY, DUE
    }

    public static class SttFilterKey {
        public final String key;
        public final int count;
        public final String query;

        private SttFilterKey(String k, int c, String q) {
            key = k;
            count = c;
            query = q;
        }
    }

    // ---------------------------------------------------------------------------------------
    // Filter key statistics
    // ---------------------------------------------------------------------------------------

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
        int noneCount = 0;

        for (final TodoTxtTask task : tasks) {
            if (!task.isDone()) {
                final List<String> tKeys = keyGetter.callback(task);
                if (tKeys == null || tKeys.isEmpty()) {
                    noneCount++;
                } else {
                    all.addAll(tKeys);
                }
            }
        }

        final List<SttFilterKey> keys = new ArrayList<>();
        final Set<String> unique = new TreeSet<>(all);

        if (noneCount > 0) {
            keys.add(new SttFilterKey(STRING_NONE, noneCount, null));
        }
        for (final String key : unique) {
            keys.add(new SttFilterKey(key, Collections.frequency(all, key), key));
        }

        return keys;
    }

    public static List<SttFilterKey> getDueKeys(final Context context, final List<TodoTxtTask> tasks) {
        final List<TodoDueState> all = new ArrayList<>();
        for (final TodoTxtTask task : tasks) {
            if (!task.isDone()) {
                all.add(task.getDueStatus());
            }
        }
        final List<SttFilterKey> keys = new ArrayList<>();
        keys.add(new SttFilterKey(context.getString(R.string.due_future), Collections.frequency(all, TodoDueState.FUTURE), QUERY_DUE_FUTURE));
        keys.add(new SttFilterKey(context.getString(R.string.due_today), Collections.frequency(all, TodoDueState.TODAY), QUERY_DUE_TODAY));
        keys.add(new SttFilterKey(context.getString(R.string.due_overdue), Collections.frequency(all, TodoDueState.OVERDUE), QUERY_DUE_OVERDUE));
        keys.add(new SttFilterKey(STRING_NONE, Collections.frequency(all, TodoDueState.NONE), null));

        return keys;
    }

    // Convert a set of query keys into a formatted query
    public static String makeQuery(final Collection<String> keys, final boolean isAnd, final TYPE type) {
        final String prefix;
        final String nullKey;
        if (type == TYPE.CONTEXT) {
            nullKey = "!@";
            prefix = "@";
        } else if (type == TYPE.PROJECT) {
            nullKey = "!+";
            prefix = "+";
        } else if (type == TYPE.PRIORITY) {
            nullKey = "!" + QUERY_PRIORITY_ANY;
            prefix = "pri:";
        } else {
            nullKey = "!" + QUERY_DUE_ANY;
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

        return String.join(isAnd ? " & " : " | ", adjusted) + " & !" + QUERY_DONE;
    }

    // ---------------------------------------------------------------------------------------
    // Delegates — Query evaluation (→ TodoTxtQueryEvaluator)
    // ---------------------------------------------------------------------------------------

    public static boolean isMatchQuery(final TodoTxtTask task, final CharSequence query) {
        return TodoTxtQueryEvaluator.isMatchQuery(task, query);
    }

    @VisibleForTesting
    public static String parseQuery(final TodoTxtTask task, final CharSequence query) {
        return TodoTxtQueryEvaluator.parseQuery(task, query);
    }

    public static boolean evaluateExpression(final CharSequence expression) {
        return TodoTxtQueryEvaluator.evaluateExpression(expression);
    }

    // ---------------------------------------------------------------------------------------
    // Delegates — Saved views persistence (→ TodoTxtSavedViews)
    // ---------------------------------------------------------------------------------------

    public static void saveFilter(final Context context, final String title, final String query) {
        TodoTxtSavedViews.saveFilter(context, title, query);
    }

    public static boolean deleteFilterIndex(final Context context, int index) {
        return TodoTxtSavedViews.deleteFilterIndex(context, index);
    }

    public static List<Pair<String, String>> loadSavedFilters(final Context context) {
        return TodoTxtSavedViews.loadSavedFilters(context);
    }
}
