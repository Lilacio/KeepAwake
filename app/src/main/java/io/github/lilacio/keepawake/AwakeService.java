package io.github.lilacio.keepawake;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

/**
 * Foreground service that exists only while keep-awake is on.
 *
 * <p>It keeps the screen on with a 1x1 transparent overlay window carrying
 * FLAG_KEEP_SCREEN_ON, plus an ongoing notification. A wake lock is not used:
 * HyperOS ignores SCREEN_BRIGHT_WAKE_LOCK from apps that are not in the
 * foreground, while KEEP_SCREEN_ON on a shown window is always honoured.
 *
 * <p>The notification has two styles, switchable from its action button and
 * remembered: silent (default, lowest priority) or Super Island (Live Update).
 *
 * <p>No polling or timers. The service stops on screen off or via "Turn off".
 */
public class AwakeService extends Service {
    static final String ACTION_STOP = "io.github.lilacio.keepawake.STOP";
    static final String ACTION_SWITCH_STYLE = "io.github.lilacio.keepawake.SWITCH_STYLE";
    /** Lowest priority: no status bar icon, collapsed at the bottom of the shade. */
    private static final String CH_SILENT = "awake_silent";
    /** Default priority (still silent): eligible for Live Updates / Super Island. */
    private static final String CH_ISLAND = "awake";
    private static final String PREF_ISLAND = "island";
    private static final int NOTIF_ID = 1;

    static volatile boolean running;

    private View keeper;
    private long startedAt;

    private final BroadcastReceiver screenOff = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            stopSelf();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        running = true;
        startedAt = System.currentTimeMillis();
        startForeground(NOTIF_ID, buildNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        if (!addKeeper()) {
            // Without the overlay permission we cannot keep the screen on.
            // The entry points ask for the permission before starting us.
            stopSelf();
            return;
        }
        registerReceiver(screenOff, new IntentFilter(Intent.ACTION_SCREEN_OFF),
                Context.RECEIVER_NOT_EXPORTED);
        Awake.refresh(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String a = intent == null ? null : intent.getAction();
        if (ACTION_STOP.equals(a)) {
            stopSelf();
        } else if (ACTION_SWITCH_STYLE.equals(a)) {
            SharedPreferences p = getSharedPreferences("awake", MODE_PRIVATE);
            p.edit().putBoolean(PREF_ISLAND, !p.getBoolean(PREF_ISLAND, false)).apply();
            getSystemService(NotificationManager.class).notify(NOTIF_ID, buildNotification());
        }
        // Do not restart automatically if the system kills us.
        return START_NOT_STICKY;
    }

    /**
     * Adds a 1x1, non-touchable, non-focusable transparent window whose only
     * purpose is to carry FLAG_KEEP_SCREEN_ON. It is placed just outside the
     * visible area so it never overlaps other apps' windows; otherwise apps
     * that reject touches while "partially obscured" (some banking or access
     * apps) could refuse to work.
     */
    private boolean addKeeper() {
        if (!Awake.canOverlay(this)) return false;
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(1, 1,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.x = -10;
        lp.y = -10;
        keeper = new View(this);
        try {
            getSystemService(WindowManager.class).addView(keeper, lp);
            return true;
        } catch (RuntimeException e) {
            keeper = null;
            return false;
        }
    }

    private Notification buildNotification() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(channel(CH_SILENT, R.string.channel_silent,
                NotificationManager.IMPORTANCE_MIN));
        nm.createNotificationChannel(channel(CH_ISLAND, R.string.channel_island,
                NotificationManager.IMPORTANCE_DEFAULT));

        boolean island = getSharedPreferences("awake", MODE_PRIVATE)
                .getBoolean(PREF_ISLAND, false);

        Notification.Builder b = new Notification.Builder(this, island ? CH_ISLAND : CH_SILENT)
                .setSmallIcon(R.drawable.ic_awake)
                .setContentTitle(getString(R.string.notif_title))
                .setContentText(getString(R.string.notif_text))
                .setWhen(startedAt)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(Notification.CATEGORY_STATUS)
                .setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
                .addAction(action(R.string.action_off, ACTION_STOP, 0))
                .addAction(action(island ? R.string.action_to_silent : R.string.action_to_island,
                        ACTION_SWITCH_STYLE, 1));
        if (island) {
            // Android 16 Live Update request flag (same key AndroidX NotificationCompat uses).
            Bundle extras = new Bundle();
            extras.putBoolean("android.requestPromotedOngoing", true);
            b.setShortCriticalText(getString(R.string.island_text)).addExtras(extras);
        }
        return b.build();
    }

    private NotificationChannel channel(String id, int name, int importance) {
        NotificationChannel ch = new NotificationChannel(id, getString(name), importance);
        ch.setSound(null, null);
        ch.enableVibration(false);
        ch.setShowBadge(false);
        return ch;
    }

    private Notification.Action action(int label, String act, int req) {
        PendingIntent pi = PendingIntent.getService(this, req,
                new Intent(this, AwakeService.class).setAction(act),
                PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Action.Builder(null, getString(label), pi).build();
    }

    @Override
    public void onDestroy() {
        running = false;
        try {
            unregisterReceiver(screenOff);
        } catch (IllegalArgumentException ignored) {
            // Not registered (we stopped before registering).
        }
        if (keeper != null) {
            try {
                getSystemService(WindowManager.class).removeView(keeper);
            } catch (RuntimeException ignored) {
                // Window already gone.
            }
            keeper = null;
        }
        stopForeground(STOP_FOREGROUND_REMOVE);
        Awake.refresh(this);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
