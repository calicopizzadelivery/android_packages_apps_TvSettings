/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.tv.settings.device.airplay;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;

/**
 * The AirPlay receiver's settings, which its own app keeps: whether it runs,
 * whether new devices must enter a code, and which devices have paired.
 * Everything goes through that app's settings provider, and its service to
 * start and stop it, as the launcher's Streaming tile does, so the two always
 * agree. The provider announces every change on {@link #URI}.
 */
public final class AirPlayClient {

    private static final String TAG = "AirPlayClient";
    private static final String PACKAGE = "org.lineageos.tv.airplay";
    private static final String AUTHORITY = "org.lineageos.tv.airplay.settings";
    private static final ComponentName SERVICE =
            new ComponentName(PACKAGE, PACKAGE + ".AirPlayService");

    /** Observe this to hear about any change, made here or anywhere else. */
    static final Uri URI = Uri.parse("content://" + AUTHORITY);

    /** The state as the receiver reports it. */
    static final class State {
        final boolean enabled;
        final boolean requireCode;
        final boolean autoDream;
        final String name;
        /** Paired devices: keys and names in step, ordered by name. */
        final String[] keys;
        final String[] names;

        private State(Bundle b) {
            enabled = b.getBoolean("enabled");
            requireCode = b.getBoolean("require_pin", true);
            autoDream = b.getBoolean("auto_dream", true);
            name = b.getString("name", "");
            String[] k = b.getStringArray("paired_keys");
            String[] n = b.getStringArray("paired_names");
            keys = k == null ? new String[0] : k;
            names = n == null || n.length != keys.length ? new String[keys.length] : n;
        }
    }

    private AirPlayClient() {
    }

    /** Whether this build has the receiver at all. */
    public static boolean isInstalled(Context context) {
        return context.getPackageManager().resolveContentProvider(AUTHORITY, 0) != null;
    }

    @Nullable
    static State get(Context context) {
        return call(context, "get", null, null);
    }

    /** A running receiver restarts, so this applies at once. */
    @Nullable
    static State setRequireCode(Context context, boolean on) {
        Bundle extras = new Bundle();
        extras.putBoolean("value", on);
        return call(context, "set_require_pin", null, extras);
    }
    static State setAutoDream(Context context, boolean on) {
        Bundle extras = new Bundle();
        extras.putBoolean("value", on);
        return call(context, "set_auto_dream", null, extras);
    }


    /** A null key forgets every device. */
    @Nullable
    static State forget(Context context, @Nullable String key) {
        return call(context, "forget", key, null);
    }

    /**
     * Settings is in the foreground, so a plain startService is allowed, and
     * is right for stopping too: see the Streaming tile.
     */
    static void setEnabled(Context context, boolean on) {
        try {
            context.startService(new Intent(PACKAGE + (on ? ".START" : ".STOP"))
                    .setComponent(SERVICE));
        } catch (RuntimeException e) {
            Log.e(TAG, "could not " + (on ? "start" : "stop") + " the receiver", e);
        }
    }

    @Nullable
    private static State call(Context context, String method, @Nullable String arg,
            @Nullable Bundle extras) {
        try {
            Bundle state = context.getContentResolver().call(AUTHORITY, method, arg, extras);
            return state == null ? null : new State(state);
        } catch (RuntimeException e) {
            Log.e(TAG, AUTHORITY + ": " + method + " failed", e);
            return null;
        }
    }
}
