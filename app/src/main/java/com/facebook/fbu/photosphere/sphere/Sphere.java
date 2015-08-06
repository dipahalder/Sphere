// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.fbu.photosphere.sphere;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.facebook.fbu.photosphere.sphere.api.SphereUser;
import com.google.android.gms.maps.model.LatLng;
import com.parse.ParseClassName;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

@ParseClassName("Sphere")
public class Sphere extends ParseObject implements Parcelable {
    public static final String MODEL_NAME = Sphere.class.getSimpleName();

    // column tags for storage in parse
    public static final String PHOTO_TYPE = "photo_type";
    public static final String THUMBNAIL = "thumbnail";
    public static final String IMAGE = "image";
    public static final String CAPTION = "caption";
    public static final String COORDINATES = "coordinates";
    public static final String USER = "user_id";
    public static final String PHOTO_ID = "objectId";
    private static final String SPHERE_USER = "sphere_user";

    public enum SpherePhotoType {
        PANORAMA,
        PHOTOSPHERE
    }

    /**
     * creates a Sphere for upload
     *
     * @param currUser - the user uploading the photo
     * @return a new Sphere with the the user set
     */
    public static Sphere createForUpload(ParseUser currUser) {
        Sphere newSphere = new Sphere();
        newSphere.setUser(currUser);
        return newSphere;
    }

    private void setUser(ParseUser user) {
        put(USER, user);
    }

    public ParseUser getUser() {
        return getParseUser(USER);
    }

    /**
     * returns the type of the upload: either Panorama or PhotoSphere
     *
     * @return value stored in parse converted to enum type
     */
    public SpherePhotoType getPhotoType() {
        return SpherePhotoType.valueOf(getString(PHOTO_TYPE));
    }

    public void setPhotoType(SpherePhotoType spherePhotoType) {
        put(PHOTO_TYPE, spherePhotoType.name());
    }

    public ParseFile getThumbnail() {
        return getParseFile(THUMBNAIL);
    }

    public void setThumbnail(ParseFile thumbnail) {
        put(THUMBNAIL, thumbnail);
    }

    public ParseFile getImage() {
        return getParseFile(IMAGE);
    }

    public void setImage(ParseFile image) {
        put(IMAGE, image);
    }

    public ParseGeoPoint getCoordinates() {
        return getParseGeoPoint(COORDINATES);
    }

    public void setCoordinates(ParseGeoPoint coordinates) {
        put(COORDINATES, coordinates);
    }

    public String getCaption() {
        return getString(CAPTION);
    }

    public void setCaption(String caption) {
        put(CAPTION, caption);
    }

    public String getPhotoId() {
        return getObjectId();
    }

    public static ParseQuery<Sphere> getQuery() {
        return ParseQuery.getQuery(Sphere.class);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        Bundle bundle = new Bundle();

        bundle.putString(PHOTO_ID, getObjectId());
        bundle.putString(PHOTO_TYPE, getPhotoType().toString());
        try {
            bundle.putByteArray(THUMBNAIL, getThumbnail().getData());
            bundle.putByteArray(IMAGE, getImage().getData());
        } catch (ParseException e) {
            Log.e(MODEL_NAME, e.getMessage());
        }
        bundle.putString(CAPTION, getCaption());
        bundle.putParcelable(COORDINATES, new LatLng(
                getCoordinates().getLatitude(),
                getCoordinates().getLongitude()));
        bundle.putParcelable(SPHERE_USER, SphereUser.from(getUser()));

        dest.writeBundle(bundle);
    }

    /**
     * Creator for creating a Sphere from parcelable
     */
    public static final Parcelable.Creator<Sphere> CREATOR = new Creator<Sphere>() {

        @Override
        public Sphere createFromParcel(Parcel source) {
            Bundle bundle = source.readBundle();

            Sphere sphere = ParseObject.createWithoutData(Sphere.class, bundle.getString(PHOTO_ID));
            sphere.setPhotoType(SpherePhotoType.valueOf(bundle.getString(PHOTO_TYPE)));
            sphere.setThumbnail(new ParseFile(bundle.getByteArray(THUMBNAIL)));
            sphere.setImage(new ParseFile(bundle.getByteArray(IMAGE)));
            sphere.setCaption(bundle.getString(CAPTION));
            LatLng location = bundle.getParcelable(COORDINATES);
            sphere.setCoordinates(new ParseGeoPoint(location.latitude, location.longitude));
            sphere.setUser((SphereUser) bundle.getParcelable(SPHERE_USER));

            return sphere;
        }

        @Override
        public Sphere[] newArray(int size) {
            return new Sphere[size];
        }

    };
}
