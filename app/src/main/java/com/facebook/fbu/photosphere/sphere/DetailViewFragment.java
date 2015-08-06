// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.fbu.photosphere.sphere;

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.fbu.photosphere.sphere.api.SphereAPI;
import com.facebook.fbu.photosphere.sphere.api.SphereAPI.SpheresLoadedCallback;
import com.facebook.fbu.photosphere.sphere.api.SphereUser;
import com.facebook.fbu.photosphere.spherelib.SphereView;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Intermediate view that is displayed when a user clicks on a pin in the map view
 */
public class DetailViewFragment extends Fragment implements SpheresLoadedCallback {

    private static final String TAG = DetailViewFragment.class.getSimpleName();
    private static final String PHOTO_ID = "PHOTO_ID";
    private static final String IMAGE_URL = "IMAGE_URL";
    private static final String SPHERE = "SPHERE";

    private String mPhotoId;

    private ImageButton mCancelButton;
    private TextView mUsername;
    private TextView mLocation;
    private ImageView mUserIcon;
    private SphereView mPhotosphereThumbnail;
    private FrameLayout mFrameLayout;

    /**
     * opened from a map
     *
     * @param photoId - photoId of photo to be displayed in view
     * @return detail view fragment set to create view for sphere associated with photoId
     */
    public static DetailViewFragment newInstance(String photoId) {
        DetailViewFragment detailViewFragment = new DetailViewFragment();
        Bundle pinData = new Bundle();
        pinData.putString(PHOTO_ID, photoId);
        detailViewFragment.setArguments(pinData);
        return detailViewFragment;
    }

    /**
     * opened from a profile view
     *
     * @param sphere - sphere to create view for
     * @return detail view fragment bundled to open view for sphere
     */
    public static DetailViewFragment newInstance(Sphere sphere) {
        DetailViewFragment detailViewFragment = new DetailViewFragment();
        Bundle pinData = new Bundle();
        pinData.putParcelable(SPHERE, sphere);
        detailViewFragment.setArguments(pinData);
        return detailViewFragment;
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.detail_view_fragment, container, false);
        //Inflate widgets and wire cancel button
        mUsername = (TextView) view.findViewById(R.id.user_name);
        mLocation = (TextView) view.findViewById(R.id.location);
        mUserIcon = (ImageView) view.findViewById(R.id.user_icon);
        mCancelButton = (ImageButton) view.findViewById(R.id.cancel);
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //return to map view
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });
        mPhotosphereThumbnail = (SphereView) view.findViewById(R.id.photosphere_thumbnail);
        mFrameLayout = (FrameLayout) view.findViewById(R.id.user_details);
        Bundle sphereData = getArguments();
        // if created from photoId query for info
        if (sphereData.getString(PHOTO_ID) != null) {
            mPhotoId = sphereData.getString(PHOTO_ID);
            // get the sphere with mPhotoId
            SphereAPI.fetchSphere(mPhotoId, this);
            return view;
        }
        // else get the associated info and create view
        bindSphereToView((Sphere) sphereData.getParcelable(SPHERE), false);
        return view;
    }

    //Convert Longitude and Latitude into a User-Readable Location
    private static String createReadableLocation(
            Context context,
            Double latitude,
            Double longitude) {
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            StringBuilder readableLocation = new StringBuilder();
            if (addresses.size() > 0) {
                Address address = addresses.get(0);
                for (int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                    readableLocation.append(address.getAddressLine(i)).append(", ");
                    readableLocation.append(address.getCountryName());
                    if (readableLocation != null) {
                        return readableLocation.toString();
                    }
                }
            }
        } catch (IOException ioe) {
            Log.e(TAG, "Failed to generate an address", ioe);
        }
        return "";
    }

    @Override
    public void onSpheresLoaded(List<Sphere> photoSpheres) {
        assert photoSpheres.size() == 1;
        final Sphere sphere = photoSpheres.get(0);
        bindSphereToView(sphere, true);
    }

    /**
     * attach sphere information to the view
     *
     * @param sphere              - sphere whose info will be shown in view
     * @param isProfileLaunchable - indicates whether or not profile navigation is allowed
     */
    private void bindSphereToView(final Sphere sphere, boolean isProfileLaunchable) {
        // create new sphere user
        final SphereUser user = SphereUser.from(sphere.getUser());
        // sphere view
        setSphereViewImage(sphere.getImage().getUrl());
        //Username
        mUsername.setText(user.getFullName());
        //Location
        mLocation.setText(createReadableLocation(
                getActivity(),
                sphere.getCoordinates().getLatitude(),
                sphere.getCoordinates().getLongitude()));
        //Icon
        String iconURL = user.getUserPhotoUrl();
        if (iconURL != null) {
            Picasso.with(getActivity()).load(iconURL).into(mUserIcon);
        }
        // if profile is launchable, set on click listener on user details
        if (isProfileLaunchable) {
            // allow for opening of associated profile
            mFrameLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ProfileFragment fragment = ProfileFragment.newInstance(user);
                    FragmentTransaction fragmentTransaction = getActivity()
                            .getSupportFragmentManager()
                            .beginTransaction();
                    fragmentTransaction.add(R.id.realtabcontent, fragment);
                    fragmentTransaction.addToBackStack(null).commit();
                }
            });
        } else {
            mFrameLayout.setOnClickListener(null);
        }
    }

    /**
     * set up sphere view image and on click listener to launch viewer
     *
     * @param thumbnailUrl - url to load into SphereView
     */
    private void setSphereViewImage(final String thumbnailUrl) {
        if (thumbnailUrl == null) {
            return;
        }
        mPhotosphereThumbnail.setImageUrl(thumbnailUrl);
        mPhotosphereThumbnail.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Intent startViewerIntent = new Intent(
                        getActivity(),
                        ViewerActivity.class);
                startViewerIntent.putExtra(IMAGE_URL, thumbnailUrl);
                //keeps activity from being added to history stack
                //return to DetailViewFragment onBackPressed()
                startViewerIntent
                        .setFlags(startViewerIntent.getFlags()
                                | Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(startViewerIntent);
                return true;
            }
        });
    }

    @Override
    public void onSphereLoadFailed() {
        Toast.makeText(getActivity(), R.string.error_loading, Toast.LENGTH_SHORT);
        getActivity().getSupportFragmentManager().popBackStack();
    }
}
