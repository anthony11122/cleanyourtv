package com.gathin.tvhome;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Schedules the boot start of Home after a reboot.
 *
 * The handover disables the stock launcher (com.jamdeo.tv.vod), which was the only package with a boot
 * receiver on this firmware: VIDAA's boot sequence asks that package to come up, finds nothing, and the
 * TV sits on a black screen until someone presses Home. This receiver only arms a set of alarms; the
 * starting itself is left to BootStart, because a startActivity from here is unreliable:
 *
 * - LOCKED_BOOT_COMPLETED arrives before the CE storage is unlocked, and MainActivity lives in CE, so any
 *   start from that broadcast fails. The old code swallowed the exception and never tried again.
 * - BOOT_COMPLETED arrives while the vendor's boot animation still owns the screen, and Android 11+ blocks
 *   background activity starts (BAL) unless the app holds SYSTEM_ALERT_WINDOW.
 *
 * Alarms fire repeatedly instead, so a start that fails at 4 s is retried at 12 s, 25 s, 45 s and 70 s, and
 * ACTION_USER_UNLOCKED re-arms the series at the moment CE really becomes usable.
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();
        if (action == null) return;
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_USER_UNLOCKED.equals(action)
                && !"android.intent.action.HISENSE_BOOT_COMPLETED".equals(action)) return;
        Context app = context.getApplicationContext();
        boolean unlocked = BootStart.isUnlocked(app);
        Log.i("BootReceiver", "action=" + action + " unlocked=" + unlocked);
        BootStart.schedule(app);
    }
}
