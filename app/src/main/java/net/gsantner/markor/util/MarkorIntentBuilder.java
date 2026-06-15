/*#######################################################
 *
 *   Maintained 2017-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.markor.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import net.gsantner.markor.activity.openeditor.OpenFromShortcutOrWidgetActivity;
import net.gsantner.markor.model.Document;

import java.io.File;

/**
 * Centralized factory for all Intents that target {@link OpenFromShortcutOrWidgetActivity}.
 * <p>
 * <b>Widget fill-in Intent contract:</b>
 * <ul>
 *   <li><b>Template Intent</b> (created by {@link #openFile}) sets
 *       {@link Document#EXTRA_FILE} but does <b>not</b> set
 *       {@link Document#EXTRA_FILE_LINE_NUMBER}.</li>
 *   <li><b>Fill-in Intent</b> (created in {@code RemoteViewsFactory.getViewAt()})
 *       sets {@link Document#EXTRA_FILE_LINE_NUMBER} only.</li>
 *   <li>The Android system merges them at click time, producing an Intent with both extras.</li>
 * </ul>
 */
public final class MarkorIntentBuilder {

    private MarkorIntentBuilder() {
    }

    // -----------------------------------------------------------------------
    // Open-file intents
    // -----------------------------------------------------------------------

    /**
     * Create an Intent that opens a file at the given line number.
     */
    public static Intent openFileAt(final Context context, final File file, final int lineNumber) {
        return new Intent(context, OpenFromShortcutOrWidgetActivity.class)
                .setAction(Intent.ACTION_EDIT)
                .setData(Uri.fromFile(file))
                .putExtra(Document.EXTRA_FILE, file)
                .putExtra(Document.EXTRA_FILE_LINE_NUMBER, lineNumber);
    }

    /**
     * Create an Intent that opens a file without specifying a line number.
     * Suitable for widget template intents where the fill-in adds the line number.
     */
    public static Intent openFile(final Context context, final File file) {
        return new Intent(context, OpenFromShortcutOrWidgetActivity.class)
                .setAction(Intent.ACTION_EDIT)
                .setData(Uri.fromFile(file))
                .putExtra(Document.EXTRA_FILE, file);
    }

    /**
     * Create an Intent that opens a file with additional flags (e.g. FLAG_ACTIVITY_NO_ANIMATION).
     */
    public static Intent openFileWithFlags(final Context context, final File file, final int flags) {
        return openFile(context, file).addFlags(flags);
    }

    /**
     * Create a bare Intent for desktop launcher shortcuts.
     * Preserves the original behavior: only {@code setData(Uri.fromFile(file))},
     * no action, no EXTRA_FILE — exactly as the legacy code produced.
     */
    public static Intent openFileForShortcut(final Context context, final File file) {
        return new Intent(context, OpenFromShortcutOrWidgetActivity.class)
                .setData(Uri.fromFile(file));
    }

    // -----------------------------------------------------------------------
    // Share-into intents
    // -----------------------------------------------------------------------

    /**
     * Create an Intent that opens the Share-Into panel with empty text.
     */
    public static Intent shareInto(final Context context) {
        return new Intent(context, OpenFromShortcutOrWidgetActivity.class)
                .setAction(Intent.ACTION_SEND)
                .putExtra(Intent.EXTRA_TEXT, "");
    }

    /**
     * Create an Intent that opens the Share-Into panel with pre-filled text.
     */
    public static Intent shareIntoWithText(final Context context, final String text) {
        return new Intent(context, OpenFromShortcutOrWidgetActivity.class)
                .setAction(Intent.ACTION_SEND)
                .putExtra(Intent.EXTRA_TEXT, text != null ? text : "");
    }

    // -----------------------------------------------------------------------
    // Base / bare intents
    // -----------------------------------------------------------------------

    /**
     * Create a minimal Intent targeting {@link OpenFromShortcutOrWidgetActivity}
     * with no action or extras. Used as the template for widget list PendingIntents
     * where fill-in intents supply all extras.
     */
    public static Intent baseIntent(final Context context) {
        return new Intent(context, OpenFromShortcutOrWidgetActivity.class);
    }
}
