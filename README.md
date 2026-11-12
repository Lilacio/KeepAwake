# KeepAwake

**English** | [简体中文](README.zh-CN.md)

A small personal Android utility that keeps the screen on with one tap. Written in Java with no third-party libraries and no network access. Requires Android 16 or later (minSdk 36). MIT licensed.

> ⚠️ Only tested on a **Xiaomi 15 (HyperOS, Android 17)**. On other brands or systems, how Super Island / Live Updates are displayed and how the screen is kept on may differ.

## Features

The app's UI is in Chinese; names in quotes below are what you will see on the device.

There are three entry points, all doing the same thing:

| Entry point | How to add it |
| --- | --- |
| Launcher icon "一键常亮" | Available after install; tap to toggle |
| 1×1 home-screen widget "常亮开关" | Long-press the home screen → Widgets → 一键常亮 |
| Quick Settings tile "屏幕常亮" | Open Control Center → Edit → drag in "屏幕常亮" |

- **Turn on**: starts the foreground service `AwakeService`, which adds a 1×1 transparent, non-touchable overlay window with `FLAG_KEEP_SCREEN_ON` just outside the visible area. The window never overlaps any app, so banking or access-control apps won't consider the screen "obscured". An ongoing notification shows how long keep-awake has been on, with "Turn off" and style-switch buttons.
- **Notification style** (switch with the notification button; the choice is remembered until uninstall):
  - **Silent** (default): lowest-priority channel `awake_silent`. No sound, no heads-up banner, not on Super Island. Android raises foreground-service notifications to "low" priority, so some systems may show a small status bar icon; HyperOS does not.
  - **Super Island**: default-priority channel `awake` (still silent) that requests a Live Update; HyperOS shows it as a "常亮" Super Island pill.
- **Turn off**: tap any toggle again, tap "Turn off" in the notification, or press the power button. Any of these stops the service and removes the overlay window and the notification.
- **No system settings are changed**: the screen timeout stays as you set it.
- The system shows an extra lowest-priority notification for the overlay ("… is displaying over other apps"). You can turn it off in the system notification settings; keep-awake is not affected.

Two permissions are needed on first use: "Display over other apps" (required, keeps the screen on) and notifications (optional; keep-awake still works without it, you just won't see the notification or the Super Island).

> Why not a wake lock: HyperOS ignores `SCREEN_BRIGHT_WAKE_LOCK` from background apps, so the lock stops working a few seconds after the app leaves the foreground. `FLAG_KEEP_SCREEN_ON` on a shown window is always honoured.
