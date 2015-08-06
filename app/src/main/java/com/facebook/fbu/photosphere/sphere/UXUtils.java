// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.fbu.photosphere.sphere;

import android.app.Activity;
import android.graphics.Bitmap;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import java.io.ByteArrayOutputStream;

public class UXUtils {

    private static final int BITMAP_UPLOAD_QUALITY = 100;

    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager = (InputMethodManager) activity
                .getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
    }

    /**
     * Converts bitmaps to byte[] bc only byte[] can be uploaded to parse
     *
     * @param bitmap - bitmaps to convert into byte array
     * @return byte array version of bitmap
     */
    public static byte[] getByteArray(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, BITMAP_UPLOAD_QUALITY, b);
        return b.toByteArray();
    }

    /**
     * helper used to get text from EditText
     *
     * @param editText - consumes an EditText
     * @return- the stripped string of its contents
     */
    public static String getText(EditText editText) {
        return editText.getText().toString().trim();
    }

    /**
     * checks to ensure that the sign up is valid
     *
     * @param username - username to validate
     * @param password - password to validate
     * @param passwordAgain - password to validate
     * @param name - name to validate
     * @param email - email to validate
     * @return if valid returns true, else false
     */
    public static boolean isValid(String username,
                            String password,
                            String passwordAgain,
                            String name,
                            String email) {
        boolean isValid = true;
        if (username.length() == 0
                || password.length() == 0
                || !password.equals(passwordAgain)
                || name.length() == 0
                || email.length() == 0) {
            isValid = false;
        }
        return isValid;
    }
}
