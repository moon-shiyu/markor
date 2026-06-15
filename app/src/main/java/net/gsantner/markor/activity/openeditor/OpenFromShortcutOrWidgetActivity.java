package net.gsantner.markor.activity.openeditor;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import net.gsantner.markor.activity.DocumentActivity;
import net.gsantner.markor.model.Document;

import java.io.File;

/**
 * This Activity exists solely to launch activities with the correct intent
 * it is necessary as widget and shortcut intents do not respect MultipleTask etc
 */
public class OpenFromShortcutOrWidgetActivity extends AppCompatActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        finish();
        DocumentActivity.launch(this, getIntent());
    }

    // Intent construction for shortcut / widget entry points.
    // Centralized here (the launch target) so the action + extra conventions
    // live in a single place instead of being re-assembled by every caller.
    // -----------------------------------------------------------------------

    /**
     * Base intent targeting this activity with the given action.
     */
    public static Intent buildIntent(final Context context, final String action) {
        return new Intent(context, OpenFromShortcutOrWidgetActivity.class).setAction(action);
    }

    /**
     * Open the empty "Share Into" editor (used by the launcher share shortcut).
     */
    public static Intent shareTextIntent(final Context context) {
        return buildIntent(context, Intent.ACTION_SEND).putExtra(Intent.EXTRA_TEXT, "");
    }

    /**
     * Open the given file for editing (used by the ToDo widget container/list).
     */
    public static Intent editFileIntent(final Context context, final File file) {
        return buildIntent(context, Intent.ACTION_EDIT).putExtra(Document.EXTRA_FILE, file);
    }

    /**
     * Fill-in intent (no component) carrying the line number to open. Merged
     * with {@link #editFileIntent} by the widget list template.
     */
    public static Intent lineFillInIntent(final int lineNumber) {
        return new Intent().putExtra(Document.EXTRA_FILE_LINE_NUMBER, lineNumber);
    }
}