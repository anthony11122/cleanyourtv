package com.gathin.tvhome;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.provider.Settings;
import android.util.AtomicFile;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * Apps the owner hid from Home, either one at a time from the app library or the tile menu, or by the
 * default system-app filter. Stored like {@link LibraryApps}: one package per line in files/hidden_apps.txt,
 * written through AtomicFile, and an unreadable file is never replaced with an empty one.
 */
final class HiddenApps {
    static final int MAX_HIDDEN = 128;
    private static final int MAX_BYTES = MAX_HIDDEN * 256;
    private static final String TAG = "TvHome.Hidden";
    private static final String FILE_NAME = "hidden_apps.txt";

    private HiddenApps() { }

    private static AtomicFile store(Context context) {
        return new AtomicFile(new File(context.getFilesDir(), FILE_NAME));
    }

    static Set<String> load(Context context) {
        try {
            return readStrict(store(context));
        } catch (IOException | RuntimeException e) {
            Log.w(TAG, "Could not read hidden apps", e);
            return Collections.emptySet();
        }
    }

    static boolean contains(Context context, String pkg) {
        return pkg != null && load(context).contains(pkg);
    }

    private static Set<String> readStrict(AtomicFile file) throws IOException {
        if (!file.getBaseFile().exists() && !new File(file.getBaseFile() + ".bak").exists())
            return Collections.emptySet();
        try (FileInputStream input = file.openRead()) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int count;
            while ((count = input.read(buffer)) != -1) {
                if (bytes.size() + count > MAX_BYTES) throw new IOException("Hidden app file too large");
                bytes.write(buffer, 0, count);
            }
            return decode(new String(bytes.toByteArray(), StandardCharsets.UTF_8));
        }
    }

    /** Hides or shows one package. Returns false, leaving the file as it was, when it cannot be read or written. */
    static boolean save(Context context, String pkg, boolean hidden) {
        AtomicFile file = store(context);
        Set<String> saved;
        try {
            saved = readStrict(file);
        } catch (IOException | RuntimeException e) {
            // Never replace unreadable owner choices with an empty list on the next edit.
            Log.w(TAG, "Keeping unreadable hidden list unchanged", e);
            return false;
        }
        return write(file, saved, pkg, hidden);
    }

    private static boolean write(AtomicFile file, Set<String> saved, String pkg, boolean hidden) {
        FileOutputStream output = null;
        try {
            Set<String> next = change(saved, pkg, hidden);
            byte[] bytes = encode(next).getBytes(StandardCharsets.UTF_8);
            output = file.startWrite();
            output.write(bytes);
            file.finishWrite(output);
            return true;
        } catch (IOException | RuntimeException e) {
            if (output != null) file.failWrite(output);
            Log.w(TAG, "Could not save hidden app", e);
            return false;
        }
    }

    /** FLAG_SYSTEM or FLAG_UPDATED_SYSTEM_APP, the same test Settings uses for "system app". */
    static boolean isSystem(ApplicationInfo info) {
        return info != null
                && (info.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
    }

    /**
     * System packages Home hides by default. Two exemptions: the app that handles Settings.ACTION_SETTINGS
     * (so the TV's own settings stay reachable) and every package the owner added to Home from the library.
     * A failure to enumerate packages hides nothing, rather than leaving the owner without a launcher.
     */
    static Set<String> systemHidden(Context context, boolean enabled) {
        if (!enabled) return Collections.emptySet();
        PackageManager pm = context.getPackageManager();
        Set<String> exempt = new HashSet<>(LibraryApps.load(context));
        String settings = settingsPackage(pm);
        if (settings != null) exempt.add(settings);
        Set<String> hidden = new HashSet<>();
        try {
            for (ApplicationInfo info : pm.getInstalledApplications(0)) {
                if (info == null || !isSystem(info)) continue;
                if (info.packageName.equals(context.getPackageName()) || exempt.contains(info.packageName)) continue;
                hidden.add(info.packageName);
            }
        } catch (RuntimeException e) {
            Log.w(TAG, "Could not list system apps; leaving them visible", e);
            return Collections.emptySet();
        }
        return hidden;
    }

    /** The package handling Settings.ACTION_SETTINGS, or null when the TV names none (then nothing is exempt). */
    private static String settingsPackage(PackageManager pm) {
        try {
            ResolveInfo info = pm.resolveActivity(new Intent(Settings.ACTION_SETTINGS), 0);
            return info != null && info.activityInfo != null ? info.activityInfo.packageName : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Everything Home must not show: hidden_apps, the owner's choices, and the default system-app filter. */
    static Set<String> effective(Context context, Collection<String> configured, boolean hideSystem) {
        Set<String> all = new HashSet<>(configured);
        all.addAll(load(context));
        if (hideSystem) all.addAll(systemHidden(context, true));
        return all;
    }

    static Set<String> change(Set<String> saved, String pkg, boolean hidden) {
        if (!LibraryApps.validPackage(pkg)) throw new IllegalArgumentException("Invalid package");
        Set<String> next = new HashSet<>(saved);
        if (hidden) next.add(pkg); else next.remove(pkg);
        if (next.size() > MAX_HIDDEN) throw new IllegalArgumentException("Too many hidden apps");
        return next;
    }

    static String encode(Set<String> hidden) {
        if (hidden.size() > MAX_HIDDEN) throw new IllegalArgumentException("Too many hidden apps");
        StringBuilder text = new StringBuilder();
        for (String pkg : new TreeSet<>(hidden)) {
            if (!LibraryApps.validPackage(pkg)) throw new IllegalArgumentException("Invalid package");
            text.append(pkg).append('\n');
        }
        return text.toString();
    }

    static Set<String> decode(String text) {
        if (text.length() > MAX_BYTES) throw new IllegalArgumentException("Hidden app file too large");
        Set<String> result = new HashSet<>();
        int start = 0;
        for (int i = 0; i <= text.length(); i++) {
            if (i < text.length() && text.charAt(i) != '\n') continue;
            if (i == start && i == text.length()) break;
            String pkg = text.substring(start, i);
            if (!LibraryApps.validPackage(pkg) || !result.add(pkg) || result.size() > MAX_HIDDEN)
                throw new IllegalArgumentException("Invalid hidden app file");
            start = i + 1;
        }
        return result;
    }
}
