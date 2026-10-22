package com.gathin.tvhome;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.UserManager;
import android.util.Log;

/**
 * Brings Home up after a reboot without a timer outside the app.
 *
 * One startActivity at boot is not enough on VIDDA. LOCKED_BOOT_COMPLETED comes before the CE storage is
 * unlocked, so MainActivity cannot start yet; BOOT_COMPLETED comes while the vendor's boot animation is still
 * on screen, and Android 11+ drops a background activity start (BAL) from a receiver unless the app holds
 * SYSTEM_ALERT_WINDOW. Either way the old code gave up silently.
 *
 * So a boot broadcast only arms a series of one-shot alarms. Each one calls attempt(): it does nothing while
 * the user is still locked or while Home is already in front, and otherwise asks for Home again. The offsets
 * cover the slow cases (a cold set that takes tens of seconds to finish booting) while still trying early.
 */
final class BootStart {

    private static final String TAG = "BootStart";

    /** Offsets from the boot broadcast, in ms: early enough to beat the stock "no launcher" screen, late
     *  enough to survive a slow unlock and the vendor's tile animation. */
    private static final long[] RETRY_MS = {4000, 12000, 25000, 45000, 70000};

    private BootStart() {
    }

    static void schedule(Context context) {
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarms == null) return;
        long now = System.currentTimeMillis();
        for (int i = 0; i < RETRY_MS.length; i++) {
            PendingIntent pi = retryIntent(context, i);
            long at = now + RETRY_MS[i];
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi);
                } else {
                    alarms.setExact(AlarmManager.RTC_WAKEUP, at, pi);
                }
            } catch (Exception e) {
                Log.w(TAG, "schedule failed at " + RETRY_MS[i] + " ms", e);
            }
        }
        Log.i(TAG, "scheduled " + RETRY_MS.length + " retries");
    }

    private static PendingIntent retryIntent(Context context, int index) {
        Intent intent = new Intent(context, BootRetryReceiver.class)
                .setAction(BootRetryReceiver.ACTION_RETRY);
        return PendingIntent.getBroadcast(context, 1000 + index, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** True once the CE storage is unlocked, which is when MainActivity can actually be started. */
    static boolean isUnlocked(Context context) {
        UserManager users = (UserManager) context.getSystemService(Context.USER_SERVICE);
        return users == null || users.isUserUnlocked();
    }

    /** One retry: start Home unless it is already in front or still cannot start. */
    static void attempt(Context context) {
        Context app = context.getApplicationContext();
        if (!isUnlocked(app)) {
            Log.i(TAG, "attempt skipped: user locked");
            return;
        }
        if (HomeState.isResumed()) {
            Log.i(TAG, "attempt skipped: Home already resumed");
            return;
        }
        Intent home = new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .setPackage(app.getPackageName())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try {
            app.startActivity(home);
            Log.i(TAG, "attempt: started Home");
        } catch (Exception e) {
            // Logged, not swallowed: without SYSTEM_ALERT_WINDOW this is where a BAL block shows up.
            Log.w(TAG, "attempt: startActivity failed", e);
        }
    }
}
