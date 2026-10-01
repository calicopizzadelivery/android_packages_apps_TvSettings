/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.tv.settings.device.airplay;

import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Keep;
import androidx.preference.Preference;

import com.android.tv.settings.R;
import com.android.tv.settings.RadioPreference;
import com.android.tv.settings.SettingsPreferenceFragment;

/**
 * Settings > System > AirPlay > New devices: ask for a code, or not. A page
 * of its own rather than a switch, so that turning the code off is a
 * deliberate choice made with its consequence in view.
 */
@Keep
public class AirPlayCodeFragment extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private static final String KEY_ON = "code_on";
    private static final String KEY_OFF = "code_off";

    /** This page is also the preview beside AirPlayFragment, so follow changes. */
    private final ContentObserver mObserver =
            new ContentObserver(new Handler(Looper.getMainLooper())) {
                @Override
                public void onChange(boolean selfChange) {
                    refresh();
                }
            };

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.airplay_code, null);
        findPreference(KEY_ON).setOnPreferenceChangeListener(this);
        findPreference(KEY_OFF).setOnPreferenceChangeListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        try {
            getContext().getContentResolver().registerContentObserver(
                    AirPlayClient.URI, false, mObserver);
        } catch (SecurityException e) {
            // Still shows the state as of now; it just will not follow it.
        }
        refresh();
    }

    @Override
    public void onStop() {
        getContext().getContentResolver().unregisterContentObserver(mObserver);
        super.onStop();
    }

    private void refresh() {
        AirPlayClient.State state = getContext() == null ? null : AirPlayClient.get(getContext());
        if (state != null) {
            ((RadioPreference) findPreference(KEY_ON)).setChecked(state.requireCode);
            ((RadioPreference) findPreference(KEY_OFF)).setChecked(!state.requireCode);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        RadioPreference radio = (RadioPreference) preference;
        if (radio.isChecked()) {
            return false;
        }
        radio.clearOtherRadioPreferences(getPreferenceScreen());
        AirPlayClient.setRequireCode(getContext(), KEY_ON.equals(radio.getKey()));
        radio.setChecked(true);
        return true;
    }
}
