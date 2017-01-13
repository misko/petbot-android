package com.atos.petbot;

import android.content.Intent;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.VideoView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.error.VolleyError;
import com.android.volley.request.DownloadRequest;
import com.android.volley.request.StringRequest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SelfieActivity extends Activity {

	Uri media_url;
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

						final VideoView video_view = (VideoView) findViewById(R.id.video_view);
						video_view.setOnPreparedListener(new MediaPlayer.OnPreparedListener(){
							@Override
							public void onPrepared(MediaPlayer player){
								findViewById(R.id.progressBar).setVisibility(View.GONE);
								player.setLooping(true);
								player.start();
							}
						});
						video_view.setVideoPath(video_file.getPath());
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

	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		FloatingActionButton saveButton = (FloatingActionButton) findViewById(R.id.save);
		FloatingActionButton shareButton = (FloatingActionButton) findViewById(R.id.share);

		RelativeLayout.LayoutParams save_layout = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		RelativeLayout.LayoutParams share_layout = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);

		// Checks the orientation of the screen
		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {

			save_layout.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
			save_layout.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);

			share_layout.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
			share_layout.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);

		} else {

			save_layout.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
			save_layout.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);

			share_layout.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
			share_layout.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
		}

		saveButton.setLayoutParams(save_layout);
		shareButton.setLayoutParams(share_layout);
	}

	public void delete(View view) {

		if (video_file != null) {
			video_file.delete();
		}

		StringRequest stringRequest = new StringRequest(Request.Method.GET, rm_url,
				new Response.Listener<String>() {
					@Override
					public void onResponse(String response) {
						showToast("Deleted");
					}
				},
				new Response.ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError error) {
						showToast("Failed");
					}
				}
		);
		((ApplicationState) getApplicationContext()).request_queue.add(stringRequest);

		finish();
	}

	public void share(View view) {
		Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);

		shareIntent.setAction(Intent.ACTION_SEND);
		shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(video_file));
		shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "PetSelfie");
		shareIntent.putExtra(Intent.EXTRA_TEXT, getIntent().getStringExtra("message"));
		shareIntent.setType("video/mp4");

		startActivity(Intent.createChooser(shareIntent, "Share via"));
	}

	public void save(View view){

		try {
			// copy file to movies directory
			File dest_file = new File(selfie_directory, System.currentTimeMillis() + ".mp4");
			copy(video_file, dest_file);

			// scan file to make sure it shows up in gallery
			MediaScannerConnection.scanFile(
				this,
				new String[] {dest_file.getParent()},
				null,
				new MediaScannerConnection.OnScanCompletedListener(){
					@Override
					public void onScanCompleted(String s, Uri uri) {
						Log.i("petbot", "file added to gallery");
					}
				}
			);

			showToast("Saved");

		} catch (Exception exc){
			showToast("Failed");
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

	private void showToast(String message){
		Toast toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
		toast.setGravity(Gravity.CENTER, 0, 0);
		toast.show();
	}

}
