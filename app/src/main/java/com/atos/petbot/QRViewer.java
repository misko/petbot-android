package com.atos.petbot;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.error.VolleyError;
import com.android.volley.request.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.atos.petbot.R;

import org.json.JSONException;
import org.json.JSONObject;

public class QRViewer extends AppCompatActivity {

	private View mProgressView;


	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_qrviewer);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		mProgressView = findViewById(R.id.qr_progress);

		// TODO: better to download image to temp and display in gallery?
		String qr_text = getIntent().getExtras().getString("qr_text");
		final JSONObject qr_info = new JSONObject();
		try {
			qr_info.put("text", qr_text);
		} catch (JSONException error) {
			//TODO
		}

		showProgress(true);
		final ImageView mQRView = (ImageView) findViewById(R.id.qr_code);


		JsonObjectRequest login_request = new JsonObjectRequest(
				Request.Method.POST,
				ApplicationState.HTTPS_ADDRESS_QRCODE_JSON,
				qr_info,
				new Response.Listener<JSONObject>() {
					@Override
					public void onResponse(JSONObject response) {
						showProgress(false);
					}
				},
				new Response.ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError error) {
						Log.e("asdfasdfasdf", error.toString());
						showProgress(false);
					}
				}
		){
			@Override
			protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
				Bitmap bmp = BitmapFactory.decodeByteArray(response.data, 0, response.data.length);
				mQRView.setImageBitmap(bmp);
				return null;
			}
		};

		RequestQueue queue = Volley.newRequestQueue(this);
		queue.add(login_request);
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
		}
	}

}
