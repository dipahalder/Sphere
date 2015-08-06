// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.fbu.photosphere.sphere.api;


import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.util.Log;

import com.facebook.fbu.photosphere.sphere.DispatchActivity;
import com.facebook.fbu.photosphere.sphere.Sphere;
import com.parse.FindCallback;
import com.parse.LogInCallback;
import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.parse.SignUpCallback;

import java.util.Date;
import java.util.List;

import static com.facebook.fbu.photosphere.sphere.Sphere.*;

/**
 * SphereAPI is used to communicate with the backend
 * Handles the following:
 * 1) adding a sphere to database
 * 2) loading spheres from the database and specific queries:
 * - fetchAllSpheres - fetching all spheres
 * - fetchSphere - fetch a single sphere by id
 * - fetchUserSpheres - fetch a user's spheres
 * 3) user login
 * 4) adding a user to parse
 * 5) logging out current parse user
 */
public class SphereAPI {

    private static final String TAG = SphereAPI.class.getSimpleName();
    public static final String FULL_NAME = "full_name";
    public static final String TAG_LINE = "tag_line";
    public static final String USER_PHOTO = "user_photo";
    public static final String DOB = "date_of_birth";
    public static final String USER_ID = "objectId";

    /**
     * Adds a new sphere to database
     *
     * @param imageByteArray      - the image file
     * @param thumbnailByteArray  - the thumbnail
     * @param spherePhotoType                - is the sphere a photosphere or not
     * @param caption             - the caption of the photo
     * @param location            - the location the photo was taken in
     * @param sphereAddedCallback - callback to trigger on upload of sphere
     */
    public static void addSphere(byte[] imageByteArray,
                                 byte[] thumbnailByteArray,
                                 SpherePhotoType spherePhotoType,
                                 String caption,
                                 Location location,
                                 final SphereAddedCallback sphereAddedCallback) {

        Sphere sphere = createForUpload(ParseUser.getCurrentUser());
        sphere.setImage(new ParseFile(imageByteArray));
        sphere.setThumbnail(new ParseFile(thumbnailByteArray));
        sphere.setPhotoType(spherePhotoType);
        sphere.setCaption(caption);
        if (location != null) {
            sphere.setCoordinates(new ParseGeoPoint(
                    location.getLatitude(),
                    location.getLongitude()));
        }
        ParseACL acl = new ParseACL();
        acl.setPublicReadAccess(true);
        sphere.setACL(acl);
        sphere.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e != null) {
                    Log.e(TAG, "Error loading spheres: " + e);
                    sphereAddedCallback.onSphereAddFailed();
                    return;
                }
                sphereAddedCallback.onSphereAdded();
            }
        });
    }

    /**
     * Query all spheres in the database
     *
     * @param spheresLoadedCallback - callback to trigger on successful query
     */
    public static void fetchAllSpheres(final SpheresLoadedCallback spheresLoadedCallback) {
        loadSpheres(getQuery(), spheresLoadedCallback);
    }

    /**
     * Query single sphere's data
     *
     * @param objectId              - id of sphere to query
     * @param spheresLoadedCallback - callback to trigger on completed query
     */
    public static void fetchSphere(String objectId,
                                   final SpheresLoadedCallback spheresLoadedCallback) {
        ParseQuery<Sphere> query = getQuery();
        query.whereEqualTo(PHOTO_ID, objectId);
        loadSpheres(query, spheresLoadedCallback);
    }

    /**
     * queries all spheres attributed to a specific user
     *
     * @param user                  - user who's spheres are being queried
     * @param spheresLoadedCallback - callback to trigger on completed query
     */
    public static void fetchUserSpheres(ParseUser user,
                                        final SpheresLoadedCallback spheresLoadedCallback) {
        ParseQuery<Sphere> query = getQuery();
        query.whereEqualTo(USER, user);
        query.orderByDescending("updatedAt");
        loadSpheres(query, spheresLoadedCallback);
    }

    /**
     * Queries spheres from parse and triggers callback to inform of success or failure
     *
     * @param query                 - SphereQuery to perform
     * @param spheresLoadedCallback - callback to trigger on completed query
     */
    public static void loadSpheres(ParseQuery<Sphere> query,
                                   final SpheresLoadedCallback spheresLoadedCallback) {
        query.findInBackground(new FindCallback<Sphere>() {
            @Override
            public void done(List<Sphere> list, ParseException e) {
                if (e != null) {
                    Log.e(TAG, "error:" + e);
                    spheresLoadedCallback.onSphereLoadFailed();
                    return;
                }
                spheresLoadedCallback.onSpheresLoaded(list);
            }
        });
    }

    /**
     * Adds a user to parse
     *
     * @param username           - the username associated with that user
     * @param password           - the password associated with that user
     * @param email              - the email associated with that user
     * @param fullName           - the full name associated with that user
     * @param tagLine            - the tag line associated with that user
     * @param userPhotoByteArray - the byte[] of the photo associated with that user
     * @param dateOfBirth        - the date of birth associated with that user
     * @param userAddedCallback  - callback to trigger on completed user upload
     */
    public static void addUser(String username,
                               String password,
                               String email,
                               String fullName,
                               String tagLine,
                               byte[] userPhotoByteArray,
                               final Date dateOfBirth,
                               final UserAddedCallback userAddedCallback) {
        // Set up a new Sphere user
        final ParseUser newUser = new ParseUser();
        newUser.setUsername(username);
        newUser.setPassword(password);
        newUser.setEmail(email);
        newUser.put(FULL_NAME, fullName);
        newUser.put(TAG_LINE, tagLine);
        if (dateOfBirth != null) {
            newUser.put(DOB, dateOfBirth);
        }
        if (userPhotoByteArray == null) {
            newUser.signUpInBackground(new SignUpCallback() {
                @Override
                public void done(ParseException e) {
                    if (e != null) {
                        userAddedCallback.onUserAddFailed(e);
                        return;
                    }
                    userAddedCallback.onUserAdded();
                }
            });
            return;
        }
        final ParseFile userPhotoFile = new ParseFile(userPhotoByteArray);
        userPhotoFile.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                newUser.put(USER_PHOTO, userPhotoFile);
                newUser.signUpInBackground(new SignUpCallback() {
                    @Override
                    public void done(ParseException e) {
                        if (e != null) {
                            userAddedCallback.onUserAddFailed(e);
                            return;
                        }
                        userAddedCallback.onUserAdded();
                    }
                });
            }
        });
    }

    /**
     * Logs a user in
     *
     * @param username            - username to login
     * @param password            - password associated with username
     * @param sphereLoginCallback - callback to trigger on completed login
     */
    public static void loginUser(String username,
                                 String password,
                                 final SphereLoginCallback sphereLoginCallback) {
        // calls the Parse login method
        ParseUser.logInInBackground(username, password, new LogInCallback() {
            @Override
            public void done(ParseUser user, ParseException e) {
                if (e != null) {
                    sphereLoginCallback.onLoginFailed(e);
                } else {
                    sphereLoginCallback.onLoginSuccess();
                }
            }
        });
    }

    /**
     * logs current user out of application and relaunches DispatchActivity
     *
     * @param context - context in which the method is called
     */
    public static void logout(Context context) {
        ParseUser.logOut();
        Intent intent = new Intent(context, DispatchActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    /**
     * fetches a user by id and then responds based on success
     *
     * @param userId             - userId being queried for
     * @param userLoadedCallback - callback to trigger on completed query for user
     */
    public static void fetchUser(String userId, final UserLoadedCallback userLoadedCallback) {
        ParseQuery<ParseUser> query = ParseUser.getQuery();
        query.whereEqualTo(USER_ID, userId);
        query.findInBackground(new FindCallback<ParseUser>() {
            public void done(List<ParseUser> users, ParseException e) {
                if (e == null) {
                    Log.i(TAG, "Num users" + users.size());
                    assert users.size() == 1;
                    userLoadedCallback.onUserLoaded(users.get(0));
                } else {
                    Log.e(TAG, "Error incorrect user list size found: " + e.getMessage());
                    userLoadedCallback.onUserLoadFailed();
                }
            }
        });
    }

    /**
     * interface used to handle adding Spheres to database
     */
    public interface SphereAddedCallback {

        // callback to trigger a response to successful save of Sphere to database
        void onSphereAdded();

        // callback to create a response to an unsuccessful save of Sphere to database
        void onSphereAddFailed();
    }

    /**
     * interface used to handle logging in
     */
    public interface SphereLoginCallback {

        // callback to trigger a response to successful login
        void onLoginSuccess();

        /**
         * callback to trigger a response to unsuccessful login
         *
         * @param e - error received upon attempted login
         */
        void onLoginFailed(ParseException e);
    }

    /**
     * interface used to handle parse queries on Spheres
     */
    public interface SpheresLoadedCallback {
        /**
         * used to create a response to successful parse query
         *
         * @param photoSpheres- the list of Spheres returned from a parse query
         */
        void onSpheresLoaded(List<Sphere> photoSpheres);

        // creates response to unsuccessful parse query
        void onSphereLoadFailed();
    }

    /**
     * interface used to handle adding a user to parse
     */
    public interface UserAddedCallback {

        // used to create a response to successful save of user to Parse database
        void onUserAdded();

        /**
         * used to generate a response upon unsuccessful attempt to add user to Parse database
         *
         * @param e - error received upon unsuccessful attempt
         */
        void onUserAddFailed(ParseException e);
    }

    /**
     * interface used to handle loading a user from parse
     */
    public interface UserLoadedCallback {

        // used to create a response to successful query of user by id
        void onUserLoaded(ParseUser currUser);

        // used to generate a response upon unsuccessful attempt to find user by id
        void onUserLoadFailed();
    }

}
