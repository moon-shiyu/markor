package net.gsantner.markor.format.todotxt;

import android.content.Context;
import android.content.SharedPreferences;
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
 * Persistence for todo.txt saved filter views.
 * <p>
 * Saved views are stored as a JSON array of {@code {TITLE, QUERY}} objects under
 * {@link #SAVED_TODO_VIEWS} in the app {@link SharedPreferences}. The newest view is kept at the
 * head of the array and the list is capped at {@link #MAX_RECENT_VIEWS} entries.
 * <p>
 * This is purely the storage format; building and evaluating the queries lives in
 * {@link TodoTxtFilter} and {@link TodoTxtQuery}.
 */
public class TodoTxtFilterStorage {

    public static final String SAVED_TODO_VIEWS = "todo_txt__saved_todo_views";
    public static final int MAX_RECENT_VIEWS = 10;

    private static final String TITLE = "TITLE";
    private static final String QUERY = "QUERY";

    public static void saveFilter(final Context context, final String title, final String query) {
        try {
            // Create the view dict
            final JSONObject obj = new JSONObject();
            obj.put(TITLE, title);
            obj.put(QUERY, query);

            // This oldArray / newArray approach needed as array.remove is api 19+
            final JSONArray newArray = new JSONArray();
            newArray.put(obj);

            // Load the existing list of views and append the required number to the newArray
            final SharedPreferences pref = context.getSharedPreferences(GsSharedPreferencesPropertyBackend.SHARED_PREF_APP, Context.MODE_PRIVATE);
            final JSONArray oldArray = new JSONArray(pref.getString(SAVED_TODO_VIEWS, "[]"));
            final int addCount = Math.min(MAX_RECENT_VIEWS - 1, oldArray.length());
            for (int i = 0; i < addCount; i++) {
                newArray.put(oldArray.get(i));
            }

            // Save
            pref.edit().putString(SAVED_TODO_VIEWS, newArray.toString()).apply();

        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(context, "𐄂", Toast.LENGTH_SHORT).show();
        }

        Toast.makeText(context, String.format("✔ %s️", title), Toast.LENGTH_SHORT).show();
    }

    public static boolean deleteFilterIndex(final Context context, int index) {
        try {
            final SharedPreferences pref = context.getSharedPreferences(GsSharedPreferencesPropertyBackend.SHARED_PREF_APP, Context.MODE_PRIVATE);
            // Load the existing list of views

            final JSONArray oldArray = new JSONArray(pref.getString(SAVED_TODO_VIEWS, "[]"));
            if (index < 0 || index >= oldArray.length()) {
                return false;
            }

            final JSONArray newArray = new JSONArray();
            for (int i = 0; i < oldArray.length(); i++) {
                if (i != index) {
                    newArray.put(oldArray.get(i));
                }
            }

            pref.edit().putString(SAVED_TODO_VIEWS, newArray.toString()).apply();

            return true;
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static List<Pair<String, String>> loadSavedFilters(final Context context) {
        final SharedPreferences pref = context.getSharedPreferences(GsSharedPreferencesPropertyBackend.SHARED_PREF_APP, Context.MODE_PRIVATE);
        try {
            final List<Pair<String, String>> loadedViews = new ArrayList<>();
            final String jsonString = pref.getString(SAVED_TODO_VIEWS, "[]");
            final JSONArray array = new JSONArray(jsonString);
            for (int i = 0; i < array.length(); i++) {
                final JSONObject obj = array.getJSONObject(i);
                loadedViews.add(Pair.create(obj.getString(TITLE), obj.getString(QUERY)));
            }
            return loadedViews;
        } catch (JSONException e) {
            e.printStackTrace();
            pref.edit().remove(SAVED_TODO_VIEWS).apply();
        }
        return Collections.emptyList();
    }
}
