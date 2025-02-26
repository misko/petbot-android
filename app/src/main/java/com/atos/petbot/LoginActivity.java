package com.atos.petbot;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
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
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.iid.FirebaseInstanceId;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends Activity implements LoaderCallbacks<Cursor> {

	// Id to identity READ_CONTACTS permission request.
	private static final int REQUEST_READ_CONTACTS = 0;

	// UI references.
	private EditText mUsernameView;
	private EditText mPasswordView;
	private View mProgressView;
	private View mLoginFormView;
	private SharedPreferences sharedPreferences;

	boolean debug_mode = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);

		// Set up the login form.
		mUsernameView = (EditText) findViewById(R.id.username);
		populateAutoComplete();

		mPasswordView = (EditText) findViewById(R.id.password);
		mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
				if (id == R.id.login || id == EditorInfo.IME_NULL) {
					attemptLogin();
					return true;
				}
				return false;
			}
		});

		Button mUsernameSignInButton = (Button) findViewById(R.id.username_sign_in_button);
		mUsernameSignInButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				attemptLogin();
			}
		});

		mUsernameSignInButton.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View view) {

				debug_mode = true;
				FirebaseLogger.setLogLevel(FirebaseLogger.log_level.DEBUG);
				view.setBackgroundColor(getResources().getColor(R.color.PBRedColor));
				FirebaseLogger.logInfo("debug mode set");

				return false;
			}
		});

		Button forgetMeButton = (Button) findViewById(R.id.forgetme);
		forgetMeButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				deauth();
			}
		});

		Button registerButton = (Button) findViewById(R.id.setup);
		registerButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent open_registration = new Intent(LoginActivity.this, RegistrationActivity.class);
				LoginActivity.this.startActivity(open_registration);
			}
		});


		try {
			String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
			TextView versionStringView = (TextView) findViewById(R.id.versionText);
			versionStringView.setText(versionName);
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
		}

		mLoginFormView = findViewById(R.id.login_form);
		mProgressView = findViewById(R.id.login_progress);

		// Get SharedPreferences
		sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		// set UI
		String strusername = sharedPreferences.getString("username", "");
		String strpassword = sharedPreferences.getString("password", "");
		mUsernameView.setText(strusername);
		mPasswordView.setText(strpassword);


	}

	private void deauth() {

		FirebaseLogger.logInfo("deauthenticating user");
		JSONObject json_request = new JSONObject();
		try {
			json_request.put("deviceID", FirebaseInstanceId.getInstance().getToken());
		} catch (JSONException error) {
			FirebaseLogger.logError(error.toString());
		}

		JsonObjectRequest deauth_request = new JsonObjectRequest(
				Request.Method.POST,
				ApplicationState.HTTPS_ADDRESS_DEAUTH,
				json_request,
				new Response.Listener<JSONObject>() {
					@Override
					public void onResponse(JSONObject response) {

						FirebaseLogger.logDebug("deauth response:\n" + response.toString());
						showProgress(false);

						boolean success = false;
						try {
							success = response.getInt("status") == 1;
						} catch (JSONException error) {
							FirebaseLogger.logError(error.toString());
						}

						FirebaseLogger.logInfo("deauth success; " + Boolean.toString(success));
						if (success) {
							SharedPreferences.Editor editor = sharedPreferences.edit();
							editor.putString("username", "");
							editor.putString("password", "");
							editor.commit();
							mPasswordView.setText("");
							mUsernameView.setText("");
						}
					}
				},
				new Response.ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError error) {
						FirebaseLogger.logError("deauth error:\n" + error.toString());
						showProgress(false);
					}
				}
		);

		RequestQueue queue = Volley.newRequestQueue(this);
		queue.add(deauth_request);
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


	private void hideKeyboard() {
		//hide the keyboard
		View view = this.getCurrentFocus();
		if (view != null) {
			InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
		}
	}


	/**
	 * Attempts to sign in or register the account specified by the login form.
	 * If there are form errors (invalid email, missing fields, etc.), the
	 * errors are presented and no actual login attempt is made.
	 */
	private void attemptLogin() {

		FirebaseLogger.logInfo("attempting login");

		hideKeyboard();
		// Reset errors.
		mUsernameView.setError(null);
		mPasswordView.setError(null);

		// Store values at the time of the login attempt.
		final String username = mUsernameView.getText().toString();
		final String password = mPasswordView.getText().toString();

		boolean cancel = false;
		View focusView = null;

		// Check for a valid password, if the user entered one.
		if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
			mPasswordView.setError(getString(R.string.error_invalid_password));
			focusView = mPasswordView;
			cancel = true;
		}

		// Check for a valid email address.
		if (TextUtils.isEmpty(username)) {
			mUsernameView.setError(getString(R.string.error_field_required));
			focusView = mUsernameView;
			cancel = true;
		} else if (!isEmailValid(username)) {
			mUsernameView.setError(getString(R.string.error_invalid_email));
			focusView = mUsernameView;
			cancel = true;
		}

		if (cancel) {
			// There was an error; don't attempt login and focus the first
			// form field with an error.
			focusView.requestFocus();
		} else {
			// Show a progress spinner, and kick off a background task to
			// perform the user login attempt.
			showProgress(true);

			JSONObject login_info = new JSONObject();
			try {
				login_info.put("username", username);
				login_info.put("password", password);
				login_info.put("deviceID", FirebaseInstanceId.getInstance().getToken());
				login_info.put("notifier", "firebase");
			} catch (JSONException error) {
				FirebaseLogger.logError(error.toString());
			}

			final Context context = this.getApplicationContext();
			JsonObjectRequest login_request = new JsonObjectRequest(
					Request.Method.POST,
					ApplicationState.HTTPS_ADDRESS_AUTH,
					login_info,
					new Response.Listener<JSONObject>() {
						@Override
						public void onResponse(JSONObject response) {

							FirebaseLogger.logDebug("login response:\n" + response.toString());
							showProgress(false);

							boolean success = false;
							try {
								success = response.getInt("status") == 1;
							} catch (JSONException error) {
								FirebaseLogger.logError(error.toString());
							}

							if (success) {

								if(debug_mode) {
									FirebaseAnalytics firebase = FirebaseAnalytics.getInstance(context);
									firebase.setUserId(username);
								}
								FirebaseLogger.logInfo("login success");

								SharedPreferences.Editor editor = sharedPreferences.edit();
								editor.putString("username", username);
								editor.putString("password", password);
								editor.commit();

								try {
									JSONObject pbserver = response.getJSONObject("pubsubserver");
									JSONObject turnserver = response.getJSONArray("turn").getJSONObject(0);

									ApplicationState state = (ApplicationState) getApplicationContext();
									state.server_secret = pbserver.getString("secret");
									state.server = pbserver.getString("server");
									state.port = pbserver.getInt("port");
									state.username = pbserver.getString("username");

									state.stun_server = turnserver.getString("server");
									state.stun_port = turnserver.getString("port");
									state.stun_username = turnserver.getString("username");
									state.stun_password = turnserver.getString("password");
									state.debug_mode=debug_mode;

									//Settings.updateable = !TextUtils.isEmpty(response.getString("updates_allowed"));
									state.updateable = response.getString("updates_allowed");

									Intent open_main = new Intent(LoginActivity.this, PetBot.class);
									LoginActivity.this.startActivity(open_main);
									finish();

								} catch (JSONException error) {
									FirebaseLogger.logError(error.toString());
								}

							} else {
								// TODO: put reason for login failure
								FirebaseLogger.logInfo("login failed");
								mPasswordView.setError(getString(R.string.error_incorrect_password));
								mPasswordView.requestFocus();
							}
						}
					},
					new Response.ErrorListener() {
						@Override
						public void onErrorResponse(VolleyError error) {
							FirebaseLogger.logError("login error:\n"  + error.toString());
							showProgress(false);
							Toast toast = Toast.makeText(getApplicationContext(), error.toString(), Toast.LENGTH_SHORT);
							toast.setGravity(Gravity.TOP|Gravity.CENTER, 0, 0);
							toast.show();
						}
					}
			);

			RequestQueue queue = Volley.newRequestQueue(this);
			FirebaseLogger.logInfo("adding login request to queue");
			queue.add(login_request);

		}
	}


	private boolean isEmailValid(String email) {
		//TODO: Replace this with your own logic
		return true;//email.contains("@");
	}

	private boolean isPasswordValid(String password) {
		//TODO: Replace this with your own logic
		return password.length() > 4;
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

			mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
			mLoginFormView.animate().setDuration(shortAnimTime).alpha(
					show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
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
			mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
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
				new ArrayAdapter<>(LoginActivity.this,
						android.R.layout.simple_dropdown_item_1line, emailAddressCollection);

		//mUsernameView.setAdapter(adapter);
	}


	private interface ProfileQuery {
		String[] PROJECTION = {
				ContactsContract.CommonDataKinds.Email.ADDRESS,
				ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
		};

		int ADDRESS = 0;
		int IS_PRIMARY = 1;
	}

	@Override
	public void onResume() {
		super.onResume();
		ApplicationState state = (ApplicationState) getApplicationContext();
		if (!state.status.equals("")) {
			Toast toast = Toast.makeText(getApplicationContext(), state.status, Toast.LENGTH_SHORT);
			toast.setGravity(Gravity.TOP|Gravity.CENTER, 0, 0);
			toast.setDuration(5);
			toast.show();
			state.status="";
		}
	}

}

