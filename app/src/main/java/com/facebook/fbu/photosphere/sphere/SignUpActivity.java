// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.fbu.photosphere.sphere;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.ParseException;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import static android.app.DatePickerDialog.OnDateSetListener;
import static com.facebook.fbu.photosphere.sphere.api.SphereAPI.UserAddedCallback;
import static com.facebook.fbu.photosphere.sphere.api.SphereAPI.addUser;

/**
 * Activity that handles sign up
 */
public class SignUpActivity extends FragmentActivity implements UserAddedCallback {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final String TAG = SignUpActivity.class.getSimpleName();

    // UI components
    private EditText mUsernameEditText;
    private EditText mPasswordEditText;
    private EditText mPasswordAgainEditText;
    private EditText mFullNameEditText;
    private EditText mTagLineEditText;
    private EditText mEmailAddress;
    private EditText mDOBButton;
    private Button mSignUpButton;
    private TextView mLoginLink;
    private ImageView mUserImage;
    private ProgressDialog mDialog;

    // info to pass off with user
    private Bitmap mBitmap;
    private Date mDOBDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.signup_activity);
        //Hide soft keyboard on create
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        mUserImage = (ImageView) findViewById(R.id.user_prof);
        mUserImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent startSelectActivity = new Intent(
                        SignUpActivity.this,
                        GallerySelectionActivity.class);
                startActivityForResult(startSelectActivity, PICK_IMAGE_REQUEST);
            }
        });

        mPasswordEditText = (EditText) findViewById(R.id.password_edit_text);
        mPasswordAgainEditText = (EditText) findViewById(R.id.password_again_edit_text);
        mPasswordAgainEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == R.id.edittext_action_signup ||
                        actionId == EditorInfo.IME_ACTION_UNSPECIFIED) {
                    signup();
                    return true;
                }
                return false;
            }
        });

        mUsernameEditText = (EditText) findViewById(R.id.username_edit_text);
        mFullNameEditText = (EditText) findViewById(R.id.fullname_edit_text);
        mTagLineEditText = (EditText) findViewById(R.id.tagline_edit_text);
        mEmailAddress = (EditText) findViewById(R.id.email_edit_text);

        mSignUpButton = (Button) findViewById(R.id.sign_up_button);
        mSignUpButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                signup();
            }
        });

        mLoginLink = (TextView) findViewById(R.id.return_login);
        mLoginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
            }
        });

        final Calendar newCalendar = Calendar.getInstance();
        mDOBButton = (EditText) findViewById(R.id.date_picker_button);
        mDOBButton.setMovementMethod(null);
        mDOBButton.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus == false) {
                    return;
                }
                DatePickerDialog datePicker = new DatePickerDialog(SignUpActivity.this,
                        new OnDateSetListener() {
                            private Date mDate;

                            public void onDateSet(DatePicker view, int year, int month, int day) {
                                mDate = new GregorianCalendar(year, month, day).getTime();
                                SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
                                String formattedDate = dateFormat.format(mDate);
                                mDOBButton.setText(formattedDate);
                                mDOBButton.setTextColor(getResources().getColor(R.color.black));
                                mDOBDate = mDate;
                            }
                        },
                        newCalendar.get(Calendar.YEAR),
                        newCalendar.get(Calendar.MONTH),
                        newCalendar.get(Calendar.DAY_OF_MONTH));
                datePicker.show();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_IMAGE_REQUEST) {
            if (resultCode == RESULT_CANCELED) {
                return;
            }
            Uri selectedImage = data.getParcelableExtra(GallerySelectionActivity.IMAGE_URI);

            try {

                InputStream stream = getContentResolver().openInputStream(
                        selectedImage);
                mBitmap = BitmapFactory.decodeStream(stream);
                stream.close();
                mUserImage.setImageBitmap(mBitmap);
            } catch (IOException e) {
                Log.e(TAG, "image not found" + e);
            }
        }
    }

    /**
     * if sign up is valid, creates user, logs user in using parse and starts dispatch activity
     */
    private void signup() {
        String userName = UXUtils.getText(mUsernameEditText);
        String password = UXUtils.getText(mPasswordEditText);
        String passwordAgain = UXUtils.getText(mPasswordAgainEditText);
        String name = UXUtils.getText(mFullNameEditText);
        String tagLine = UXUtils.getText(mTagLineEditText);
        String email = UXUtils.getText(mEmailAddress);
        byte[] userPhotoByteArray = UXUtils.getByteArray(mBitmap);

        // stop signup if not valid
        if (!UXUtils.isValid(userName, password, passwordAgain, name, email)) {
            return;
        }

        // Set up a progress dialog
        mDialog = new ProgressDialog(SignUpActivity.this);
        mDialog.setMessage(getString(R.string.progress_signup));
        mDialog.show();
        addUser(
                userName,
                password,
                email,
                name,
                tagLine,
                userPhotoByteArray,
                mDOBDate,
                this);
    }

    @Override
    public void onUserAdded() {
        mDialog.dismiss();
        // Start an intent for the dispatch activity
        Intent intent = new Intent(SignUpActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK |
                Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public void onUserAddFailed(ParseException error) {
        mDialog.dismiss();
        // Show the error message
        Toast.makeText(SignUpActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
    }
}
