package com.gathin.tvhome;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

/**
 * Starts Home after a reboot.
 *
 * The handover disables the stock launcher (com.jamdeo.tv.vod), which was the only package with a boot
 * receiver on this firmware: VIDAA's boot sequence asks that package to come up, finds nothing, and the
 * TV sits on a black screen until someone presses Home. This receiver asks for Home instead.
 *
 * The start is delayed a few seconds: the vendor's tile animation runs on top of the boot and a Home
 * activity started too early is pushed behind it on VIDAA 6.
 */
public class BootReceiver extends BroadcastReceiver {

    /** How long to wait for the vendor's boot tile animation to finish. */
    private static final long DELAY_MS = 4000;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();
        if (action == null) return;
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action)
                && !Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action)
                && !"android.intent.action.HISENSE_BOOT_COMPLETED".equals(action)) return;
        final Context app = context.getApplicationContext();
        new Handler(Looper.getMainLooper()).postDelayed(() -> startHome(app), DELAY_MS);
    }

    private static void startHome(Context context) {
        Intent home = new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .setPackage(context.getPackageName())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try {
            context.startActivity(home);
        } catch (Exception ignored) {
            // Let the system bring Home up on its own.
        }
    }
}
