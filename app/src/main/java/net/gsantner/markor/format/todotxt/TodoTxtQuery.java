package net.gsantner.markor.format.todotxt;

import androidx.annotation.VisibleForTesting;

import net.gsantner.markor.format.todotxt.TodoTxtTask.TodoDueState;

import java.util.EmptyStackException;
import java.util.Stack;

/**
 * The todo.txt query language: turns a filter query string into a boolean result for a given task.
 * <p>
 * A query is a whitespace separated sequence of <em>elements</em> (e.g. {@code +project},
 * {@code @context}, {@code pri:A}, {@code due<}, {@code done}) combined with the boolean operators
 * {@code !} (not), {@code &} (and), {@code |} (or) and grouped with parentheses. Each element is
 * evaluated against the task to {@code T}/{@code F}, then the resulting boolean expression is
 * reduced with a simple stack machine.
 * <p>
 * This class only understands the query language; building query strings and gathering filter-key
 * statistics lives in {@link TodoTxtFilter}.
 */
public class TodoTxtQuery {

    // Special query keywords
    // ----------------------------------------------------------------------------------------
    public final static String QUERY_PRIORITY_ANY = "pri";
    public final static String QUERY_DUE_TODAY = "due=";
    public final static String QUERY_DUE_OVERDUE = "due<";
    public final static String QUERY_DUE_FUTURE = "due>";
    public final static String QUERY_DUE_ANY = "due";
    public final static String QUERY_DONE = "done";

    // Query matching
    // -------------------------------------------------------------------------------------------

    public static boolean isMatchQuery(final TodoTxtTask task, final CharSequence query) {
        try {
            final CharSequence expression = parseQuery(task, query);
            return evaluateExpression(expression);
        } catch (EmptyStackException | IllegalArgumentException e) {
            // TODO - display a useful message somehow
            return false;
        }
    }

    // Pre-process the query to simplify the syntax
    private static String preProcess(final CharSequence query) {
        return String.format(" %s ", query)                  // Leading and trailing spaces
                .replace(" !", " ! ")    // Add space after exclamation mark
                .replace(" (", " ( ")    // Add space before opening paren
                .replace(") ", " ) ");   // Add space after closing paren
    }

    private static boolean isSyntax(final char c) {
        return c == '!' || c == '|' || c == '&' || c == '(' || c == ')';
    }

    // Parse a query into an expression.
    // i.e. evaluate the elements in the query to true or false
    @VisibleForTesting
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

    // Evaluate a word (element) for truthyness or falsiness
    // Step through all the possible conditions
    private static char evalElement(final TodoTxtTask task, final String element) {

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
            result = task.getDueStatus() == TodoDueState.TODAY;
        } else if (QUERY_DUE_OVERDUE.equals(element)) {
            result = task.getDueStatus() == TodoDueState.OVERDUE;
        } else if (QUERY_DUE_FUTURE.equals(element)) {
            result = task.getDueStatus() == TodoDueState.FUTURE;
        } else if (QUERY_DUE_ANY.equals(element)) {
            result = task.getDueStatus() != TodoDueState.NONE;
        } else if (QUERY_DONE.equals(element)) {
            result = task.isDone();
        } else if (element.equals("@")) {
            result = !task.getContexts().isEmpty();
        } else if (element.equals("+")) {
            result = !task.getProjects().isEmpty();
        } else if (element.startsWith("@")) {
            result = task.getContexts().contains(element.substring(1));
        } else if (element.startsWith("+")) {
            result = task.getProjects().contains(element.substring(1));
        } else {
            // Default to string match
            result = task.getLine().toLowerCase().contains(element.toLowerCase());
        }

        return result ? 'T' : 'F';
    }

    // Expression evaluator
    // ---------------------------------------------------------------------------------------------

    private static boolean isStart(final Stack<Character> stack) {
        return stack.isEmpty() || stack.peek() == '(';
    }

    private static boolean isValue(final Stack<Character> stack) {
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

    @VisibleForTesting
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
}
