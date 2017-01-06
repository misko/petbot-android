package com.petbot;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.app.Activity;
import android.app.LoaderManager.LoaderCallbacks;

import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;

import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.error.VolleyError;
import com.android.volley.request.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * A login screen that offers login via email/password.
 */
public class RegistrationActivity extends Activity implements LoaderCallbacks<Cursor> {

	/**
	 * Id to identity READ_CONTACTS permission request.
	 */
	private static final int REQUEST_READ_CONTACTS = 0;

	// UI references.
	private EditText mUsernameView;
	private AutoCompleteTextView mEmailView;
	private EditText mPasswordView;
	private EditText mConfirmPasswordView;
	private View mProgressView;
	private View mRegistrationFormView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_registration);
		// Set up the login form.

		mUsernameView = (EditText) findViewById(R.id.username);

		mEmailView = (AutoCompleteTextView) findViewById(R.id.email);
		populateAutoComplete();

		mPasswordView = (EditText) findViewById(R.id.password);

		mConfirmPasswordView = (EditText) findViewById(R.id.confirm_password);
		mConfirmPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
				if (id == R.id.register || id == EditorInfo.IME_NULL) {
					attemptRegister();
					return true;
				}
				return false;
			}
		});

		Button mNextButton = (Button) findViewById(R.id.next_button);
		mNextButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				attemptRegister();
			}
		});

		mRegistrationFormView = findViewById(R.id.registration_form);
		mProgressView = findViewById(R.id.registration_progress);
	}

	private void populateAutoComplete() {
		if (!mayRequestContacts()) {
			return;
		}

		getLoaderManager().initLoader(0, null, this);
	}

	private boolean mayRequestContacts() {
		/*if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
			return true;
		}
		if (checkSelfPermission(READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
			return true;
		}
		if (shouldShowRequestPermissionRationale(READ_CONTACTS)) {
			Snackbar.make(mEmailView, R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE)
					.setAction(android.R.string.ok, new View.OnClickListener() {
						@Override
						@TargetApi(Build.VERSION_CODES.M)
						public void onClick(View v) {
							requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
						}
					});
		} else {
			requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
		}*/
		return false;
	}

	/**
	 * Callback received when a permissions request has been completed.
	 */
	/*@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
										   @NonNull int[] grantResults) {
		if (requestCode == REQUEST_READ_CONTACTS) {
			if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				populateAutoComplete();
			}
		}
	}*.


	/**
	 * Attempts to sign in or register the account specified by the login form.
	 * If there are form errors (invalid email, missing fields, etc.), the
	 * errors are presented and no actual login attempt is made.
	 */
	private void attemptRegister() {

		// Reset errors.
		mUsernameView.setError(null);
		mEmailView.setError(null);
		mPasswordView.setError(null);

		// Store values at the time of the login attempt.
		final String username = mUsernameView.getText().toString();
		final String email = mEmailView.getText().toString();
		final String password = mPasswordView.getText().toString();

		boolean cancel = false;
		View focusView = null;

		// field validation
		if (TextUtils.isEmpty(password) || !isPasswordValid(password)) {
			mPasswordView.setError(getString(R.string.error_invalid_password));
			focusView = mPasswordView;
			cancel = true;
		} else if (TextUtils.isEmpty(username)) {
			mUsernameView.setError(getString(R.string.error_field_required));
			focusView = mUsernameView;
			cancel = true;
		} else if (TextUtils.isEmpty(email)) {
			mEmailView.setError(getString(R.string.error_field_required));
			focusView = mEmailView;
			cancel = true;
		} else if (!isEmailValid(email)) {
			mEmailView.setError(getString(R.string.error_invalid_email));
			focusView = mEmailView;
			cancel = true;
		} else if (!password.equals(mConfirmPasswordView.getText().toString())) {
			mConfirmPasswordView.setError("Passwords don't match");
			focusView = mConfirmPasswordView;
			cancel = true;
		}

		if (cancel) {
			// There was an error; don't attempt login and focus the first
			// form field with an error.
			focusView.requestFocus();
			return;
		}

		JSONObject user_info = new JSONObject();
		try {
			user_info.put("username", username);
			user_info.put("email", email);
		} catch (JSONException error) {
			//TODO
		}
		JsonObjectRequest registration_request = new JsonObjectRequest(
				Request.Method.POST,
				"https://petbot.ca:5000/SETUP/CHECK",
				user_info,
				new Response.Listener<JSONObject>() {
					@Override
					public void onResponse(JSONObject response) {

						showProgress(false);

						boolean success = false;
						try {

							success = response.getInt("status") == 1;
							if (success) {

								// open petbot setup activity, passing in registration email as a parameter
								Intent open_setup = new Intent(RegistrationActivity.this, SetupActivity.class);
								open_setup.putExtra("username", username);
								open_setup.putExtra("email", email);
								open_setup.putExtra("password", password);
								RegistrationActivity.this.startActivity(open_setup);

							} else {
								// TODO: should only put error message for user errors, not all errors
								mUsernameView.setError(response.getString("err_msg"));
								mUsernameView.requestFocus();
							}

						} catch (JSONException error) {
							//TODO
						}
					}
				},
				new Response.ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError error) {
						Log.e("asdfasdfasdf", error.toString());
						showProgress(false);
					}
				}
		);

		showProgress(true);
		RequestQueue queue = Volley.newRequestQueue(this);
		queue.add(registration_request);


	}

	private boolean isEmailValid(String email) {
		//TODO: Replace this with your own logic
		return email.contains("@");
	}

	private boolean isPasswordValid(String password) {
		//TODO: Replace this with your own logic
		return password.length() >= 8;
	}


	/**
	 * Shows the progress UI and hides the login form.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
	private void showProgress(final boolean show) {
		// On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
		// for very easy animations. If available, use these APIs to fade-in
		// the progress spinner.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
			int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

			mRegistrationFormView.setVisibility(show ? View.GONE : View.VISIBLE);
			mRegistrationFormView.animate().setDuration(shortAnimTime).alpha(
					show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					mRegistrationFormView.setVisibility(show ? View.GONE : View.VISIBLE);
				}
			});

			mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
			mProgressView.animate().setDuration(shortAnimTime).alpha(
					show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
				}
			});
		} else {
			// The ViewPropertyAnimator APIs are not available, so simply show
			// and hide the relevant UI components.
			mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
			mRegistrationFormView.setVisibility(show ? View.GONE : View.VISIBLE);
		}
	}


	@Override
	public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
		return new CursorLoader(this,
				// Retrieve data rows for the device user's 'profile' contact.
				Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
						ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

				// Select only email addresses.
				ContactsContract.Contacts.Data.MIMETYPE +
						" = ?", new String[]{ContactsContract.CommonDataKinds.Email
				.CONTENT_ITEM_TYPE},

				// Show primary email addresses first. Note that there won't be
				// a primary email address if the user hasn't specified one.
				ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
	}

	@Override
	public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
		List<String> emails = new ArrayList<>();
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			emails.add(cursor.getString(ProfileQuery.ADDRESS));
			cursor.moveToNext();
		}

		addEmailsToAutoComplete(emails);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> cursorLoader) {

	}

	private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
		//Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
		ArrayAdapter<String> adapter =
				new ArrayAdapter<>(RegistrationActivity.this,
						android.R.layout.simple_dropdown_item_1line, emailAddressCollection);

		mEmailView.setAdapter(adapter);
	}


	private interface ProfileQuery {
		String[] PROJECTION = {
				ContactsContract.CommonDataKinds.Email.ADDRESS,
				ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
		};

		int ADDRESS = 0;
		int IS_PRIMARY = 1;
	}

}

