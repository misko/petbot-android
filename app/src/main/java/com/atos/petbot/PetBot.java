package com.atos.petbot;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.media.AudioManager;

import android.graphics.BitmapFactory;

import java.io.InputStream;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.annotation.IntegerRes;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
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

import static android.graphics.Paint.ANTI_ALIAS_FLAG;

public class PetBot extends AppCompatActivity implements SurfaceHolder.Callback {

	private native void nativeInit();     // Initialize native code, build pipeline, etc
	private native void nativePlayAgent(long jagent, int jstream_id);     // Initialize native code, build pipeline, etc
	private native void nativeFinalize(); // Destroy pipeline and shutdown native code
	private native void nativePlay();     // Set pipeline to PLAYING
	private native void nativePause();    // Set pipeline to PAUSED
	private native int[] jitterStats();
	private static native boolean nativeClassInit(); // Initialize native class: cache Method IDs for callbacks
	private native void nativeSurfaceInit(Object surface);
	private native void nativeSurfaceFinalize();
	private long native_custom_data;      // Native code will use this to keep private data
	private int bb_streamer_id = 0;
	private int waiting_selfies = 0;

	Handler handler;
	private TextView ice_textview=null;
	private TextView fps_textview=null;


	boolean bye_pressed = false;
	boolean petbot_found = false;

	FloatingActionButton selfieButton;

	String status = "";

	PBConnector pb;
	private Swipe swipe;

	Vibrator vibrator;



