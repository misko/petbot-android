package com.petbot;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup.LayoutParams;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;


import java.util.Arrays;

import org.freedesktop.gstreamer.GStreamer;
import com.petbot.PBConnector;

import com.petbot.R;

public class PetBot extends Activity implements SurfaceHolder.Callback {
	private native void nativeInit();     // Initialize native code, build pipeline, etc
	private native void nativePlayAgent(long jagent, int jstream_id);     // Initialize native code, build pipeline, etc
	private native void nativeFinalize(); // Destroy pipeline and shutdown native code
	private native void nativePlay();     // Set pipeline to PLAYING
	private native void nativePause();    // Set pipeline to PAUSED
	private static native boolean nativeClassInit(); // Initialize native class: cache Method IDs for callbacks
	private native void nativeSurfaceInit(Object surface);
	private native void nativeSurfaceFinalize();
	private long native_custom_data;      // Native code will use this to keep private data


	// Called when the activity is first created.
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		//PBConnector pb = new PBConnector();
		//pb.stringFromJNI();
		//byte[] wtf=  pb.newByteArray();
		Log.w("petbot", "no network");
		final PBConnector pb = new PBConnector("159.203.252.147",8888,"A20PETBOTX1");

		//pb.initGlib(); //setup the context and launch main run loop

		//start up a read thread
		Thread read_thread = new Thread() {
			@Override
			public void run() {
				while (true) {
					final PBMsg m = pb.readPBMsg();
					if ((m.pbmsg_type ^  (PBMsg.PBMSG_SUCCESS | PBMsg.PBMSG_RESPONSE | PBMsg.PBMSG_ICE | PBMsg.PBMSG_CLIENT | PBMsg.PBMSG_STRING))==0) {
						Log.w("petbot", "READ" + m.toString() +" MOVE TO ICE NEGOTIATE!");		//start up a read thread
						Thread negotiate_thread = new Thread() {
							@Override
							public void run() {
								Log.w("petbot", "ANDROID - NEGOTIATE  ");
								pb.iceNegotiate(m);
								Log.w("petbot", "ANDROID - NEGOTIATE DONE");
								nativePlayAgent(pb.ptr_agent,pb.stream_id);
							}
						};
						negotiate_thread.start();
					} else {
						Log.w("petbot", "READ" + m.toString());
					}
				}
			}
		};
		read_thread.start();

		pb.startNiceThread(0);

		//start up a read thread
		Thread request_thread = new Thread() {
			@Override
			public void run() {
				Log.w("petbot", "ANDROID - ICE REQUEST ");
				pb.iceRequest();
				Log.w("petbot", "ANDROID - ICE REQUEST DONE");
			}
		};
		request_thread.start();

		Log.w("petbot", String.valueOf(pb.ptr_pbs));
		//pb.connectToServerWithKey(JNIEnv* env,jobject thiz, jstring hostname, int portno, jstring key );
		//System.out.println(wtf);

		// Initialize GStreamer and warn if it fails
		try {
			GStreamer.init(this);
		} catch (Exception e) {
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
			finish();
			return;
		}

		setContentView(R.layout.main);


		Button cookieButton = (Button) this.findViewById(R.id.cookieButton);
		cookieButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				pb.sendCookie();
			}
		});

		Button soundButton = (Button) this.findViewById(R.id.soundButton);
		cookieButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				pb.sendCookie();
			}
		});

		/*ImageButton play = (ImageButton) this.findViewById(R.id.button_play);
		play.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				is_playing_desired = true;
				nativePlay();
			}
		});

		ImageButton pause = (ImageButton) this.findViewById(R.id.button_stop);
		pause.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				is_playing_desired = false;
				nativePause();
			}
		});*/

		SurfaceView sv = (SurfaceView) this.findViewById(R.id.surface_video);
		SurfaceHolder sh = sv.getHolder();
		sh.addCallback(this);

		/*if (savedInstanceState != null) {
			is_playing_desired = savedInstanceState.getBoolean("playing");
			Log.i ("GStreamer", "Activity created. Saved state is playing:" + is_playing_desired);
		} else {
			is_playing_desired = false;
			Log.i ("GStreamer", "Activity created. There is no saved state, playing: false");
		}*/

		// Start with disabled buttons, until native code is initialized
		//this.findViewById(R.id.button_play).setEnabled(false);
		//this.findViewById(R.id.button_stop).setEnabled(false);

		nativeInit();
	}

	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		// Checks the orientation of the screen
		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			//Toast.makeText(this, "landscape", Toast.LENGTH_SHORT).show();
			// for example the width of a layout
			int width = 300;
			int height = LayoutParams.WRAP_CONTENT;
			//SurfaceView sv = (SurfaceView) this.findViewById(R.id.surface_video);
			//sv.setLayoutParams(new LayoutParams(width, height));
			//childLayout.setLayoutParams(new LayoutParams(width, height));
		} else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
			//Toast.makeText(this, "portrait", Toast.LENGTH_SHORT).show();
		}
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
		System.loadLibrary("crypto");
		System.loadLibrary("ssl");
		System.loadLibrary("gstreamer_android");
		System.loadLibrary("tutorial-3");
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
	}

}
