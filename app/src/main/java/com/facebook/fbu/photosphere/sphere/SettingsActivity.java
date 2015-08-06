// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.fbu.photosphere.sphere;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.widget.TextView;

import com.facebook.fbu.photosphere.sphere.api.SphereAPI;

/**
 * Handles all the settings and preferences of the app
 */
public class SettingsActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        // changes the title of the toolbar
        TextView tv = (TextView) findViewById(R.id.title_text_view);
        FontUtils.applyOpenSans(this, tv);
        getFragmentManager().beginTransaction().replace(
                android.R.id.content,
                new MyPreferenceFragment())
                .commit();
    }

    public static class MyPreferenceFragment extends PreferenceFragment {

        private static final String PREF_LOGOUT_KEY = "log_out_preference";
        private static final String PREF_RESET_PASSWORD_KEY = "reset_password_preference";
        private static final String PREF_EDUCATE_KEY = "educate_preference";
        private Preference mLogout;
        private Preference mResetPassword;
        private Preference mEditProfile;
        private Preference mEducate;

        @Override
        public void onCreate(final Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
            mLogout = findPreference(PREF_LOGOUT_KEY);
            mLogout.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    SphereAPI.logout(getActivity());
                    return true;
                }
            });

            mResetPassword = findPreference(PREF_RESET_PASSWORD_KEY);
            mResetPassword.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent editPasswordIntent = new Intent(
                            getActivity(),
                            ResetPasswordActivity.class);
                    startActivity(editPasswordIntent);
                    return true;
                }
            });

            mEducate = findPreference(PREF_EDUCATE_KEY);
            mEducate.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent startNUXIntent = new Intent(getActivity(), NUXPagerActivity.class);
                    startActivity(startNUXIntent);
                    return true;
                }
            });
        }
    }
}
