package io.github.lilacio.keepawake;

import android.Manifest;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

/**
 * Transparent trampoline activity used by the launcher icon and the widget.
 * Asks for permissions on first use, then toggles and finishes immediately.
 */
public class ToggleActivity extends Activity {
    private static final int REQ = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences p = getSharedPreferences("awake", MODE_PRIVATE);
        boolean needAsk = checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED && !p.getBoolean("asked", false);
        if (needAsk) {
            p.edit().putBoolean("asked", true).apply();
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ);
        } else {
            doToggle();
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        // Keep-awake works without the notification permission; only the notification / Super Island is hidden.
        doToggle();
    }

    private void doToggle() {
        if (!Awake.isOn() && !Awake.canOverlay(this)) {
            Toast.makeText(this, R.string.need_overlay, Toast.LENGTH_LONG).show();
            startActivity(Awake.overlayPermissionIntent(this));
            finish();
            return;
        }
        boolean on = Awake.toggle(this);
        Toast.makeText(this, on ? R.string.toast_on : R.string.toast_off,
                Toast.LENGTH_SHORT).show();
        finish();
    }
}
