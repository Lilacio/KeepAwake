package io.github.lilacio.keepawake;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.service.quicksettings.TileService;

/** Toggle logic: keep-awake is on while AwakeService runs. When off, the service is gone and the process can be reclaimed. */
public final class Awake {
    private Awake() {}

    static boolean isOn() {
        return AwakeService.running;
    }

    /** Keep-awake needs the "display over other apps" permission. */
    static boolean canOverlay(Context c) {
        return Settings.canDrawOverlays(c);
    }

    static Intent overlayPermissionIntent(Context c) {
        return new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + c.getPackageName()))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    /** Returns the new state. */
    static boolean toggle(Context c) {
        if (isOn()) {
            c.stopService(new Intent(c, AwakeService.class));
            return false;
        }
        c.startForegroundService(new Intent(c, AwakeService.class));
        return true;
    }

    /** Refreshes the home-screen widgets and the Quick Settings tile. */
    static void refresh(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        int[] ids = m.getAppWidgetIds(new ComponentName(c, AwakeWidget.class));
        AwakeWidget.render(c, m, ids);
        TileService.requestListeningState(c, new ComponentName(c, AwakeTile.class));
    }
}
