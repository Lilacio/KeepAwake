package io.github.lilacio.keepawake;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

/** 1x1 home-screen widget. */
public class AwakeWidget extends AppWidgetProvider {
    private static final int FG_ON = 0xFF2B1F00;
    private static final int FG_OFF = 0xFFFFFFFF;

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        render(c, m, ids);
    }

    static void render(Context c, AppWidgetManager m, int[] ids) {
        if (ids == null || ids.length == 0) return;
        boolean on = Awake.isOn();
        Intent i = new Intent(c, ToggleActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pi = PendingIntent.getActivity(c, 0, i,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        int fg = on ? FG_ON : FG_OFF;
        for (int id : ids) {
            RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget);
            v.setInt(R.id.root, "setBackgroundResource", on ? R.drawable.bg_on : R.drawable.bg_off);
            v.setInt(R.id.icon, "setColorFilter", fg);
            v.setTextViewText(R.id.label, c.getString(on ? R.string.state_on : R.string.state_off));
            v.setTextColor(R.id.label, fg);
            v.setOnClickPendingIntent(R.id.root, pi);
            m.updateAppWidget(id, v);
        }
    }
}
