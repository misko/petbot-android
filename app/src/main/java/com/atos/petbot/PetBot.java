package com.atos.petbot;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.media.AudioManager;

import android.graphics.BitmapFactory;

import java.io.InputStream;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.content.res.ColorStateList;

import java.io.IOException;
import java.net.URL;

import org.freedesktop.gstreamer.GStreamer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.error.VolleyError;
import com.android.volley.request.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import android.os.AsyncTask;
import com.atos.petbot.R;
import com.github.pwittchen.swipe.library.Swipe;
import com.github.pwittchen.swipe.library.SwipeListener;

import rx.Subscription;

public class PetBot extends AppCompatActivity implements SurfaceHolder.Callback {

	private native void nativeInit();     // Initialize native code, build pipeline, etc
	private native void nativePlayAgent(long jagent, int jstream_id);     // Initialize native code, build pipeline, etc
	private native void nativeFinalize(); // Destroy pipeline and shutdown native code
	private native void nativePlay();     // Set pipeline to PLAYING
	private native void nativePause();    // Set pipeline to PAUSED
	private static native boolean nativeClassInit(); // Initialize native class: cache Method IDs for callbacks
	private native void nativeSurfaceInit(Object surface);
	private native void nativeSurfaceFinalize();
	private long native_custom_data;      // Native code will use this to keep private data
	private int bb_streamer_id=0;

	boolean bye_pressed=false;
	boolean petbot_found=false;

	String status = "Connecting...";

	PBConnector pb;
	private Swipe swipe;

	Vibrator vibrator;


