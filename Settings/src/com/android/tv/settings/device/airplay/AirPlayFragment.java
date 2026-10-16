/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.tv.settings.device.airplay;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Keep;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.TwoStatePreference;

import com.android.tv.settings.R;
import com.android.tv.settings.SettingsPreferenceFragment;
import com.android.tv.twopanelsettings.TwoPanelSettingsFragment;

import java.util.Arrays;

/**
 * Settings > System > AirPlay: the receiver's switch, the name iPhones and
 * Macs show, whether new devices must enter a code, and the paired devices.
 * The receiver's app keeps all of it (see {@link AirPlayClient}); this page
 * only shows it and asks for changes, and follows changes made elsewhere,
 * such as from the launcher's Streaming tile or a device pairing.
 */
@Keep
public class AirPlayFragment extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    private static final String TAG = "AirPlayFragment";
    private static final String KEY_ENABLED = "airplay_enabled";
    private static final String KEY_NAME = "airplay_name";
    private static final String KEY_CODE = "airplay_code";
    private static final String KEY_OPEN_AUDIO = "airplay_open_audio";
    private static final String KEY_DREAM = "airplay_dream";
    private static final String KEY_PAIRED = "airplay_paired";

    private final ContentObserver mObserver =
            new ContentObserver(new Handler(Looper.getMainLooper())) {
                @Override
                public void onChange(boolean selfChange) {
                    refresh();
                }
            };

    /** The paired devices last shown, so the list is only rebuilt when it changes. */
    private String mShownDevices;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.airplay, null);
        findPreference(KEY_ENABLED).setOnPreferenceChangeListener(this);
        findPreference(KEY_DREAM).setOnPreferenceChangeListener(this);
        findPreference(KEY_OPEN_AUDIO).setOnPreferenceChangeListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        try {
            getContext().getContentResolver().registerContentObserver(
                    AirPlayClient.URI, false, mObserver);
        } catch (SecurityException e) {
            Log.e(TAG, "cannot follow the receiver's changes", e);
        }
        refresh();
    }

    @Override
    public void onStop() {
        getContext().getContentResolver().unregisterContentObserver(mObserver);
        super.onStop();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (KEY_OPEN_AUDIO.equals(preference.getKey())) {
            AirPlayClient.setOpenAudio(getContext(), (Boolean) newValue);
            return true;
        }
        if (KEY_DREAM.equals(preference.getKey())) {
            // Read when a stream starts, so this applies to the next one and
            // never interrupts one in progress.
            AirPlayClient.setAutoDream(getContext(), (Boolean) newValue);
            return true;
        }
        // The switch moves now; the receiver confirms through the observer,
        // and refresh() puts it back if it did not start.
        AirPlayClient.setEnabled(getContext(), (Boolean) newValue);
        return true;
    }

    private void refresh() {
        Context context = getContext();
        AirPlayClient.State state = context == null ? null : AirPlayClient.get(context);
        if (state == null) {
            return;
        }
        ((TwoStatePreference) findPreference(KEY_ENABLED)).setChecked(state.enabled);
        findPreference(KEY_NAME).setSummary(state.name);
        findPreference(KEY_CODE).setSummary(state.requireCode
                ? R.string.airplay_code_summary_on : R.string.airplay_code_summary_off);
        ((TwoStatePreference) findPreference(KEY_DREAM)).setChecked(state.autoDream);
        ((TwoStatePreference) findPreference(KEY_OPEN_AUDIO)).setChecked(state.openAudio);

        String devices = Arrays.toString(state.keys) + Arrays.toString(state.names);
        if (devices.equals(mShownDevices)) {
            return;
        }
        mShownDevices = devices;
        rebuildPaired(state);
        refreshPreview();
    }

    private void rebuildPaired(AirPlayClient.State state) {
        PreferenceCategory paired = findPreference(KEY_PAIRED);
        paired.removeAll();
        Context themed = getPreferenceManager().getContext();
        if (state.keys.length == 0) {
            Preference none = new Preference(themed);
            none.setTitle(R.string.airplay_paired_none);
            none.setSummary(R.string.airplay_paired_none_summary);
            none.setSelectable(false);
            paired.addPreference(none);
            return;
        }
        for (int i = 0; i < state.keys.length; i++) {
            String name = TextUtils.isEmpty(state.names[i])
                    ? getString(R.string.airplay_unnamed_device) : state.names[i];
            Preference device = new Preference(themed);
            // Keyed by the device, not its position: the two-panel view keeps
            // a row's preview while its key is unchanged, so a positional key
            // would go on previewing a device that has just been forgotten.
            device.setKey("device_" + state.keys[i]);
            device.setTitle(name);
            device.setFragment(AirPlayForgetFragment.class.getName());
            AirPlayForgetFragment.prepareArgs(device.getExtras(), state.keys[i], name);
            paired.addPreference(device);
        }
        if (state.keys.length > 1) {
            Preference all = new Preference(themed);
            all.setKey("forget_all");
            all.setTitle(R.string.airplay_forget_all);
            all.setFragment(AirPlayForgetFragment.class.getName());
            AirPlayForgetFragment.prepareArgs(all.getExtras(), null, null);
            paired.addPreference(all);
        }
    }

    /**
     * The rows just changed under the focus, typically because a device was
     * forgotten from its own page. Going back from that page leaves it in the
     * preview panel, so ask for a preview of whichever row has the focus now,
     * once the list has settled.
     */
    private void refreshPreview() {
        if (!(getCallbackFragment() instanceof TwoPanelSettingsFragment) || getListView() == null) {
            return;
        }
        TwoPanelSettingsFragment parent = (TwoPanelSettingsFragment) getCallbackFragment();
        getListView().post(() -> {
            if (!isAdded()) {
                return;
            }
            Preference chosen = TwoPanelSettingsFragment.getChosenPreference(this);
            if (chosen != null) {
                parent.refocusPreferenceForceRefresh(chosen, this);
            }
        });
    }
}
