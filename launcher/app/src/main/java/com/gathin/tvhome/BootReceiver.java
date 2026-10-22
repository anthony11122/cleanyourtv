package com.gathin.tvhome;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.UserManager;
import android.util.Log;

/**
 * Records the boot broadcasts; it does not start Home.
 *
 * The handover disables the stock launcher, which was the only package VIDAA's boot sequence asked to come up,
 * so the TV would sit on a black screen. Starting Home from here does not work reliably: a startActivity from a
 * boot receiver is a background activity start (BAL), which Android 11 drops unless the app holds
 * SYSTEM_ALERT_WINDOW, and this firmware revokes that permission on every boot. On some cold starts nothing of
 * ours receives BOOT_COMPLETED at all.
 *
 * HomeKeyService is bound by the system at boot regardless, so BootHome does the starting from there. This
 * receiver stays only for the log line: it tells from a bug report whether the broadcast arrived at all.
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
        boolean unlocked = context.getSystemService(UserManager.class).isUserUnlocked();
        Log.i("BootReceiver", "action=" + action + " unlocked=" + unlocked);
    }
}