	private void set_status(final String s) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				TextView connect_text = (TextView) findViewById(R.id.connect_status);
				connect_text.setText(s);
			}
		});
	}

	public void exit_streaming() {
		//TODO USE THE TOAST?
		runOnUiThread(new Runnable() {
			@Override
			public void run() {

				ApplicationState state = (ApplicationState) getApplicationContext();
				if (!status.isEmpty()) {
					state.status = status;
				}
				if (pb!=null) {
					pb.close();
					pb=null;
					Log.w("petbot", "ANDROID - SETTING NULL" );
					nativePause();
				}
				finish();

				Log.w("petbot", "ANDROID - FINISH" );
				Intent open_main = new Intent(PetBot.this, LoginActivity.class);
				PetBot.this.startActivity(open_main);
			}
		});
	}

	private void fps_monitor() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {

				Log.w("petbot", "ANDROID - JITTER" );
				if (pb != null) {
					int jt[] = jitterStats();
					Log.w("petbot", "ANDROID - JITTER x2" );
					if (jt != null) {
						Log.w("petbot", "ANDROID - JITTER x3" );
						fps_textview.setText(Integer.toString(jt[0]) + "/" + Integer.toString(jt[1]) + "/" + Integer.toString(jt[2]) + "/" + Integer.toString(jt[3]));
					}
				}
			}
		});
	}

	private void look_for_petbot(int attempt) {

		while (!petbot_found) {

			if (attempt == 0) {
				set_status("Looking for your PetBot...");
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

		JsonObjectRequest story_request = new JsonObjectRequest(
				Request.Method.POST,
				ApplicationState.HTTPS_ADDRESS_PB_WAIT,
				json_request,
				new Response.Listener<JSONObject>() {

					@Override
					public void onResponse(JSONObject response) {

						FirebaseLogger.logDebug("pet story response:\n" + response.toString());
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
							FirebaseLogger.logError(error.toString());
						}

					}
				},
				new Response.ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError error) {
						FirebaseLogger.logError("pet story error:\n" + error.toString());
					}
				}
		);

		RequestQueue queue = Volley.newRequestQueue(this);
		queue.add(story_request);
	}

	private void enable_selfie_button(boolean x) {
		selfieButton.setEnabled(x);
		if (x) {
			selfieButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.PBBlueColor)));
		} else {
			selfieButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.PBGrayColor)));
		}
	}

	public static Bitmap textAsBitmap(String text, float textSize, int textColor) {
		Paint paint = new Paint(ANTI_ALIAS_FLAG);
		paint.setTextSize(textSize);
		paint.setColor(textColor);
		paint.setTextAlign(Paint.Align.LEFT);
		float baseline = -paint.ascent(); // ascent() is negative
		int width = (int) (paint.measureText(text) + 0.0f); // round
		int height = (int) (baseline + paint.descent() + 0.0f);
		Bitmap image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

		Canvas canvas = new Canvas(image);
		canvas.drawText(text, 0, baseline, paint);
		return image;
	}

	private void check_selfie(final boolean activate) {

		ApplicationState state = (ApplicationState) this.getApplicationContext();

		JSONObject json_request = new JSONObject();
		JsonObjectRequest selfie_request = new JsonObjectRequest(
				Request.Method.POST,
				ApplicationState.HTTPS_ADDRESS_PB_SELFIE_LAST+state.server_secret,
				json_request,
				new Response.Listener<JSONObject>() {

					@Override
					public void onResponse(JSONObject response) {

						FirebaseLogger.logDebug("pet story response:\n" + response.toString());
						boolean success = false;
						String rm_url = "";
						String media_url = "";
						try {
							success = response.getInt("status") == 1;
							waiting_selfies = response.getInt("count");
							rm_url = response.getString("rm_url");
							media_url = response.getString("selfie_url");
						} catch (JSONException error) {
							FirebaseLogger.logError(error.toString());
						}

						if (!success) {
							// TODO: log reason why this was not a success
							waiting_selfies = 0;
						}

						//run the corresponding event
						if (waiting_selfies > 0) {

							if (activate) {
								waiting_selfies--;
								//run selfie activity
								Intent open_selfie = new Intent(PetBot.this, SelfieActivity.class);
								open_selfie.putExtra("rm_url", rm_url);
								open_selfie.putExtra("media_url", media_url);
								PetBot.this.startActivity(open_selfie);
								if (waiting_selfies==0) {
									enable_selfie_button(true);
								}
							}

							if (waiting_selfies > 0) {
								selfieButton.setImageBitmap(textAsBitmap(Integer.toString(waiting_selfies),40, getResources().getColor(R.color.PBTextWhite)));
								selfieButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.PBRedColor)));
							} else {
								selfieButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_video));
								selfieButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.PBBlueColor)));
							}

						} else {
							if (activate) {
								vibrator.vibrate(400);
								pb.takeSelfie();
								enable_selfie_button(false);
							}
						}

					}
				},
				new Response.ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError error) {
						FirebaseLogger.logError("pet story error:\n" + error.toString());
						waiting_selfies = 0;
					}
				}
		);

		RequestQueue queue = Volley.newRequestQueue(this);
		queue.add(selfie_request);

	}

	// Called when the activity is first created.
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		Log.w("petbot", "ANDROID - ON CREATE PETBOT" );

		setContentView(R.layout.main);

		final ApplicationState state = (ApplicationState) this.getApplicationContext();

		set_status("Connecting...");
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		Foreground.Listener myListener = new Foreground.Listener() {
			@Override
			public void onBecameForeground() {
				Log.w("petbot","WTF FOREGROUND????!?!!?");

			}

			@Override
			public void onBecameBackground() {
				Log.w("petbot","WTF BACKGROUND????!?!!?");
				exit_streaming();
			}
		};
		 Foreground.get(this).addListener(myListener);

		status="";
		ice_textview = (TextView) this.findViewById(R.id.ice_textview);
		fps_textview = (TextView) this.findViewById(R.id.fps_textview);
		if (!state.debug_mode) {
			ice_textview.setVisibility(View.GONE);
			fps_textview.setVisibility(View.GONE);
		} else {

			handler =new Handler();
			final int delay = 1000; //milliseconds
			handler.postDelayed(new Runnable(){
				public void run(){
					fps_monitor();
					if (pb!=null) {
						handler.postDelayed(this, delay);
					}
				}
			}, delay);
		}
		ice_textview.setText("");
		fps_textview.setText("");


		pb = new PBConnector(state.server, state.port, state.server_secret, state.stun_server, state.stun_port, state.stun_username, state.stun_password);
		Log.w("petbot","GOT EHRE");
		Log.w("petbot","GOT EHRE");
		Log.w("petbot","GOT EHRE");
		Log.w("petbot","GOT EHRE");
		Log.w("petbot","GOT EHRE");
		Log.w("petbot","GOT EHRE");
		Log.w("petbot","GOT EHRE");
		if (!pb.getError().isEmpty()) {
			pb.log("INIT ERROR " + pb.getError());
			status = pb.getError();
			Log.w("petbot","GOT EHREx3");
			Log.w("petbot","GOT EHREx3");
			Log.w("petbot","GOT EHREx3");
			Log.w("petbot","GOT EHREx3");
			exit_streaming();
			return;
		}
		Log.w("petbot","GOT EHREx2");
		Log.w("petbot","GOT EHREx2");
		Log.w("petbot","GOT EHREx2");
		Log.w("petbot","GOT EHREx2");
		Log.w("petbot","GOT EHREx2");

		start_wait();



        this.findViewById(android.R.id.content).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                pb.camera_hflip();
                return true;
            }
        });


		swipe = new Swipe();
		swipe.addListener(new SwipeListener() {

			@Override public void onSwipingLeft(final MotionEvent event) {}
			@Override public void onSwipingRight(final MotionEvent event) {}
			@Override public void onSwipingUp(final MotionEvent event) {}
			@Override public void onSwipingDown(final MotionEvent event) {}

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

						Log.w("petbot", "ANDROID - BLANK MESSAGE MOVE TO EXIT");
						if (!bye_pressed && !state.status.isEmpty()) {
							FirebaseLogger.logError("connection closed");
							status="PetBot connection closed";
						}
						exit_streaming();
						Log.w("petbot", "EXIT HERE X5");
						return;
					}

					if ((m.pbmsg_type ^ (PBMsg.PBMSG_CLIENT | PBMsg.PBMSG_STRING)) == 0) {

							//m.pbmsg.toString().substring(0,"UPTIME".length()).equals("UPTIME")) {
						Log.w("petbot",new String(m.pbmsg));
						Log.w("petbot",m.pbmsg.toString().substring(0,"UPTIME".length()));
						if (!petbot_found) {
							String msg = new String(m.pbmsg);
							String[] parts = msg.split(" ");
							if (parts.length>=3) {
								int uptime = Integer.parseInt(parts[1]);
								if (uptime > 20) {
									petbot_found = true;
									set_status("Negotiating with your PetBot...");
									if (parts.length>=5) {
										if (Integer.parseInt(parts[4])>1) {
											pb.nice_mode=pb.NICE_MODE_SDP;
										}
									} else {
										pb.nice_mode=pb.NICE_MODE_OLD;
									}
									pb.init();  //TODO WATCH OUT FOR TIMEOUTS HERE!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
									if (!pb.getError().isEmpty()) {
										status=pb.getError();
										exit_streaming();
										Log.w("petbot", "EXIT HERE X2");
										return;
									}
									pb.makeIceRequest();
								} else {
									set_status("Found your PetBot...");
								}
							}
						}


					} else if ((m.pbmsg_type ^  (PBMsg.PBMSG_SUCCESS | PBMsg.PBMSG_RESPONSE | PBMsg.PBMSG_WEBRTC | PBMsg.PBMSG_CLIENT | PBMsg.PBMSG_STRING)) == 0) {
						FirebaseLogger.logWarn("multiple client connection attempt");
						status="Someone else connected :(";
						exit_streaming();
						Log.w("petbot", "EXIT HERE X4");
						return;
					} else if ((m.pbmsg_type ^  (PBMsg.PBMSG_SUCCESS | PBMsg.PBMSG_RESPONSE | PBMsg.PBMSG_ICE | PBMsg.PBMSG_CLIENT | PBMsg.PBMSG_STRING)) == 0) {

						FirebaseLogger.logError("got ice response: " + Integer.toString(bb_streamer_id));
						if (bb_streamer_id == 0) {
							bb_streamer_id = m.pbmsg_from;
							Thread negotiate_thread = new Thread() {

								@Override
								public void run() {

									Log.w("petbot", "ANDROID - START ICE" );
									pb.iceNegotiate(m);
									if (!pb.getError().isEmpty()) {

										Log.w("petbot", "ANDROID - ERROR IN ICE" );
										pb.log("ICE NEGOTIATION FAILED " + pb.getError());
										status = pb.getError();
										exit_streaming();
										Log.w("petbot", "EXIT HERE X3");
										return;
									}
									Log.w("petbot", "ANDROID - ICE GOOD" );

									pb.log("ICE NEGOTIATION SUCCESS");
									runOnUiThread(new Runnable() {
										@Override
										public void run() {
											if (state.debug_mode) {
												ice_textview.setText(pb.getIcePair());
												pb.log("ICE PAIR " + pb.getIcePair());

											}
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
							FirebaseLogger.logWarn("multiple client connection attempt");
							status="Someone else connected :(";
							exit_streaming();
							Log.w("petbot", "EXIT HERE X4");
							return;
						}

					} else if ((m.pbmsg_type ^  (PBMsg.PBMSG_CLIENT | PBMsg.PBMSG_VIDEO | PBMsg.PBMSG_RESPONSE | PBMsg.PBMSG_STRING | PBMsg.PBMSG_SUCCESS)) == 0) {

						runOnUiThread(new Runnable() {

							@Override
							public void run() {

								enable_selfie_button(true);

								final Handler handler = new Handler();
								handler.postDelayed(new Runnable() {
									@Override
									public void run() {
										check_selfie(false);
									}
								}, 10*1000);
								handler.postDelayed(new Runnable() {
									@Override
									public void run() {
										check_selfie(false);
									}
								}, 30*1000);
								handler.postDelayed(new Runnable() {
									@Override
									public void run() {
										check_selfie(false);
									}
								}, 60*1000);
							}
						});

					} else if ((m.pbmsg_type & PBMsg.PBMSG_DISCONNECTED) != 0) {

						if (m.pbmsg_from == bb_streamer_id) {

							if (!bye_pressed) {
								FirebaseLogger.logError("PetBot connection closed");
								status="PetBot connection closed";
								exit_streaming();
							}
							Log.w("petbot", "EXIT HERE X1");
							return;

						} else {
							FirebaseLogger.logWarn("other client disconnected: " + Integer.toString(bb_streamer_id));
							//fprintf(stderr,"SOMEONE ELSE DISCONNETED %d vs %d\n",bb_streamer_id,m->pbmsg_from);
						}

					} else {
						FirebaseLogger.logWarn("unknown message:\n" + m.toString());
					}
				}
			}
		};
		read_thread.setDaemon(true);
		read_thread.start();

		Log.w("petbot", String.valueOf(pb.ptr_pbs));

		// Initialize GStreamer and warn if it fails
		try {
			GStreamer.init(this);
		} catch (Exception e) {
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
			finish();
			return;
		}

		// Get instance of Vibrator from current Context
		vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

		FloatingActionButton cookieButton = (FloatingActionButton) this.findViewById(R.id.cookieButton);
		cookieButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				vibrator.vibrate(400);
				pb.sendCookie();
			}
		});

		selfieButton = (FloatingActionButton) this.findViewById(R.id.selfieButton);
		selfieButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				check_selfie(true);
			}
		});

		FloatingActionButton logoutButton = (FloatingActionButton) this.findViewById(R.id.logoutButton);
		logoutButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				bye_pressed=true;
				exit_streaming();
			}
		});

		FloatingActionButton settingsButton = (FloatingActionButton) findViewById(R.id.settingsButton);
		settingsButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				nativePause();
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

		SurfaceView sv = (SurfaceView) this.findViewById(R.id.surface_video);
		SurfaceHolder sh = sv.getHolder();
		sh.addCallback(this);

		nativeInit();

		check_selfie(false);


	}

	public void playSound() {

		final ApplicationState application = (ApplicationState) getApplicationContext();

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		String sound_ID  = preferences.getString("alert_sounds", "00000000000000000000000000000000");
		vibrator.vibrate(400);

		String url = ApplicationState.HTTPS_ADDRESS_PB_DL+ application.server_secret + "/" + sound_ID;
		MediaPlayer mediaPlayer = new MediaPlayer();
		mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		try {
			mediaPlayer.setDataSource(url);
			mediaPlayer.prepare(); // might take long! (for buffering, etc)
		} catch (IOException error) {
			FirebaseLogger.logError(error.toString());
		}

		mediaPlayer.start();
		pb.playSound(url);
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

		if(!isFinishing()) {
			try {
				new Exception();
			} catch(Exception error) {
				FirebaseLogger.logError(Log.getStackTraceString(error));
			}
		}

		nativeFinalize();
		super.onDestroy();
	}

	// Called from native code. This sets the content of the TextView from the UI thread.
	private void setMessage(final String message) {
		//final TextView tv = (TextView) this.findViewById(R.id.textview_message);
		/*runOnUiThread (new Runnable() {
			public void run() {
				tv.setText(message);
			}
		});*/
	}

	// Called from native code. Native code calls this once it has created its pipeline and
	// the main loop is running, so it is ready to accept commands.
	private void onGStreamerInitialized () {
		FirebaseLogger.logInfo("gstreamer play");
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

	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		FirebaseLogger.logDebug("Surface changed to format " + format + " width " + width + " height " + height);
		nativeSurfaceInit (holder.getSurface());
	}

	public void surfaceCreated(SurfaceHolder holder) {
		FirebaseLogger.logDebug("Surface created: " + holder.getSurface());
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		FirebaseLogger.logDebug("Surface destroyed");
		nativeSurfaceFinalize ();
		FirebaseLogger.logDebug("Surface destroyed - done");
	}

	@Override
	protected void onResume(){
		Log.w("petbot", "onResume - petbot JAVA");
		super.onResume();
	}

}
