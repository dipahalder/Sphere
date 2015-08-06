// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.fbu.photosphere.sphere;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import static android.provider.MediaStore.Images.Media;

/**
 * Activity to handle selecting an image from gallery, which happens in several parts of the app
 */
public class GallerySelectionActivity extends Activity {

    public static final String IMAGE_URI = "image_path";
    private static final String TAG = GallerySelectionActivity.class.getSimpleName();
    private static final int RESULT_LOAD_IMG = 2;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        openGallery();
    }

    /**
     * creates an intent to open up photo gallery
     */
    public void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, Media.EXTERNAL_CONTENT_URI);
        // Start the Intent
        startActivityForResult(galleryIntent, RESULT_LOAD_IMG);
    }

    /**
     * Handles image in upload selection
     * on complete return resulting data to calling activity
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Intent returnIntent = new Intent();
        try {
            if (requestCode != RESULT_LOAD_IMG
                    || resultCode != RESULT_OK
                    || data == null) {
                Toast.makeText(this, R.string.no_photo, Toast.LENGTH_SHORT).show();
                setResult(RESULT_CANCELED, returnIntent);
                finish();
                return;
            }
            Uri selectedImage = data.getData();
            returnIntent.putExtra(IMAGE_URI, selectedImage);
            setResult(RESULT_OK, returnIntent);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Something went wrong when selecting image from gallery" + e);
        }
    }

}
