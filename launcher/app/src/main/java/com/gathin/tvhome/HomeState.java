package com.gathin.tvhome;

/**
 * Whether Home is in front right now, for the boot retries.
 *
 * A retry that starts Home while it is already showing would take the focus away from whatever the viewer is
 * doing (a card, a dialog, the app library), so BootHome asks here first instead of sending
 * GLOBAL_ACTION_HOME blindly on every retry.
 */
final class HomeState {

    private static volatile boolean resumed;

    private HomeState() {
    }

    static void setResumed(boolean value) {
        resumed = value;
    }

    static boolean isResumed() {
        return resumed;
    }
}
