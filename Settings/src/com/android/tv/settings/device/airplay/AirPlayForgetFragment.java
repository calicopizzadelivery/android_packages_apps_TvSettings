/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.tv.settings.device.airplay;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.tv.settings.BaseSettingsFragment;
import com.android.tv.settings.R;
import com.android.tv.settings.SettingsPreferenceFragment;
import com.android.tv.twopanelsettings.TwoPanelSettingsFragment;

/** Confirms forgetting one paired AirPlay device, or all of them. */
@Keep
public class AirPlayForgetFragment extends SettingsPreferenceFragment {

    private static final String ARG_KEY = "airplay_key";
    private static final String ARG_NAME = "airplay_name";
    private static final String KEY_FORGET = "forget";
    private static final String KEY_CANCEL = "cancel";

    /** A null key means every device. */
    static void prepareArgs(Bundle args, @Nullable String key, @Nullable String name) {
        args.putString(ARG_KEY, key);
        args.putString(ARG_NAME, name);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context themed = getPreferenceManager().getContext();
        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(themed);
        boolean all = getArguments() == null || getArguments().getString(ARG_KEY) == null;
        screen.setTitle(all ? getString(R.string.airplay_forget_all_title)
                : getString(R.string.airplay_forget_title, getArguments().getString(ARG_NAME)));

        Preference message = new Preference(themed);
        message.setTitle(all ? R.string.airplay_forget_all_message
                : R.string.airplay_forget_message);
        message.setSelectable(false);
        screen.addPreference(message);

        Preference forget = new Preference(themed);
        forget.setKey(KEY_FORGET);
        forget.setTitle(R.string.airplay_forget);
        screen.addPreference(forget);

        Preference cancel = new Preference(themed);
        cancel.setKey(KEY_CANCEL);
        cancel.setTitle(android.R.string.cancel);
        screen.addPreference(cancel);

        setPreferenceScreen(screen);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (KEY_FORGET.equals(preference.getKey())) {
            AirPlayClient.forget(getContext(),
                    getArguments() == null ? null : getArguments().getString(ARG_KEY));
            back();
            return true;
        }
        if (KEY_CANCEL.equals(preference.getKey())) {
            back();
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    private void back() {
        if (getCallbackFragment() instanceof TwoPanelSettingsFragment) {
            TwoPanelSettingsFragment parent = (TwoPanelSettingsFragment) getCallbackFragment();
            if (parent.isFragmentInTheMainPanel(this)) {
                parent.navigateBack();
            }
        } else if (getCallbackFragment() instanceof BaseSettingsFragment) {
            ((BaseSettingsFragment) getCallbackFragment()).navigateBack();
        }
    }
}
