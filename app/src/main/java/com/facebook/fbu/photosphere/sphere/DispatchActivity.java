// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.fbu.photosphere.sphere;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.parse.ParseUser;

/**
 * Handles user login to ensure that user can stay logged in,
 * handles the user's 2 potential states
 * 1) logged in: launches (MainActivity)
 * 2) logged out: (SignUpOrLoginActivity) activity
 */
public class DispatchActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Check if there is current user info
        if (ParseUser.getCurrentUser() != null) {
            // Start an intent for the logged in activity
            startActivity(new Intent(this, MainActivity.class));
        } else {
            // Start and intent for the logged out activity
            startActivity(new Intent(this, LoginActivity.class));
        }
    }
}
