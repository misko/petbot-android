package com.petbot;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.Volley;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

public class QRViewer extends Activity {

	private View mProgressView;

	@Override
	protected void onResume() {

		super.onResume();
		final ApplicationState state = (ApplicationState) getApplicationContext();

		TextView IP_text = (TextView) findViewById(R.id.ip_label);
		IP_text.setText("IP address: " + state.IP + ":" + Integer.toString(state.port));

		Thread server_thread = new Thread() {
			@Override
			public void run() {
				try {
					Boolean end = false;
					ServerSocket ss = new ServerSocket(state.port);
					ss.setReuseAddress(true);
					//Server is waiting for client here, if needed
					Log.e("petbot","waiting for petbot to connect");
					Socket s = ss.accept();
					s.close();

					Log.e("petbot","petbot connected");
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							//TextView t=(TextView)findViewById(R.id.petbot_status);
							//t.setText("CONNECTED");
							Intent open_main = new Intent(QRViewer.this, PetBot.class);
							open_main.putExtra("image_url", getIntent().getExtras().getString("image_url"));
							QRViewer.this.startActivity(open_main);
						}

					});
					ss.close();

				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
		server_thread.start();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_qrviewer);
		getActionBar().setDisplayHomeAsUpEnabled(true);

		mProgressView = findViewById(R.id.qr_progress);

		// TODO: better to download image to temp and display in gallery?

		showProgress(true);
		final ImageView mQRView = (ImageView) findViewById(R.id.qr_code);
		ImageRequest qr_request = new ImageRequest(
				getIntent().getExtras().getString("image_url"),
				new Response.Listener<Bitmap>() {
					@Override
					public void onResponse(Bitmap bitmap) {
						showProgress(false);
						mQRView.setImageBitmap(bitmap);
					}
				},
				500,
				500,
				ImageView.ScaleType.CENTER_INSIDE,
				Bitmap.Config.RGB_565,
				new Response.ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError error) {
						// TODO: display error?
						Log.e("asdfasdfasdf", error.toString());
						Log.e("asdfasdfasdf", error.networkResponse.toString());
						showProgress(false);
					}
				}
		);

		RequestQueue queue = Volley.newRequestQueue(this);
		queue.add(qr_request);





		/*Button continue_button = (Button) findViewById(R.id.continue_button);
		continue_button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent open_main = new Intent(QRViewer.this, PetBot.class);
				QRViewer.this.startActivity(open_main);
			}
		});*/

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
