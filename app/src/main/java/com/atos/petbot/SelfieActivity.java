package com.atos.petbot;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.app.DownloadManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ShareActionProvider;
import android.widget.VideoView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.error.VolleyError;
import com.android.volley.request.StringRequest;
import com.android.volley.toolbox.Volley;
import com.atos.petbot.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SelfieActivity extends Activity {

	Uri media_url;
	ShareActionProvider shareActionProvider;
	File video_file;
	String rm_url ;


	/*public class DownloadFile extends AsyncTask<String, Integer, String> {
		String videoToDownload = "http://r2---sn-u2oxu-f5f6.googlevideo.com/videoplayback?expire=1438261718&fexp=901816,9405637,9407538,9407942,9408513,9408710,9409172,9413020,9414764,9414856,9414935,9415365,9415485,9416126,9416355,9417009,9417719,9418201,9418204&id=d813f7f3bef428da&mn=sn-u2oxu-f5f6&mm=31&mime=video/mp4&upn=82UaibRK7EM&itag=18&pl=24&dur=148.189&ip=167.114.5.145&key=yt5&ms=au&mt=1438239687&mv=u&source=youtube&ipbits=0&pcm2cms=yes&sparams=dur,id,ip,ipbits,itag,lmt,mime,mm,mn,ms,mv,pcm2cms,pl,ratebypass,source,upn,expire&lmt=1428049239028653&signature=39087CBD9BDC9EBD612CA0E8E82AC692B427FFE3.18C23CD0AEC8410CFBE4F35F532199DFF21E7DFA&ratebypass=yes&sver=3&signature=&title=How+To+Train+Your+Dragon+2+Official+Trailer+%231+%282014%29+-+Animation+Sequel+HD&filename=How_To_Train_Your_Dragon_2_Official_Trailer_1_2014__Animation_Sequel_HD.mp4";

		@Override
		protected String doInBackground(String... params) {
			int count;

			try {
				mp4load(videoToDownload);
			} catch (Exception e) {
				// TODO: handle exception
			}

			return null;
		}

		public void mp4load(String urling) {
			try {
				URL url = new URL(urling);
				HttpURLConnection con = (HttpURLConnection) url.openConnection();
				con.setRequestMethod("GET");
				//c.setDoOutput(true);
				con.connect();

				String downloadsPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();

				String fileName = "test.mp4";

				File outputFile = new File(downloadsPath, fileName);

				if (!outputFile.exists()) {
					outputFile.createNewFile();
				}

				FileOutputStream fos = new FileOutputStream(outputFile);

				int status = con.getResponseCode();

				InputStream is = con.getInputStream();

				byte[] buffer = new byte[1024];
				int len1 = 0;
				while ((len1 = is.read(buffer)) != -1) {
					fos.write(buffer, 0, len1);
				}
				fos.close();
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}*/

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		Log.w("petbot", "ANDROID - CREATE SELFIE!");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_selfie);

		media_url = Uri.parse(getIntent().getStringExtra("media_url"));
		rm_url = getIntent().getStringExtra("rm_url");


		try {
			Log.w("petbot", "ANDROID - TRY CREATE SELFIE!");
			//video_file = File.createTempFile("selfie", "mp4");
			Log.w("petbot", "ANDROID - TRY CREATE SELFIE!");
			DownloadManager downloader = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
			Log.w("petbot", "ANDROID - TRY CREATE SELFIE!");
			DownloadManager.Request request = new DownloadManager.Request(media_url);
			Log.w("petbot", "ANDROID - TRY CREATE SELFIE! xxxxxxxxx");
			//request.setDestinationUri(Uri.fromFile(video_file));
			//request.setDestinationUri(null);
			//request.setDestinationInExternalFilesDir(SelfieActivity.this, Environment.DIRECTORY_DOWNLOADS,"selfie.mp4");
			request.setTitle("SELFIE");
			request.setDescription("This is a selfie.");

			//set in movies directory
			File path = Environment.getExternalStoragePublicDirectory(
					Environment.DIRECTORY_MOVIES);
			path = new File(path,"petbot-selfies");
			//path = this.getFilesDir();
			//path = getExternalFilesDir(null);

			path.mkdirs();
			video_file = new File(path, "latest.mp4");
			if (video_file.exists()) {
				video_file.delete();
			}
			request.setDestinationUri(Uri.fromFile(video_file));

			//request.setDestinationInExternalPublicDir(Environment.DIRECTORY_MOVIES,"selfie.mp4");

			/*
			Log.w("petbot", "ANDROID - TRY CREATE SELFIE! xxxxxxxxx");
			String downloadPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
			String filename = "selfie.mp4";
			//Log.w("petbot", "ANDROID - TRY CREATE SELFIE! xxxxxxxxx" + downloadPath + " " + filename);
			//video_file = new File(downloadPath, filename);
			video_file = new File(this.getFilesDir(), filename);*/
			Log.w("petbot", "ANDROID - TRY CREATE SELFIE!");
			long enqueue = downloader.enqueue(request);

			Log.w("petbot", "ANDROID - CREATE SELFIE x2");
			Log.w("petbot", "ANDROID - CREATE SELFIE x3");
		} catch (Exception exc){
			Log.w("petbot", Log.getStackTraceString(exc));
		}

		BroadcastReceiver onComplete=new BroadcastReceiver() {
			public void onReceive(Context ctxt, Intent intent) {
				// your code

				Log.w("petbot", "ANDROID - CREATE SELFIE x555 - download complete");
				VideoView video_view = (VideoView) findViewById(R.id.video_view);
				video_view.setOnPreparedListener(new MediaPlayer.OnPreparedListener(){
					@Override
					public void onPrepared(MediaPlayer player){
						player.setLooping(true);
						player.start();
					}
				});
				video_view.setVideoPath(video_file.getPath());


				setShareIntent(Uri.fromFile(video_file));
			}
		};

		registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));


		/*VideoView video_view = (VideoView) findViewById(R.id.video_view);
		MediaController media_controller = new MediaController(this);

		media_controller.setAnchorView(video_view);
		media_controller.setMediaPlayer(video_view);
		video_view.setMediaController(media_controller);

		video_view.setOnPreparedListener(new MediaPlayer.OnPreparedListener(){
			@Override
			public void onPrepared(MediaPlayer player){
				player.start();
			}
		});

		Log.w("petbot", "ANDROID - CREATE SELFIE x4");
		video_view.setVideoURI(media_url);*/
	}

	public void copy(File src, File dst) throws IOException {
		InputStream in = new FileInputStream(src);
		OutputStream out = new FileOutputStream(dst);

		// Transfer bytes from in to out
		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		in.close();
		out.close();
	}

	public void delete(View view) {
		if (video_file.exists()) {
			video_file.delete();
		}
		//make api call to remove from server
		// Instantiate the RequestQueue.
		RequestQueue queue = Volley.newRequestQueue(this);
		String url ="http://www.google.com";


		StringRequest stringRequest = new StringRequest(Request.Method.GET, rm_url,
				new Response.Listener<String>() {
					@Override
					public void onResponse(String response) {
						// Display the first 500 characters of the response string.
					}
				}, new Response.ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError error) {
			}
		});

		queue.add(stringRequest);

		finish();

	}

	public void share(View view) {
		Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);

		sharingIntent.setAction(Intent.ACTION_SEND);
		sharingIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(video_file));
		sharingIntent.setType("video/mp4");

		String shareBody = "Here is the share content body";
		sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Subject Here");
		sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
		startActivity(Intent.createChooser(sharingIntent, "Share via"));
	}

	public void save(View view){
		//set in movies directory
		File path = Environment.getExternalStoragePublicDirectory(
				Environment.DIRECTORY_MOVIES);
		path = new File(path,"petbot-selfies");
		path.mkdirs();
		File dest_file = new File(path, "selfie.mp4");
		try {
			copy(video_file,dest_file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		Log.w("petbot", "ANDROID - CREATE OPTIONS MENU");
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.selfie, menu);

		// Locate MenuItem with ShareActionProvider
		MenuItem item = menu.findItem(R.id.menu_item_share);
		shareActionProvider = (ShareActionProvider) item.getActionProvider();



		return true;
	}

	private void setShareIntent(Uri video) {

		Intent shareIntent = new Intent();
		shareIntent.setAction(Intent.ACTION_SEND);
		shareIntent.putExtra(Intent.EXTRA_STREAM, video);
		shareIntent.setType("video/mp4");

		if (shareActionProvider != null) {

			Log.w("petbot", "ANDROID - SHARE ACTION WAS NOT NULL!");
			shareActionProvider.setShareIntent(shareIntent);
		} else {

			Log.w("petbot", "ANDROID - SHARE ACTION WAS NULL!");
		}
	}
}
