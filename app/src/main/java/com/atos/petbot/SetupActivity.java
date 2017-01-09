package com.atos.petbot;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;

import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


//import com.android.volley.VolleyError;
//import com.android.volley.toolbox.ImageRequest;
import com.petbot.R;

/**
 * A login screen that offers login via email/password.
 */
public class SetupActivity extends Activity {

	/**
	 * Id to identity READ_CONTACTS permission request.
	 */
	private static final int REQUEST_READ_CONTACTS = 0;

	// UI references.
	private EditText mNetworkView;
	private EditText mPasswordView;
	private View mRegistrationFormView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_setup);
		// Set up the login form.
		mNetworkView = (EditText) findViewById(R.id.network_name);

		mPasswordView = (EditText) findViewById(R.id.password);
		mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
				if (id == R.id.register || id == EditorInfo.IME_NULL) {
					generateQR();
					return true;
				}
				return false;
			}
		});

		Button mQRButton = (Button) findViewById(R.id.generate_qr_button);
		mQRButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				generateQR();
			}
		});

		mRegistrationFormView = findViewById(R.id.network_form);
	}


	/**
	 * Attempts to sign in or register the account specified by the login form.
	 * If there are form errors (invalid email, missing fields, etc.), the
	 * errors are presented and no actual login attempt is made.
	 */
	private void generateQR() {

		// Reset errors.
		mNetworkView.setError(null);
		mPasswordView.setError(null);

		// Store values at the time of the login attempt.
		String network = mNetworkView.getText().toString();
		String network_key = mPasswordView.getText().toString();

		boolean cancel = false;
		View focusView = null;

		// Check for a valid password, if the user entered one.
		if (TextUtils.isEmpty(network_key)) {
			mPasswordView.setError(getString(R.string.error_field_required));
			focusView = mPasswordView;
			cancel = true;
		}

		// Check for a valid email address.
		if (TextUtils.isEmpty(network)) {
			mNetworkView.setError(getString(R.string.error_field_required));
			focusView = mNetworkView;
			cancel = true;
		}

		if (cancel) {
			// There was an error; don't attempt login and focus the first
			// form field with an error.
			focusView.requestFocus();
		} else {

			// open activity to display QR code
			String username = getIntent().getExtras().getString("username");
			String email = getIntent().getExtras().getString("email");
			String password = getIntent().getExtras().getString("password");
			String wait_time = "40";
			String qr_text =  "SETUP:" + username.length() + ":" + username + ":" + email.length() + ":" + email + ":"
					+ password.length() + ":" + password + ":" + network.length() + ":" + network + ":" + network_key.length() + ":" + network_key + ":"
					+ wait_time.length() + ":" + wait_time;
			Intent open_qr = new Intent(SetupActivity.this, QRViewer.class);
			open_qr.putExtra("qr_text", qr_text);
			SetupActivity.this.startActivity(open_qr);

		}
	}

}

