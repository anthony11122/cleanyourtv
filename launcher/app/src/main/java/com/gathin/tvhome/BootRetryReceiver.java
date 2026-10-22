package com.gathin.tvhome;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * One alarm from BootStart. Not exported and with no intent-filter: only our own alarms reach it, so nothing
 * else on the TV can ask Home to come up.
 */
public class BootRetryReceiver extends BroadcastReceiver {

    static final String ACTION_RETRY = "com.gathin.tvhome.action.BOOT_RETRY";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION_RETRY.equals(intent.getAction())) return;
        BootStart.attempt(context.getApplicationContext());
    }
}
