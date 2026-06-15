package net.gsantner.markor.widget;

import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import net.gsantner.markor.R;
import net.gsantner.markor.activity.openeditor.OpenFromShortcutOrWidgetActivity;
import net.gsantner.markor.model.AppSettings;
import net.gsantner.markor.model.Document;

import java.util.ArrayList;
import java.util.List;

public class TodoWidgetRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

    private final Context _context;
    private final AppSettings _appSettings;
    private final Document _document;
    private final List<String> _descriptions;

    public TodoWidgetRemoteViewsFactory(Context context, Intent intent) {
        _context = context;
        _appSettings = AppSettings.get(_context);
        _document = new Document(_appSettings.getTodoFile());
        _descriptions = new ArrayList<>();
    }

    @Override
    public void onCreate() {
        onDataSetChanged();
    }

    @Override
    public void onDataSetChanged() {
        _descriptions.clear();
        _descriptions.addAll(TodoWidgetData.parseTaskDescriptions(_document.loadContent(_context)));
    }

    @Override
    public void onDestroy() {
        _descriptions.clear();
    }

    @Override
    public int getCount() {
        return _descriptions.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        RemoteViews views = new RemoteViews(_context.getPackageName(), R.layout.todo_widget_list_item);
        views.setTextViewText(R.id.todo_widget_item_text, _descriptions.get(position));
        views.setInt(R.id.todo_widget_item_text, "setTextColor", _appSettings.getEditorForegroundColor());

        final Intent fillInIntent = OpenFromShortcutOrWidgetActivity.lineFillInIntent(position);
        views.setOnClickFillInIntent(R.id.todo_widget_item_text, fillInIntent);

        return views;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }
}
