/*#######################################################
 *
 *   Maintained 2017-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.widget;

import android.content.Context;

import net.gsantner.markor.format.todotxt.TodoTxtTask;
import net.gsantner.markor.model.Document;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Loads and parses todo.txt tasks for the ToDo widget.
 * <p>
 * Decoupled from {@link android.widget.RemoteViewsService.RemoteViewsFactory}
 * so that the parsing logic can be tested independently.
 */
public final class TodoWidgetDataLoader {

    private TodoWidgetDataLoader() {
    }

    /**
     * Read the given file and parse its content into a list of {@link TodoTxtTask}.
     *
     * @param context required by {@link Document#loadContent(Context)}
     * @param todoFile the todo.txt file
     * @return parsed tasks, or an empty list if the file is empty / unreadable
     */
    public static List<TodoTxtTask> loadTasks(final Context context, final File todoFile) {
        final Document document = new Document(todoFile);
        final String content = document.loadContent(context);
        if (content == null || content.isEmpty()) {
            return Collections.emptyList();
        }
        return parseTasks(content);
    }

    /**
     * Parse a raw todo.txt content string into tasks.
     * Falls back to manual line splitting if {@link TodoTxtTask#getAllTasks}
     * cannot run (e.g. in a pure JUnit environment without Android APIs).
     *
     * @param content todo.txt file content
     * @return parsed tasks, or an empty list if content is null / empty
     */
    public static List<TodoTxtTask> parseTasks(final String content) {
        if (content == null || content.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return TodoTxtTask.getAllTasks(content);
        } catch (final NoClassDefFoundError | RuntimeException e) {
            // Fallback for pure JUnit environment where Android APIs are unavailable
            final List<TodoTxtTask> tasks = new ArrayList<>();
            for (final String line : content.split("\n")) {
                if (!line.isEmpty()) {
                    tasks.add(new TodoTxtTask(line));
                }
            }
            return tasks;
        }
    }
}
