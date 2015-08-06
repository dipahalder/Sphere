// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.fbu.photosphere.sphere;

import android.content.Context;
import android.graphics.Typeface;
import android.widget.TextView;

/**
 * Centralized utility class for font changes
 * Currently only used for Open Sans by Google Fonts
 */
public class FontUtils {
  private static Typeface sOpenSansTypeface;

  /**
   * Sets the typeface of Text View widgets to Open Sans
   */
  public static void applyOpenSans(Context context, TextView textView) {
    if (sOpenSansTypeface == null) {
      sOpenSansTypeface = Typeface.createFromAsset(context.getAssets(), "fonts/OpenSans-Light.ttf");
    }
    textView.setTypeface(sOpenSansTypeface);
  }
}
