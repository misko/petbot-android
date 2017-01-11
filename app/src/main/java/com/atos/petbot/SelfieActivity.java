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
import android.widget.ProgressBar;
import android.widget.ShareActionProvider;
import android.widget.VideoView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.error.VolleyError;
import com.android.volley.request.DownloadRequest;
import com.android.volley.request.StringRequest;
import com.android.volley.toolbox.Volley;

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

	//set in movies directory
	File selfie_directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "petbot-selfies");

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_selfie);
		selfie_directory.mkdirs();

		media_url = Uri.parse(getIntent().getStringExtra("media_url"));
		rm_url = getIntent().getStringExtra("rm_url");

		try {
			video_file = File.createTempFile("selfie", "mp4");
			DownloadRequest downloader = new DownloadRequest(
					media_url.toString(),
					video_file.getPath(),
					new Response.Listener<String>() {
						@Override
						public void onResponse(String response) {

							VideoView video_view = (VideoView) findViewById(R.id.video_view);
							video_view.setOnPreparedListener(new MediaPlayer.OnPreparedListener(){
								@Override
								public void onPrepared(MediaPlayer player){
									findViewById(R.id.progressBar).setVisibility(View.GONE);
									player.setLooping(true);
									player.start();
								}
							});
							video_view.setVideoPath(video_file.getPath());

							setShareIntent(Uri.fromFile(video_file));
						}
					},
					new Response.ErrorListener() {
						@Override
						public void onErrorResponse(VolleyError error) {
							Log.e("asdfasdf", "selfie downloader on error");
						}
					}
			);

			((ApplicationState) getApplicationContext()).request_queue.add(downloader);

		} catch (Exception exc){

		}
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

		if (video_file != null) {
			video_file.delete();
		}

		StringRequest stringRequest = new StringRequest(Request.Method.GET, rm_url,
				new Response.Listener<String>() {
					@Override
					public void onResponse(String response) {
						// Display the first 500 characters of the response string.
					}
				},
				new Response.ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError error) {
					}
				}
		);
		((ApplicationState) getApplicationContext()).request_queue.add(stringRequest);

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
		File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
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
			shareActionProvider.setShareIntent(shareIntent);
		} else {
			Log.w("petbot", "ANDROID - SHARE ACTION WAS NULL!");
		}
	}
}
