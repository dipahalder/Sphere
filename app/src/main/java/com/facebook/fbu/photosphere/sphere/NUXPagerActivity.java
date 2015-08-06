// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.fbu.photosphere.sphere;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.viewpagerindicator.CirclePageIndicator;

/**
 * Launched after a new user creates an account
 * Consists of a View Pager with 3 slides and a dot-indicator library
 */
public class NUXPagerActivity extends Activity {
  private final int[] mPageArray = new int[] {1, 2, 3};
  private ViewPager mViewPager;
  private TextView mSkipText;
  private CirclePageIndicator mIndicator;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.nux_pager);

    mSkipText = (TextView) findViewById (R.id.nux_skip_text);
    FontUtils.applyOpenSans(this, mSkipText);

    mSkipText.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        startActivity(new Intent(NUXPagerActivity.this, DispatchActivity.class));
      }
    });

    mViewPager = (ViewPager) findViewById(R.id.view_pager);

    NUXPagerAdapter viewPagerAdapter = new NUXPagerAdapter();
    mViewPager.setAdapter(viewPagerAdapter);
    mViewPager.setCurrentItem(0);

    mIndicator = (CirclePageIndicator) findViewById(R.id.circles);
    mIndicator.setViewPager(mViewPager);

    Resources res = getResources();
    mIndicator.setBackgroundColor(res.getColor(R.color.teal));
    mIndicator.setRadius(res.getDimensionPixelSize(R.dimen.indicator_radius));
    mIndicator.setPageColor(res.getColor(R.color.light_medium_grey));
    mIndicator.setFillColor(res.getColor(R.color.white));
    mIndicator.setStrokeWidth(0);

    mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
      @Override
      public void onPageSelected(int position) {
      }

      // Change "Skip" to "Continue" on last slide of NUX
      @Override
      public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        if (position == 2) {
          mSkipText.setText(R.string.continue_text);
        }
      }

      @Override
      public void onPageScrollStateChanged(int arg0) {
      }
    });
  }

  // Case/Switch for each slide of the NUX
  public class NUXPagerAdapter extends PagerAdapter {

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
      View view = getLayoutInflater().inflate(R.layout.nux_page_view, container, false);
      TextView caption = (TextView) view.findViewById(R.id.caption);
      ImageView image = (ImageView) view.findViewById(R.id.image);

      switch (position) {
        case 0:
          image.setImageResource(R.drawable.nux_slide_1);
          caption.setText(R.string.nux_2);
          break;
        case 1:
          image.setImageResource(R.drawable.nux_slide_3);
          caption.setText(R.string.nux_3);
          break;
        case 2:
          image.setImageResource(R.drawable.nux_slide_4);
          caption.setText(R.string.nux_4);
          break;
      }

      FontUtils.applyOpenSans(NUXPagerActivity.this, caption);
      container.addView(view);
      return view;
    }

    @Override
    public int getCount() {
      return mPageArray.length;
    }

    @Override
    public boolean isViewFromObject(View view, Object obj) {
      return view == obj;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
      View view = (View) object;
      container.removeView(view);
    }
  }
}
