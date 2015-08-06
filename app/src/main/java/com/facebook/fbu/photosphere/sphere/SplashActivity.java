// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.fbu.photosphere.sphere;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Marked as Launch Activity in the Application Manifest
 */
public class SplashActivity extends Activity {
  private static final long DELAY = 2000;
  private static final String PREF_FIRST_TIME_KEY = "firstTime";
  private boolean mScheduled = false;
  private Timer mSplashTimer;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.splash);
    TextView logo = (TextView) findViewById(R.id.splash_text);
    FontUtils.applyOpenSans(this, logo);

    mSplashTimer = new Timer();
    mSplashTimer.schedule(
        new TimerTask() {
        @Override
        public void run() {
          Activity activity = SplashActivity.this;
          activity.finish();
          //Runs the NUX only the first time the app is installed and opened
          SharedPreferences prefs = PreferenceManager
              .getDefaultSharedPreferences(getApplicationContext());
          if (!prefs.getBoolean(PREF_FIRST_TIME_KEY, false)) {
            startActivity(new Intent(activity, NUXIntroActivity.class));
            prefs.edit().putBoolean(PREF_FIRST_TIME_KEY, true).apply();
          // Else LoginActivity is launched
          } else {
            startActivity(new Intent(activity, DispatchActivity.class));
          }
        }
    },
        DELAY);
    mScheduled = true;
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (mScheduled) {
      mSplashTimer.cancel();
    }
    mSplashTimer.purge();
  }
}
