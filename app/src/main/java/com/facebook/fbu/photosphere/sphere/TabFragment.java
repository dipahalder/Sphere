// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.fbu.photosphere.sphere;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Placeholder class for the Tab View
 */
public class TabFragment extends Fragment {

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState) {

    View v = inflater.inflate(R.layout.tab_fragment, container, false);
    TextView tv = (TextView) v.findViewById(R.id.text);
    tv.setText(this.getTag() + " Content");

    return v;
  }
}
