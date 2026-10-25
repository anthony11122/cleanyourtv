package com.gathin.tvhome;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

/**
 * Opens the TV's own source-selection panel, for sets with no TvInputManager (Hisense VIDAA / MT9653, TCL
 * Android TV 9): a service, or an activity on sets whose panel is a dialog (BYINTEK U4's QuickSourceActivity).
 *
 * Shared by the Inputs pill in Home and by InputKeyService, so the remote's Input button works from any app,
 * not only while Home is in front. The intent is built exactly as the pill builds it, including
 * source_panel_extra ("name=value") for panels that need an int extra.
 */
final class SourcePanel {

    private SourcePanel() {
    }

    /**
     * Opens the panel. Returns false when none is configured or nothing on the TV handles it, so the caller
     * can say so instead of leaving the button silently dead.
     */
    static boolean open(Context ctx, String service, String action, String extra) {
        if (service.isEmpty() && action.isEmpty()) return false;
        Intent i = new Intent(action);
        ComponentName c = ComponentName.unflattenFromString(service);
        if (c != null) i.setComponent(c);
        int eq = extra.indexOf('=');
        if (eq > 0) {
            try {
                i.putExtra(extra.substring(0, eq).trim(),
                        Integer.parseInt(extra.substring(eq + 1).trim()));
            } catch (NumberFormatException ignored) {
                // A malformed extra is not a reason to keep the panel closed.
            }
        }
        try {
            if (ctx.getPackageManager().resolveActivity(i, 0) != null) {
                ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                return true;
            }
            // The panel is a service on most of these sets. startService throws on Android 8+ when the app is
            // in the background and the service is not already running, which the caller treats as "not
            // available" rather than crashing the helper.
            return ctx.startService(i) != null;
        } catch (Exception unavailable) {
            return false;
        }
    }
}
