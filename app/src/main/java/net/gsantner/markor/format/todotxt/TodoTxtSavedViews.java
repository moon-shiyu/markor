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
import android.widget.Toast;

import net.gsantner.opoc.model.GsSharedPreferencesPropertyBackend;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Persistence layer for saved todo.txt filter views.
 * <p>
 * Views are stored as a JSON array in SharedPreferences under the key
 * {@link #SAVED_TODO_VIEWS}. Each entry is a {@code {"TITLE":..., "QUERY":...}}
 * object. A maximum of {@link #MAX_RECENT_VIEWS} views are retained.
 */
public class TodoTxtSavedViews {

    public static final String SAVED_TODO_VIEWS = "todo_txt__saved_todo_views";
    public static final int MAX_RECENT_VIEWS = 10;

    static final String TITLE = "TITLE";
    static final String QUERY = "QUERY";

    private TodoTxtSavedViews() {
        // Utility class
    }

    /**
     * Save a filter view. Prepends the new view and trims to {@link #MAX_RECENT_VIEWS}.
     */
    public static void saveFilter(final Context context, final String title, final String query) {
        try {
            final JSONObject obj = new JSONObject();
            obj.put(TITLE, title);
            obj.put(QUERY, query);

            final JSONArray newArray = new JSONArray();
            newArray.put(obj);

            final JSONArray oldArray = loadArray(context);
            final int addCount = Math.min(MAX_RECENT_VIEWS - 1, oldArray.length());
            for (int i = 0; i < addCount; i++) {
                newArray.put(oldArray.get(i));
            }

            saveArray(context, newArray);

        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(context, "\uD801\uDC04", Toast.LENGTH_SHORT).show();
        }

        Toast.makeText(context, String.format("\u2714 %s\uFE0E", title), Toast.LENGTH_SHORT).show();
    }

    /**
     * Delete the saved view at the given index.
     *
     * @return true if the deletion succeeded, false on invalid index or JSON error
     */
    public static boolean deleteFilterIndex(final Context context, int index) {
        try {
            final JSONArray oldArray = loadArray(context);
            if (index < 0 || index >= oldArray.length()) {
                return false;
            }

            final JSONArray newArray = new JSONArray();
            for (int i = 0; i < oldArray.length(); i++) {
                if (i != index) {
                    newArray.put(oldArray.get(i));
                }
            }

            saveArray(context, newArray);
            return true;
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Load all saved filter views.
     *
     * @return list of (title, query) pairs, or empty list on error
     */
    public static List<Pair<String, String>> loadSavedFilters(final Context context) {
        try {
            final List<Pair<String, String>> loadedViews = new ArrayList<>();
            final JSONArray array = loadArray(context);
            for (int i = 0; i < array.length(); i++) {
                final JSONObject obj = array.getJSONObject(i);
                loadedViews.add(Pair.create(obj.getString(TITLE), obj.getString(QUERY)));
            }
            return loadedViews;
        } catch (JSONException e) {
            e.printStackTrace();
            getPrefs(context).edit().remove(SAVED_TODO_VIEWS).apply();
        }
        return Collections.emptyList();
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    private static JSONArray loadArray(final Context context) throws JSONException {
        final String jsonString = getPrefs(context).getString(SAVED_TODO_VIEWS, "[]");
        return new JSONArray(jsonString);
    }

    private static void saveArray(final Context context, final JSONArray array) {
        getPrefs(context).edit().putString(SAVED_TODO_VIEWS, array.toString()).apply();
    }

    private static android.content.SharedPreferences getPrefs(final Context context) {
        return context.getSharedPreferences(
                GsSharedPreferencesPropertyBackend.SHARED_PREF_APP,
                Context.MODE_PRIVATE);
    }
}
