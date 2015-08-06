// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.fbu.photosphere.sphere;

import android.app.Application;

import com.facebook.fbu.photosphere.sphere.api.SphereUser;
import com.parse.Parse;
import com.parse.ParseCrashReporting;
import com.parse.ParseObject;
import com.parse.ParseUser;


public class SphereApplication extends Application {
    public static final String APPLICATION_ID = "VD5972dg8fY8sBS915VAFOGoT6cGix7AULyj90li";
    public static final String CLIENT_KEY = "5TuB7ZdlvItqXO1Lk37WuDZyXI2V359qJMp0RF07";

    @Override
    public void onCreate() {
        super.onCreate();
        ParseCrashReporting.enable(this);
        Parse.enableLocalDatastore(this);
        Parse.initialize(this, APPLICATION_ID, CLIENT_KEY);
        ParseObject.registerSubclass(Sphere.class);
        ParseUser.registerSubclass(SphereUser.class);
    }

}
