/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.tv.settings.about;

import android.os.Build;
import android.os.Bundle;
import androidx.annotation.Keep;
import androidx.preference.Preference;
import com.android.tv.settings.R;
import com.android.tv.settings.SettingsPreferenceFragment;

/**
 * Settings &gt; About &gt; About JetsonTV.
 *
 * Says what this build is, what licence it carries, where its source lives and
 * which projects it is built on. The one thing it deliberately does NOT do is
 * carry its own copy of the licence texts: the "Open source licences" entry
 * fires android.settings.LICENSE, which renders the NOTICE file the build
 * generates from every module's declared license_text. A hand-maintained copy
 * would drift from the tree, and the tree is the thing that ships.
 */
@Keep
public class JetsonTvAboutFragment extends SettingsPreferenceFragment {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.jetsontv_about, null);

        final Preference build = findPreference("jetsontv_build");
        if (build != null) {
            /* Build.DISPLAY is the full LineageOS build id; the date is what a
               user reading a bug report actually needs. */
            build.setSummary(getString(R.string.jetsontv_build_summary,
                    Build.DISPLAY, Build.VERSION.RELEASE));
        }
    }

    @Override
    protected int getPageId() {
        return 0;
    }
}
