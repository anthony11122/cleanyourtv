package com.gathin.tvhome;

import android.accessibilityservice.AccessibilityService;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

/**
 * Brings Home up from the accessibility service instead of from a broadcast.
 *
 * A startActivity from a boot receiver is not reliable on VIDDA: Android 11 drops a background activity start
 * (BAL) unless the app holds SYSTEM_ALERT_WINDOW, and this firmware revokes that permission on every boot
 * ("AppPermissionGroup: SYSTEM_ALERT_WINDOW revokeRuntimePermission"). The boot receiver is also not guaranteed
 * to run at all: on some cold starts nothing of ours receives BOOT_COMPLETED.
 *
 * The accessibility service is the one component the firmware does bind at boot, even while the screen is black.
 * performGlobalAction(GLOBAL_ACTION_HOME) is a system-level action: it does not go through startActivity, so BAL
 * does not apply, and it needs no permission beyond the service being enabled.
 *
 * The retries are Handler callbacks, not AlarmManager alarms: an alarm outlives the process that armed it and is
 * useless once the service is gone, while a posted callback simply dies with the service, which is the lifetime
 * we actually care about here.
 */
final class BootHome {

    private static final String TAG = "BootHome";

    /** Offsets from the service connection, in ms: the first lands after the vendor boot animation, the last
     *  covers a cold set that takes a minute to finish booting. */
    private static final long[] DELAYS_MS = {3000, 10000, 22000, 40000, 65000};

    /** Main-thread handler for the retries, so onDestroy can drop them when the service goes away. */
    private static Handler handler;

    private BootHome() {
    }

    /** Posts the retry series. Calling it again on a rebind is harmless: each check looks before it acts. */
    static void schedule(AccessibilityService svc) {
        cancel();
        handler = new Handler(Looper.getMainLooper());
        for (long delay : DELAYS_MS) {
            handler.postDelayed(() -> ensureHomeShown(svc), delay);
        }
        Log.i(TAG, "scheduled " + DELAYS_MS.length + " checks");
    }

    /** Drops pending checks; the service that would run them is going away. */
    static void cancel() {
        if (handler == null) return;
        handler.removeCallbacksAndMessages(null);
        handler = null;
    }

    /** Puts Home in front unless it already is. Every branch logs, so logcat alone tells what happened. */
    static void ensureHomeShown(AccessibilityService svc) {
        if (HomeState.isResumed()) {
            // Home is up, so the remaining checks have nothing left to do. Dropping them matters: a late check
            // would pull Home back over an app the viewer opened in the meantime.
            Log.i(TAG, "check: Home already resumed, dropping the rest");
            cancel();
            return;
        }
        String front = frontPackage(svc);
        if (svc.getPackageName().equals(front)) {
            Log.i(TAG, "check: Home is already the front window (" + front + "), dropping the rest");
            cancel();
            return;
        }
        // A null root means the window could not be read (the service has no canRetrieveWindowContent), which
        // is exactly the black-screen case this exists for: ask for Home anyway.
        boolean done = svc.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);
        Log.i(TAG, "check: front=" + front + " GLOBAL_ACTION_HOME returned " + done);
    }

    /** Package of the window in front, or null when it cannot be read. */
    private static String frontPackage(AccessibilityService svc) {
        AccessibilityNodeInfo root = null;
        try {
            root = svc.getRootInActiveWindow();
            if (root == null) return null;
            CharSequence name = root.getPackageName();
            return name == null ? null : name.toString();
        } catch (RuntimeException unreadable) {
            Log.w(TAG, "front window unavailable", unreadable);
            return null;
        } finally {
            if (root != null) root.recycle();
        }
    }
}
