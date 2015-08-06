// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.fbu.photosphere.sphere.api;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.parse.ParseClassName;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseUser;

/**
 * Adapter class used for interacting with attributes of user data stored in parse
 */
@ParseClassName("SphereUser")
public class SphereUser extends ParseUser implements Parcelable {

    private static final String TAG = SphereUser.class.getSimpleName();
    private static final String FULL_NAME = "full_name";
    private static final String TAG_LINE = "tag_line";
    private static final String USER_PHOTO = "user_photo";
    private static final String USER_ID = "user_id";
    private static final String USER_PHOTO_FILE = "user_photo_file";

    private ParseUser mSphereUser;
    private String mFullName;
    private String mUsername;
    private String mEmail;
    private String mTagLine;
    private ParseFile mUserPhotoFile;
    private String mUserId;

    public static SphereUser from(ParseUser user) {
        SphereUser sphereUser = new SphereUser(user);
        return sphereUser;
    }

    private SphereUser(ParseUser sphereUser) {
        try {
            mSphereUser = sphereUser.fetch();
            mUsername = mSphereUser.getUsername();
            mEmail = mSphereUser.getEmail();
            mUserId = mSphereUser.getObjectId();
            mFullName = mSphereUser.getString(FULL_NAME);
            mTagLine = mSphereUser.getString(TAG_LINE);
            mUserPhotoFile = mSphereUser.getParseFile(USER_PHOTO);
        } catch (ParseException e) {
            Log.e(TAG, "error creating sphere user");
        }
    }

    public SphereUser() {
        // default for registering with parse
    }

    /**
     * @return String representing the full name of mSphereUser
     */
    public String getFullName() {
        return mFullName;
    }

    /**
     * @return String representing the mSphereUser's username
     */
    public String getUsername() {
        return mUsername;
    }

    /**
     * @return String representing the mSphereUser's email address
     */
    public String getEmail() {
        return mEmail;
    }

    /**
     * @return string representing mSphereUser's tag line
     */
    public String getTagLine() {
        return mTagLine;
    }

    public byte[] getUserPhotoByteArray() {
        if (mUserPhotoFile == null) {
            return null;
        }
        byte[] iconByteData = new byte[0];
        try {
            iconByteData = mUserPhotoFile.getData();
        } catch (ParseException e) {
            Log.e(TAG, "error getting byte[] " + e.getMessage());
        }
        return iconByteData;
    }

    /**
     * @return string url of user's photo
     */
    public String getUserPhotoUrl() {
        if (mUserPhotoFile == null) {
            return null;
        }
        return mUserPhotoFile.getUrl();
    }

    /**
     * checks to see if a user has a profile photo
     *
     * @return true if yes, false if no
     */
    public boolean hasProfilePhoto() {
        return mUserPhotoFile != null;
    }

    /**
     * gets a user's id
     *
     * @return string representing user's id
     */
    public String getUserId() {
        return mUserId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        Bundle bundle = new Bundle();

        bundle.putString(FULL_NAME, getFullName());
        bundle.putString(TAG_LINE, getTagLine());
        bundle.putString(USER_ID, getUserId());
        bundle.putByteArray(USER_PHOTO_FILE, getUserPhotoByteArray());

        dest.writeBundle(bundle);
    }

    /**
     * Creator for creating a SphereUser from a parcelable
     */
    public static final Parcelable.Creator<SphereUser> CREATOR = new Creator<SphereUser>() {

        @Override
        public SphereUser createFromParcel(Parcel source) {
            // read the bundle containing key value pairs from the parcel
            Bundle bundle = source.readBundle();

            SphereUser user = new SphereUser();
            user.mFullName = bundle.getString(FULL_NAME);
            user.mTagLine = bundle.getString(TAG_LINE);
            user.mUserId = bundle.getString(USER_ID);
            user.mUserPhotoFile = new ParseFile(bundle.getByteArray(USER_PHOTO_FILE));

            return user;
        }

        @Override
        public SphereUser[] newArray(int size) {
            return new SphereUser[size];
        }
    };
}
