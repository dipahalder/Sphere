// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.fbu.photosphere.sphere;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * NUX introduction slide with fade in fade out animation on first time application is installed
 * NUXPagerActivity launched after execution
 */
public class NUXIntroActivity extends Activity {
  private static final long DELAY = 3000;
  private Animation mFadeInAnimation;
  private TextView mCaption;
  private ImageView mImage;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.nux_intro);
    mCaption = (TextView) findViewById(R.id.caption);
    mImage = (ImageView) findViewById(R.id.image);
    FontUtils.applyOpenSans(this, mCaption);
    mFadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in);
  }

  @Override
  protected void onResume() {
    super.onResume();
    mCaption.startAnimation(mFadeInAnimation);
    mImage.startAnimation(mFadeInAnimation);
    handleAnimation(this);
  }

  private void handleAnimation(final Activity activity) {
    new Handler().postDelayed(
      new Runnable() {
        @Override
        public void run() {
        Intent nuxIntent = new Intent(activity, NUXPagerActivity.class);
        startActivity(nuxIntent);
        activity.finish();
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        }
      },
      DELAY);
  }
}
