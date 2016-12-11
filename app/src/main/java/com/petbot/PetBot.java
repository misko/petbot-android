package com.petbot;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup.LayoutParams;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;

import org.freedesktop.gstreamer.GStreamer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.petbot.PBConnector;
import com.petbot.QRViewer;

import com.petbot.R;

public class PetBot extends Activity implements SurfaceHolder.Callback {

	private native void nativeInit();     // Initialize native code, build pipeline, etc
	private native void nativePlayAgent(int port);     // Initialize native code, build pipeline, etc
	private native void nativeFinalize(); // Destroy pipeline and shutdown native code
	private native void nativePlay();     // Set pipeline to PLAYING
	private native void nativePause();    // Set pipeline to PAUSED
	private static native boolean nativeClassInit(); // Initialize native class: cache Method IDs for callbacks
	private native void nativeSurfaceInit(Object surface);
	private native void nativeSurfaceFinalize();
	private long native_custom_data;      // Native code will use this to keep private data

	private String petbot_secret;

	// Called when the activity is first created.
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		//start up a read thread for UDP streaming
		Thread play_thread = new Thread() {
			@Override
			public void run() {
				final int port = ((ApplicationState) getApplicationContext()).port;
				Log.w("petbot", " START STREAM WITH UDPSRC PORT: " + Integer.toString(port));        //start up a read thread
				nativePlayAgent(port);
			}
		};

		// Initialize GStreamer and warn if it fails
		try {
			GStreamer.init(this);
		} catch (Exception e) {
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
			finish();
			return;
		}

		setContentView(R.layout.main);


		final ApplicationState state = (ApplicationState) getApplicationContext();
		Thread server_thread = new Thread() {
			@Override
			public void run() {
				try {
					int version_test = 1;
					int ir_test = 2;
					int led_test = 3;
					int motor_test =4;
					int wifi_test =5;
					int microphone_test =6;
					int streaming_test =7;
					final int test_strings = 8;



					final String tests[] = new String[test_strings];
					final String test_names[] = new String[test_strings];
					final String test_status[] = new String[test_strings];

					test_names[0]="TESTING IN PROGRESS...";
					test_status[0]="";
					test_names[1]="VERSION";
					test_status[1]="?";
					test_names[2]="IR TEST";
					test_status[2]="?";
					test_names[3]="LED TEST";
					test_status[3]="?";
					test_names[4]="motor and IR sensor TEST";
					test_status[4]="?";
					test_names[5]="WIFI TEST";
					test_status[5]="?";
					test_names[6]="microphone TEST";
					test_status[6]="?";
					test_names[7]="streaming TEST";
					test_status[7]="?";

					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							for (int i=0; i<test_strings; i++){
								tests[i]=test_status[i]+ "\t"+test_names[i];
							}
							ListView listView   = (ListView) findViewById(R.id.testListView);
							ArrayAdapter<String> adapter = new ArrayAdapter<String>(PetBot.this,android.R.layout.simple_list_item_1, android.R.id.text1, tests);
							listView.setAdapter(adapter);
						}
					});


					ServerSocket ss = new ServerSocket(state.port);
					ss.setReuseAddress(true);
					//Server is waiting for client here, if needed
					Log.e("petbot","waiting for petbot to connect");
					Socket s = ss.accept();
					try {
						BufferedReader in = new BufferedReader(
								new InputStreamReader(
										s.getInputStream()));
						String line = null;
						while ((line = in.readLine()) != null) {
							Log.d("ServerActivity", line);
							String[] parts = line.split(" ");
							if (parts[0].equalsIgnoreCase("TEST")) {
								String status = parts[2];
								int index=0;
								if (parts[1].equalsIgnoreCase("LED")) {
									test_status[led_test]=status;
								} else if (parts[1].equalsIgnoreCase("MOTOR")) {
									test_status[motor_test]=status;
								} else if (parts[1].equalsIgnoreCase("IR")) {
									test_status[ir_test]=status;
								} else if (parts[1].equalsIgnoreCase("WIFI")) {
									test_status[wifi_test]=status;
								} else if (parts[1].equalsIgnoreCase("MICROPHONE")) {
									test_status[microphone_test]=status;
								} else if (parts[1].equalsIgnoreCase("STREAMING")) {
									test_status[streaming_test]=status;
								} else if (parts[1].equalsIgnoreCase("VERSION")) {
									test_status[version_test]=status;
								}
							}
							//update UI
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									for (int i=0; i<test_strings; i++){
										tests[i]=test_status[i]+ "\t\t"+test_names[i];
									}
									ListView listView   = (ListView) findViewById(R.id.testListView);
									ArrayAdapter<String> adapter = new ArrayAdapter<String>(PetBot.this,android.R.layout.simple_list_item_1, android.R.id.text1, tests);
									listView.setAdapter(adapter);
								}
							});
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					s.close();
					ss.close();

					Log.e("petbot","petbot connected");
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							finish();
							//Intent open_main = new Intent(PetBot.this,QRViewer.class);
							//open_main.putExtra("image_url", getIntent().getExtras().getString("image_url"));
							//PetBot.this.startActivity(open_main);
						}
					});

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




		SurfaceView sv = (SurfaceView) this.findViewById(R.id.surface_video);
		SurfaceHolder sh = sv.getHolder();
		sh.addCallback(this);

		nativeInit();
		play_thread.start();
	}

	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		// Checks the orientation of the screen
		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			int width = 300;
			int height = LayoutParams.WRAP_CONTENT;
		} else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
			//Toast.makeText(this, "portrait", Toast.LENGTH_SHORT).show();
		}
	}

	protected void onSaveInstanceState (Bundle outState) {
		Log.d ("GStreamer", "Saving state, playing:" );
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

		nativePlay();

		// Re-enable buttons, now that GStreamer is initialized
		final Activity activity = this;
		runOnUiThread(new Runnable() {
			public void run() {
				//activity.findViewById(R.id.button_play).setEnabled(true);
				//activity.findViewById(R.id.button_stop).setEnabled(true);
			}
		});
	}

	static {
		System.loadLibrary("gstreamer_android");
		System.loadLibrary("tutorial-3");
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
	}

}
