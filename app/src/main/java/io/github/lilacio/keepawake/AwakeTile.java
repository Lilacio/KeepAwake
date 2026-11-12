package io.github.lilacio.keepawake;

import android.app.PendingIntent;
import android.content.Intent;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

/** Quick Settings tile. */
public class AwakeTile extends TileService {
    private static final String TAG = "KeepAwake";

    @Override
    public void onStartListening() {
        show(Awake.isOn());
    }

    @Override
    public void onClick() {
        if (Awake.isOn() || Awake.canOverlay(this)) {
            try {
                // The service starts/stops asynchronously, so show the new state
                // right away instead of reading Awake.isOn(), which is still stale.
                show(Awake.toggle(this));
                return;
            } catch (RuntimeException e) {
                // E.g. ForegroundServiceStartNotAllowedException from a background start.
                Log.w(TAG, "Direct toggle from tile failed, using activity", e);
            }
        }
        // Permission needed, or direct start failed: let the trampoline activity handle it.
        try {
            startActivityAndCollapse(PendingIntent.getActivity(this, 1,
                    new Intent(this, ToggleActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    PendingIntent.FLAG_IMMUTABLE));
        } catch (RuntimeException e) {
            Log.e(TAG, "Could not open trampoline activity from tile", e);
        }
    }

    private void show(boolean on) {
        Tile t = getQsTile();
        if (t == null) return;
        t.setState(on ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        t.setLabel(getString(R.string.tile_label));
        t.setSubtitle(getString(on ? R.string.state_on : R.string.state_off));
        t.updateTile();
    }
}
