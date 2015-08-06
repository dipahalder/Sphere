// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.fbu.photosphere.sphere;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;

public class TouchCatcherFrameLayout extends FrameLayout {

  private InputMethodManager mInputMethodManager;

  public TouchCatcherFrameLayout(Context context) {
    this(context, null);
  }

  public TouchCatcherFrameLayout(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public TouchCatcherFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    setFocusable(true);
    setFocusableInTouchMode(true);

    mInputMethodManager = (InputMethodManager) context
        .getSystemService(Activity.INPUT_METHOD_SERVICE);
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (event.getAction() == MotionEvent.ACTION_DOWN) {
      mInputMethodManager.hideSoftInputFromWindow(getWindowToken(), 0);
    }
    return false;
  }
}
