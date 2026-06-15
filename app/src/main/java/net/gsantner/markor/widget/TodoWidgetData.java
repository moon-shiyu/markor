/*#######################################################
 *
 *   Maintained 2018-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.widget;

import net.gsantner.markor.format.todotxt.TodoTxtTask;

import java.util.ArrayList;
import java.util.List;

/**
 * Android-free data loading logic for the ToDo widget. Kept separate from
 * {@link TodoWidgetRemoteViewsFactory} (which depends on {@code RemoteViews})
 * so the parsing can be unit tested on the plain JVM.
 */
public final class TodoWidgetData {

    private TodoWidgetData() {
    }

    /**
     * Parse todo.txt file content into the list of task descriptions shown in
     * the widget rows. Returns an empty list when {@code content} is null.
     */
    public static List<String> parseTaskDescriptions(final String content) {
        final List<String> descriptions = new ArrayList<>();
        if (content == null) {
            return descriptions;
        }
        for (final TodoTxtTask task : TodoTxtTask.getAllTasks(content)) {
            descriptions.add(task.getDescription());
        }
        return descriptions;
    }
}
