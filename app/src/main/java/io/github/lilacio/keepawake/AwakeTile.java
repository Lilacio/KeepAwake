package io.github.lilacio.keepawake;

import android.app.PendingIntent;
import android.content.Intent;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/** Quick Settings tile. */
public class AwakeTile extends TileService {
    @Override
    public void onStartListening() {
        update();
    }

    @Override
    public void onClick() {
        if (Awake.isOn() || Awake.canOverlay(this)) {
            try {
                Awake.toggle(this);
                update();
                return;
            } catch (RuntimeException ignored) {
                // Starting a foreground service from here was not allowed; fall back to the trampoline activity.
            }
        }
        // Permission needed, or direct start failed: let the trampoline activity handle it.
        startActivityAndCollapse(PendingIntent.getActivity(this, 1,
                new Intent(this, ToggleActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_IMMUTABLE));
    }

    private void update() {
        Tile t = getQsTile();
        if (t == null) return;
        boolean on = Awake.isOn();
        t.setState(on ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        t.setLabel(getString(R.string.tile_label));
        t.setSubtitle(getString(on ? R.string.state_on : R.string.state_off));
        t.updateTile();
    }
}
