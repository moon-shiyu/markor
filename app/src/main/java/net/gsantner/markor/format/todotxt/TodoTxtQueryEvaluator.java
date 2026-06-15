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

import java.util.EmptyStackException;
import java.util.Stack;

/**
 * Pure-Java query expression evaluator for todo.txt tasks.
 * <p>
 * Supports boolean operators: {@code &} (AND), {@code |} (OR), {@code !} (NOT),
 * {@code (} {@code )} (grouping), and the following special keywords:
 * <ul>
 *   <li>{@code pri} — has any priority</li>
 *   <li>{@code pri:X} — has priority X</li>
 *   <li>{@code due=} — due today</li>
 *   <li>{@code due<} — overdue</li>
 *   <li>{@code due>} — due in future</li>
 *   <li>{@code due} — has any due date</li>
 *   <li>{@code done} — task is done</li>
 *   <li>{@code @} — has any context</li>
 *   <li>{@code +} — has any project</li>
 *   <li>{@code @work} — has context "work"</li>
 *   <li>{@code +project} — has project "project"</li>
 * </ul>
 * Any other word falls back to case-insensitive string match on the task line.
 * <p>
 * This class has no Android dependencies.
 */
public class TodoTxtQueryEvaluator {

    // Query keywords — canonical definitions; TodoTxtFilter re-exports aliases for backward compat
    public static final String QUERY_PRIORITY_ANY = "pri";
    public static final String QUERY_DUE_TODAY = "due=";
    public static final String QUERY_DUE_OVERDUE = "due<";
    public static final String QUERY_DUE_FUTURE = "due>";
    public static final String QUERY_DUE_ANY = "due";
    public static final String QUERY_DONE = "done";

    private TodoTxtQueryEvaluator() {
        // Utility class
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Test whether a task matches a query expression.
     *
     * @return true if the task satisfies the query, false on mismatch or malformed expression
     */
    public static boolean isMatchQuery(final TodoTxtTask task, final CharSequence query) {
        try {
            final CharSequence expression = parseQuery(task, query);
            return evaluateExpression(expression);
        } catch (EmptyStackException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Parse a query against a task, producing a boolean expression string
     * of {@code T}, {@code F}, and operator characters.
     */
    public static String parseQuery(final TodoTxtTask task, final CharSequence query) {
        final StringBuilder expression = new StringBuilder();
        final String[] parts = preProcess(query).split(" ");
        for (final String part : parts) {
            if (part.length() == 1 && isSyntax(part.charAt(0))) {
                expression.append(part);
            } else if (!part.isEmpty()) {
                expression.append(evalElement(task, part));
            }
        }
        return expression.toString();
    }

    /**
     * Evaluate a boolean expression string produced by {@link #parseQuery}.
     *
     * @throws IllegalArgumentException on malformed expressions
     */
    public static boolean evaluateExpression(final CharSequence expression) {
        final Stack<Character> stack = new Stack<>();
        for (int i = 0; i < expression.length(); i++) {
            final char c = expression.charAt(i);
            if (c == ')') {
                final char value = stack.pop();
                if (stack.pop() != '(') {
                    throw new IllegalArgumentException("Mismatched parenthesis");
                }
                stack.push(value);
            } else {
                stack.push(c);
            }
            evaluateOperations(stack);
        }
        if (stack.size() == 1 && isValue(stack)) {
            return stack.pop() == 'T';
        }
        throw new IllegalArgumentException("Malformed expression");
    }

    // -----------------------------------------------------------------------
    // Query preprocessing & element evaluation
    // -----------------------------------------------------------------------

    private static String preProcess(final CharSequence query) {
        return String.format(" %s ", query)
                .replace(" !", " ! ")
                .replace(" (", " ( ")
                .replace(") ", " ) ");
    }

    private static boolean isSyntax(final char c) {
        return c == '!' || c == '|' || c == '&' || c == '(' || c == ')';
    }

    /**
     * Evaluate a single query element (word) against a task.
     *
     * @return 'T' for true, 'F' for false
     */
    static char evalElement(final TodoTxtTask task, final String element) {
        final boolean result;

        if (element.startsWith(QUERY_PRIORITY_ANY)) {
            if (QUERY_PRIORITY_ANY.equals(element)) {
                result = task.getPriority() != TodoTxtTask.PRIORITY_NONE;
            } else if (element.length() == 5 && element.charAt(3) == ':') {
                result = task.getPriority() == element.charAt(4);
            } else {
                result = false;
            }
        } else if (QUERY_DUE_TODAY.equals(element)) {
            result = task.getDueStatus() == TodoTxtTask.TodoDueState.TODAY;
        } else if (QUERY_DUE_OVERDUE.equals(element)) {
            result = task.getDueStatus() == TodoTxtTask.TodoDueState.OVERDUE;
        } else if (QUERY_DUE_FUTURE.equals(element)) {
            result = task.getDueStatus() == TodoTxtTask.TodoDueState.FUTURE;
        } else if (QUERY_DUE_ANY.equals(element)) {
            result = task.getDueStatus() != TodoTxtTask.TodoDueState.NONE;
        } else if (QUERY_DONE.equals(element)) {
            result = task.isDone();
        } else if ("@".equals(element)) {
            result = !task.getContexts().isEmpty();
        } else if ("+".equals(element)) {
            result = !task.getProjects().isEmpty();
        } else if (element.startsWith("@")) {
            result = task.getContexts().contains(element.substring(1));
        } else if (element.startsWith("+")) {
            result = task.getProjects().contains(element.substring(1));
        } else {
            // Default: case-insensitive string match on the full task line
            result = task.getLine().toLowerCase().contains(element.toLowerCase());
        }

        return result ? 'T' : 'F';
    }

    // -----------------------------------------------------------------------
    // Stack-based expression evaluator internals
    // -----------------------------------------------------------------------

    private static boolean isStart(final Stack<Character> stack) {
        return stack.isEmpty() || stack.peek() == '(';
    }

    static boolean isValue(final Stack<Character> stack) {
        if (!stack.isEmpty()) {
            final char top = stack.peek();
            return top == 'T' || top == 'F';
        }
        return false;
    }

    private static char toChar(final boolean v) {
        return v ? 'T' : 'F';
    }

    private static void evaluateOperations(final Stack<Character> stack) {
        while (!isStart(stack) && isValue(stack)) {
            final char rhs = stack.pop();
            if (isStart(stack)) {
                stack.push(rhs);
                return;
            }
            final char op = stack.pop();
            if (op == '|') {
                stack.push(toChar(stack.pop() == 'T' | rhs == 'T'));
            } else if (op == '&') {
                stack.push(toChar(stack.pop() == 'T' & rhs == 'T'));
            } else if (op == '!') {
                stack.push(toChar(rhs == 'F'));
            } else {
                throw new IllegalArgumentException("Unexpected character");
            }
        }
    }
}
