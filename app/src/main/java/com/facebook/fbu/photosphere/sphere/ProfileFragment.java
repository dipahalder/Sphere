// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.fbu.photosphere.sphere;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.facebook.fbu.photosphere.sphere.api.SphereUser;
import com.parse.ParseUser;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import static com.facebook.fbu.photosphere.sphere.api.SphereAPI.SpheresLoadedCallback;
import static com.facebook.fbu.photosphere.sphere.api.SphereAPI.UserLoadedCallback;
import static com.facebook.fbu.photosphere.sphere.api.SphereAPI.fetchUser;
import static com.facebook.fbu.photosphere.sphere.api.SphereAPI.fetchUserSpheres;

/**
 * ProfileFragment that creates view of a user with their associated posts
 */
public class ProfileFragment extends Fragment implements SpheresLoadedCallback, UserLoadedCallback {

    private static final String TAG = ProfileFragment.class.getSimpleName();
    private static final String SPHERE_USER = "SPHERE_USER";

    private ListView mListView;
    private ParseUser mCurrUser;
    private View mHeaderView;
    private SphereUser mSphereUser;

    /**
     * creates a new ProfileFragment creating a bundle for the user who's data you wish to present
     *
     * @param user - user who's data you wish to present
     * @return new ProfileFragment with bundle created
     */
    public static ProfileFragment newInstance(SphereUser user) {
        ProfileFragment profileFragment = new ProfileFragment();
        Bundle profileData = new Bundle();
        profileData.putParcelable(SPHERE_USER, user);
        profileFragment.setArguments(profileData);
        return profileFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.profile_fragment, container, false);
        mHeaderView = inflater.inflate(R.layout.profile_header, null, false);
        mListView = (ListView) view.findViewById(R.id.list_view);
        mListView.addHeaderView(mHeaderView);
        ImageButton settingsButton = (ImageButton) view.findViewById(R.id.settings_button);
        if (getArguments() != null) {
            // if user is not current user, allow profile to be closed, ie popped off back stack
            settingsButton.setImageBitmap(BitmapFactory.decodeResource(getActivity().getResources(),
                    R.mipmap.cancel));
            settingsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getActivity().getSupportFragmentManager().popBackStack();
                }
            });
            // user is the user associated with id stored in bundle
            mSphereUser = getArguments().getParcelable(SPHERE_USER);
            fetchUser(mSphereUser.getUserId(), this);
        } else {
            // curent user is user
            settingsButton.setVisibility(View.VISIBLE);
            settingsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent startSettings = new Intent(getActivity(), SettingsActivity.class);
                    startActivity(startSettings);
                }
            });
            mCurrUser = ParseUser.getCurrentUser();
            mSphereUser = SphereUser.from(mCurrUser);
            fetchUserSpheres(mCurrUser, ProfileFragment.this);
        }
        bindProfileHeader(mSphereUser, mHeaderView);
        return view;
    }

    @Override
    public void onSpheresLoaded(List<Sphere> photoSpheres) {
        mListView.setDivider(null);
        mListView.setDividerHeight(0);
        mListView.setAdapter(new ProfileListViewAdapter(photoSpheres, getActivity()));
    }

    @Override
    public void onSphereLoadFailed() {
        //TODO : handle load failure
    }

    // binds the current user's data to the profile header
    private void bindProfileHeader(SphereUser sphereUser, View view) {
        ImageView profileImageView = (ImageView) view.findViewById(R.id.user_icon);
        TextView tagLineTextView = (TextView) view.findViewById(R.id.tag_line);
        TextView fullNameTextView = (TextView) view.findViewById(R.id.name);
        FontUtils.applyOpenSans(getActivity(), tagLineTextView);
        FontUtils.applyOpenSans(getActivity(), fullNameTextView);
        if (sphereUser.hasProfilePhoto()) {
            Picasso.with(getActivity())
                    .load(sphereUser.getUserPhotoUrl())
                    .into(profileImageView);
        }
        String name = sphereUser.getFullName();
        String tagLine = sphereUser.getTagLine();
        fullNameTextView.setText(name);
        // tag line can be null, check to see that it's not
        if (tagLine != null) {
            tagLineTextView.setText(tagLine);
        }
    }

    @Override
    public void onUserLoaded(ParseUser currUser) {
        mCurrUser = currUser;
        fetchUserSpheres(mCurrUser, ProfileFragment.this);
    }

    @Override
    public void onUserLoadFailed() {
        Log.e(TAG, "profile not found");
        FragmentManager fragmentManager = getActivity()
                .getSupportFragmentManager();
        fragmentManager.popBackStack();
    }

    /**
     * Adapts Spheres to profile list view
     */
    private class ProfileListViewAdapter extends BaseAdapter {

        private static final int THUMBNAILS_PER_ROW = 3;

        private List<Sphere> mSpheres;
        private LayoutInflater mInflater;

        public ProfileListViewAdapter(List<Sphere> spheres, Context context) {
            mSpheres = spheres;
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return (mSpheres.size() + THUMBNAILS_PER_ROW - 1) / THUMBNAILS_PER_ROW;
        }

        @Override
        public List<Sphere> getItem(int position) {
            // returns the list of items that composes the row at input position
            List<Sphere> sublist = new ArrayList<>();
            int offset = position * THUMBNAILS_PER_ROW;
            for (int i = 0; i < THUMBNAILS_PER_ROW && (offset + i < mSpheres.size()); i++) {
                sublist.add(mSpheres.get(offset + i));
            }
            return sublist;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        //TODO: create custom view instead
        private class ViewHolder {
            ImageView image1;
            ImageView image2;
            ImageView image3;
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            ViewHolder holder;
            if (view == null) {
                view = mInflater.inflate(R.layout.photo_row, parent, false);
                holder = new ViewHolder();
                holder.image1 = (ImageView) view.findViewById(R.id.image1);
                holder.image2 = (ImageView) view.findViewById(R.id.image2);
                holder.image3 = (ImageView) view.findViewById(R.id.image3);
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }

            ImageView[] thumbnailViews = {holder.image1, holder.image2, holder.image3};
            List<Sphere> toBind = getItem(position);
            for (int i = 0; i < thumbnailViews.length; i++) {
                if (i < toBind.size()) {
                    thumbnailViews[i].setVisibility(View.VISIBLE);
                    final Sphere sphere = toBind.get(i);
                    Picasso.with(getActivity())
                            .load(sphere.getThumbnail().getUrl())
                            .into(thumbnailViews[i]);
                    // on click, open up associated detail view
                    thumbnailViews[i].setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            DetailViewFragment fragment = DetailViewFragment.newInstance(sphere);
                            FragmentTransaction fragmentTransaction = getActivity()
                                    .getSupportFragmentManager()
                                    .beginTransaction();
                            fragmentTransaction.add(R.id.realtabcontent, fragment);
                            fragmentTransaction.addToBackStack(null).commit();
                        }
                    });
                } else {
                    thumbnailViews[i].setVisibility(View.INVISIBLE);
                }
            }
            return view;
        }
    }
}
