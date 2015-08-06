// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.fbu.photosphere.sphere;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.ParseException;

import static com.facebook.fbu.photosphere.sphere.api.SphereAPI.SphereLoginCallback;
import static com.facebook.fbu.photosphere.sphere.api.SphereAPI.loginUser;

/**
 * Handles user login with validation
 * Links to Reset password or Create a new account are wired into the login interface
 */
public class LoginActivity extends Activity implements SphereLoginCallback {
    private EditText mUsernameEditText;
    private EditText mPasswordEditText;
    private TextView mPasswordReset;
    private TextView mSignUp;
    private Button mLoginButton;
    private ProgressDialog mDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);
        mUsernameEditText = (EditText) findViewById(R.id.username);
        mPasswordEditText = (EditText) findViewById(R.id.password);
        mPasswordEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == R.id.edittext_action_login ||
                        actionId == EditorInfo.IME_ACTION_UNSPECIFIED) {
                    login();
                    return true;
                }
                return false;
            }
        });
        mLoginButton = (Button) findViewById(R.id.login_button);
        mLoginButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                login();
            }
        });

        mPasswordReset = (TextView) findViewById(R.id.forgot_password);
        mPasswordReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, ResetPasswordActivity.class));
            }
        });

        mSignUp = (TextView) findViewById(R.id.signup_form);
        mSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
            }
        });
    }

    /**
     * checks to ensure that the login is valid
     *
     * @param username - username to validate against
     * @param password - the password to validate with
     * @return if login is valid true, else false
     */
    private boolean isValid(String username, String password) {
        boolean isValid = true;
        StringBuilder validationErrorMessage = new StringBuilder(getString(R.string.error_intro));
        if (username.length() == 0) {
            isValid = false;
            validationErrorMessage.append(getString(R.string.error_blank_username));
        }
        if (password.length() == 0) {
            if (!isValid) {
                validationErrorMessage.append(getString(R.string.error_join));
            }
            isValid = false;
            validationErrorMessage.append(getString(R.string.error_blank_password));
        }
        validationErrorMessage.append(getString(R.string.error_end));

        // If there is a validation error, display the error
        if (!isValid) {
            Toast.makeText(LoginActivity.this, validationErrorMessage.toString(), Toast.LENGTH_LONG)
                    .show();
        }
        return isValid;
    }

    /**
     * if login is valid, logs user in using parse and starts dispatch activity
     */
    private void login() {
        String username = mUsernameEditText.getText().toString().trim();
        String password = mPasswordEditText.getText().toString().trim();

        // stop if login is not valid
        if (!isValid(username, password)) {
            return;
        }

        // shows logging in progress
        mDialog = new ProgressDialog(LoginActivity.this);
        mDialog.setMessage(getString(R.string.progress_login));
        mDialog.show();
        loginUser(username, password, this);
    }

    @Override
    public void onLoginSuccess() {
        mDialog.dismiss();
        // Start an intent for the dispatch activity
        Intent intent = new Intent(LoginActivity.this, DispatchActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public void onLoginFailed(ParseException e) {
        mDialog.dismiss();
        // Show the error message
        Toast.makeText(LoginActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
    }
}