	private void set_status(String s) {
		status=s;
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				TextView connect_text = (TextView) findViewById(R.id.connect_status);
				connect_text.setText(status);
			}
		});
	}

	public void exit_with_toast(final String msg) {
		//TODO USE THE TOAST?
		runOnUiThread(new Runnable() {
			@Override
			public void run() {

				ApplicationState state = (ApplicationState) getApplicationContext();
				state.status=msg;
				pb.close();
				nativePause();
				finish();
				Intent open_main = new Intent(PetBot.this, LoginActivity.class);
				PetBot.this.startActivity(open_main);
			}
		});
	}

	private void look_for_petbot(int attempt) {
		while (!petbot_found) {
			if (attempt == 0) {
				set_status("Looking for your PetBot...");
				//"Looking for your PetBot..."
			} else {
				String s = "Looking for your PetBot... (x" + Integer.toString(attempt+1) + ")";
				set_status(s);
			}

			pb.getUptime();
			try {
				Thread.sleep(5000);
			} catch (InterruptedException ie) {
				return;
			}
			attempt++;
		}
 	}
	private class DownLoadImageTask extends AsyncTask<String,Void,Bitmap>{
		ImageView imageView;

		public DownLoadImageTask(ImageView imageView){
			this.imageView = imageView;
		}

		/*
            doInBackground(Params... params)
                Override this method to perform a computation on a background thread.
         */
		protected Bitmap doInBackground(String...urls){
			String urlOfImage = urls[0];
			Bitmap logo = null;
			try{
				InputStream is = new URL(urlOfImage).openStream();
                /*
                    decodeStream(InputStream is)
                        Decode an input stream into a bitmap.
                 */
				logo = BitmapFactory.decodeStream(is);
			}catch(Exception e){ // Catch the download exception
				e.printStackTrace();
			}
			return logo;
		}

		/*
            onPostExecute(Result result)
                Runs on the UI thread after doInBackground(Params...).
         */
		protected void onPostExecute(Bitmap result){
			imageView.setImageBitmap(result);
		}
	}

	@Override public boolean dispatchTouchEvent(MotionEvent event) {
		swipe.dispatchTouchEvent(event);
		return super.dispatchTouchEvent(event);
	}

	private void start_wait() {
		//code to forget user here
		JSONObject json_request = new JSONObject();

		JsonObjectRequest deauth_request = new JsonObjectRequest(
				Request.Method.POST,
				ApplicationState.HTTPS_ADDRESS_PB_WAIT,
				json_request,
				new Response.Listener<JSONObject>() {
					@Override
					public void onResponse(JSONObject response) {
						boolean success = false;
						try {
							success = response.getInt("status") == 1;
							JSONObject pet = response.getJSONObject("pet");
							String img = pet.getString("img");
							String name = pet.getString("name");
							String story = pet.getString("story");
							ImageView petImageView = (ImageView) findViewById(R.id.petImageView);
							new DownLoadImageTask(petImageView).execute(img);

							TextView petName = (TextView) findViewById(R.id.petName);
							petName.setText(name);
							TextView petStory = (TextView) findViewById(R.id.petStory);
							petStory.setText(story);
						} catch (JSONException error) {
							//TODO
						}

						if (success) {
						}
					}
				},
				new Response.ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError error) {
						Log.e("asdfasdfasdf", error.toString());
					}
				}
		);

		RequestQueue queue = Volley.newRequestQueue(this);
		queue.add(deauth_request);
	}

	private void enable_selfie_button(boolean x) {
		final FloatingActionButton selfieButton = (FloatingActionButton) this.findViewById(R.id.selfieButton);
		selfieButton.setEnabled(x);
		if (x) {
			selfieButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.PBBlueColor)));
		} else {
			selfieButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.PBGrayColor)));
		}
	}

	// Called when the activity is first created.
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		Log.w("petbot", "ANDROID - ON CREATE PETBOT" );
		start_wait();



		swipe = new Swipe();
		swipe.addListener(new SwipeListener() {
			@Override public void onSwipingLeft(final MotionEvent event) {
			}
			@Override public void onSwipingRight(final MotionEvent event) {
			}

			@Override public void onSwipingUp(final MotionEvent event) {
			}

			@Override public void onSwipingDown(final MotionEvent event) {
			}

			@Override public void onSwipedLeft(final MotionEvent event) {
				Log.w("petbot","SWIPED_LEFT");
				pb.camera_fx_down();
			}

			@Override public void onSwipedRight(final MotionEvent event) {
				Log.w("petbot","SWIPED_RIGHT");
				pb.camera_fx_up();
			}
			@Override public void onSwipedUp(final MotionEvent event) {
				Log.w("petbot","SWIPED_UP");
				pb.camera_exp_up();
			}

			@Override public void onSwipedDown(final MotionEvent event) {
				Log.w("petbot","SWIPED_DOWN");
				pb.camera_exp_down();
			}
		});


		/*RelativeLayout myView = (RelativeLayout) findViewById(R.id.mainLayout);
		myView.setOnTouchListener(new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				// ... Respond to touch events
				return true;
			}
		});*/

		ApplicationState state = (ApplicationState) this.getApplicationContext();

		pb = new PBConnector(state.server, state.port, state.server_secret);

		//start looking for the petbot
		final Thread look_thread = new Thread() {
			@Override
			public void run() {
				look_for_petbot(0);
			}
		};
		look_thread.setDaemon(true);
		look_thread.start();

		//start up a read thread
		final Thread read_thread = new Thread() {
			@Override
			public void run() {
				while (true) {
					Log.w("petbot", "ANDROID - READ MSG");
					final PBMsg m = pb.readPBMsg();
					Log.w("petbot", "ANDROID - READ MSG -DONE");
					if (m == null) {
						Log.w("petbot", "ANDROID - LEAVE READ THREAD");
						if (!bye_pressed) {
							exit_with_toast("PetBot connection closed");
						}
						break;
					}
					if ((m.pbmsg_type ^ (PBMsg.PBMSG_CLIENT | PBMsg.PBMSG_STRING))==0) {
						if (petbot_found==false) {
							String msg = new String(m.pbmsg);
							String[] parts = msg.split(" ");
							if (parts[0].equals("UPTIME")) {
								int uptime = Integer.parseInt(parts[1]);
								if (uptime > 20) {
									petbot_found = true;
									set_status("Negotiating with your PetBot...");
									pb.makeIceRequest();
								} else {
									set_status("Found your PetBot...");
								}
							}
						}
					} else if ((m.pbmsg_type ^  (PBMsg.PBMSG_SUCCESS | PBMsg.PBMSG_RESPONSE | PBMsg.PBMSG_ICE | PBMsg.PBMSG_CLIENT | PBMsg.PBMSG_STRING))==0) {
						Log.w("petbot","got ice response!!" + Integer.toString(bb_streamer_id));
						if (bb_streamer_id==0) {
							bb_streamer_id=m.pbmsg_from;
							Thread negotiate_thread = new Thread() {
								@Override
								public void run() {
									pb.iceNegotiate(m);

									runOnUiThread(new Runnable() {
										@Override
										public void run() {
											FrameLayout layout = (FrameLayout) findViewById(R.id.wait_screen);
											layout.setVisibility(View.GONE);
										}
									});

									nativePlayAgent(pb.ptr_agent, pb.stream_id);
								}
							};
							negotiate_thread.setDaemon(true);
							negotiate_thread.start();
						} else {
							//someone else connected
							exit_with_toast("Someone else connected :(");
							return;
						}

					} else if ((m.pbmsg_type ^  (PBMsg.PBMSG_CLIENT | PBMsg.PBMSG_VIDEO | PBMsg.PBMSG_RESPONSE | PBMsg.PBMSG_STRING | PBMsg.PBMSG_SUCCESS))==0) {
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								enable_selfie_button(true);
							}
						});
					} else {
						Log.w("petbot", "READ" + m.toString());
					}
				}
			}
		};
		read_thread.setDaemon(true);
		read_thread.start();


		//start up a read thread
		/*Thread request_thread = new Thread() {
			@Override
			public void run() {
				Log.w("petbot", "ANDROID - ICE REQUEST ");
				pb.iceRequest();
				Log.w("petbot", "ANDROID - ICE REQUEST DONE");
			}
		};
		request_thread.setDaemon(true);
		request_thread.start();*/

		Log.w("petbot", String.valueOf(pb.ptr_pbs));

		// Initialize GStreamer and warn if it fails
		try {
			GStreamer.init(this);
		} catch (Exception e) {
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
			finish();
			return;
		}

		setContentView(R.layout.main);

		// Get instance of Vibrator from current Context
		vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

		FloatingActionButton cookieButton = (FloatingActionButton) this.findViewById(R.id.cookieButton);
		cookieButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				vibrator.vibrate(400);
				pb.sendCookie();
			}
		});

		final FloatingActionButton selfieButton = (FloatingActionButton) this.findViewById(R.id.selfieButton);
		selfieButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				selfieButton.setEnabled(false);
				vibrator.vibrate(400);
				pb.takeSelfie();
			}
		});

		FloatingActionButton logoutButton = (FloatingActionButton) this.findViewById(R.id.logoutButton);
		logoutButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				bye_pressed=true;
				pb.close();
				nativePause();
				finish();
				Intent open_main = new Intent(PetBot.this, LoginActivity.class);
				PetBot.this.startActivity(open_main);
			}
		});

		FloatingActionButton settingsButton = (FloatingActionButton) findViewById(R.id.settingsButton);
		settingsButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent open_settings = new Intent(PetBot.this, SettingsActivity.class);
				PetBot.this.startActivity(open_settings);
			}
		});

		FloatingActionButton soundButton = (FloatingActionButton) this.findViewById(R.id.soundButton);
		soundButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				playSound();
			}
		});

		/*SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		if(preferences.contains("alert_sounds")){
			setSound(preferences.getString("alert_sounds", ""));
		} else {


			JSONObject sounds_info = new JSONObject();
			try {
				sounds_info.put("file_type", "mp3");
				sounds_info.put("start_idx", 0);
				sounds_info.put("end_idx", 0); //TODO: start_idx and end_idx are not used in server
			} catch (JSONException error) {
				//TODO
			}

			final String server_secret = state.server_secret;
			JsonObjectRequest sounds_request = new JsonObjectRequest(
					Request.Method.POST,
					"https://petbot.ca:5000/FILES_LS/" + state.server_secret,
					sounds_info,
					new Response.Listener<JSONObject>() {
						@Override
						public void onResponse(JSONObject response) {

							Log.e("petbot", response.toString());
							boolean success = false;
							try {

								success = response.getInt("status") == 1;
								if (success) {

									JSONArray sound_files = response.getJSONArray("files");
									if (sound_files.length() > 0) {

										// get the file id of the first sound in the list
										final String file_id = sound_files.getJSONArray(0).getString(0);
										setSound(file_id);

									}
								}
							} catch (JSONException error) {
								//TODO
								Log.e("petbot", error.toString());
							}

						}
					},
					new Response.ErrorListener() {
						@Override
						public void onErrorResponse(VolleyError error) {
							Log.e("petbot", error.toString());
						}
					}
			);

			RequestQueue queue = Volley.newRequestQueue(this);
			queue.add(sounds_request);
		}*/

		SurfaceView sv = (SurfaceView) this.findViewById(R.id.surface_video);
		SurfaceHolder sh = sv.getHolder();
		sh.addCallback(this);

		nativeInit();

	}

	public void playSound() {
		final ApplicationState application = (ApplicationState) getApplicationContext();
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		String sound_ID  = preferences.getString("alert_sounds", "00000000000000000000000000000000");
		vibrator.vibrate(400);
		String url = "https://petbot.ca:5000/FILES_DL/" + application.server_secret + "/" + sound_ID;
		MediaPlayer mediaPlayer = new MediaPlayer();
		mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		try {
			mediaPlayer.setDataSource(url);
			mediaPlayer.prepare(); // might take long! (for buffering, etc)
		} catch (IOException error) {
			//TODO
			Log.e("petbot", error.toString());
		}
		mediaPlayer.start();
		pb.playSound(url);
	}

	public void setSound(final String sound_ID){


		final ApplicationState application = (ApplicationState) getApplicationContext();
		FloatingActionButton soundButton = (FloatingActionButton) this.findViewById(R.id.soundButton);
		soundButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				vibrator.vibrate(400);
				String url = "https://petbot.ca:5000/FILES_DL/" + application.server_secret + "/" + sound_ID;
				MediaPlayer mediaPlayer = new MediaPlayer();
				mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
				try {
					mediaPlayer.setDataSource(url);
					mediaPlayer.prepare(); // might take long! (for buffering, etc)
				} catch (IOException error) {
					//TODO
					Log.e("petbot", error.toString());
				}
				mediaPlayer.start();
				pb.playSound(url);
			}
		});
	}

	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		FloatingActionButton selfieButton = (FloatingActionButton) this.findViewById(R.id.selfieButton);
		FloatingActionButton soundButton = (FloatingActionButton) this.findViewById(R.id.soundButton);
		FloatingActionButton settingsButton = (FloatingActionButton) this.findViewById(R.id.settingsButton);

		RelativeLayout.LayoutParams selfie_layout = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		RelativeLayout.LayoutParams sound_layout = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		RelativeLayout.LayoutParams settings_layout = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);

		// Checks the orientation of the screen
		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {

			selfie_layout.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
			selfie_layout.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);

			sound_layout.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
			sound_layout.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);

			settings_layout.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
			settings_layout.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);

		} else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){

			selfie_layout.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
			selfie_layout.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);

			sound_layout.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
			sound_layout.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);

			settings_layout.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
			settings_layout.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
		}

		selfieButton.setLayoutParams(selfie_layout);
		soundButton.setLayoutParams(sound_layout);
		settingsButton.setLayoutParams(settings_layout);
		Log.w("petbot","WTF CONFIG CHANGE?");
	}

	protected void onSaveInstanceState (Bundle outState) {
		Log.d ("GStreamer", "Saving state, playing:" );
		//outState.putBoolean("playing", is_playing_desired);
	}

	protected void onDestroy() {
		Log.w("Petbot","WTF WTF DESTROY????");
		Log.d("Petbot", Log.getStackTraceString(new Exception()));
		nativeFinalize();
		super.onDestroy();
	}

	// Called from native code. This sets the content of the TextView from the UI thread.
	private void setMessage(final String message) {
		final TextView tv = (TextView) this.findViewById(R.id.textview_message);
		runOnUiThread (new Runnable() {
			public void run() {
				tv.setText(message);
			}
		});
	}

	// Called from native code. Native code calls this once it has created its pipeline and
	// the main loop is running, so it is ready to accept commands.
	private void onGStreamerInitialized () {
		Log.w("petbot","GSTREAMER PLAY!!");
		nativePlay();
	}

	static {
		System.loadLibrary("crypto");
		System.loadLibrary("ssl");
		System.loadLibrary("gstreamer_android");
		System.loadLibrary("pb_gst");
		System.loadLibrary("PBConnector");
		nativeClassInit();
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int width,
							   int height) {
		Log.d("GStreamer", "Surface changed to format " + format + " width "
				+ width + " height " + height);
		nativeSurfaceInit (holder.getSurface());

	}

	public void surfaceCreated(SurfaceHolder holder) {
		Log.d("GStreamer", "Surface created: " + holder.getSurface());
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.d("GStreamer", "Surface destroyed");
		nativeSurfaceFinalize ();
		Log.d("GStreamer", "Surface destroyed - done");
	}

	@Override
	protected void onResume(){
		Log.w("petbot", "onResume - petbot JAVA");
		super.onResume();
	}

}
