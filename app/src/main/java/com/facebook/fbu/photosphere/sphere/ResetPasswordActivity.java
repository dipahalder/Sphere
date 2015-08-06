// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.fbu.photosphere.sphere;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.RequestPasswordResetCallback;

/**
 * Activity used to handle password reset
 */
public class ResetPasswordActivity extends Activity {

    private Button mResetPasswordButton;
    private EditText mEmailAddressEditText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reset_password_activity);

        mEmailAddressEditText = (EditText) findViewById(R.id.email_edit_text);
        mResetPasswordButton = (Button) findViewById(R.id.button);
        mResetPasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String emailAddress = UXUtils.getText(mEmailAddressEditText);
                if(emailAddress.length() != 0) {
                    ParseUser.requestPasswordResetInBackground(
                            emailAddress,
                            new RequestPasswordResetCallback() {
                        public void done(ParseException e) {
                            if (e == null) {
                                // An email was successfully sent with reset instructions.
                                Toast.makeText(ResetPasswordActivity.this, "SENT",
                                        Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                // Something went wrong.
                                Toast.makeText(ResetPasswordActivity.this, "NOT SENT",
                                        Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        }
                    });
                } else {
                    Toast.makeText(ResetPasswordActivity.this, R.string.error_blank_email,
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
