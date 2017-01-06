package com.petbot;

import android.app.DownloadManager;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.MediaController;
import android.widget.ShareActionProvider;
import android.widget.VideoView;

import java.io.File;

public class SelfieActivity extends Activity {

	Uri media_url;
	ShareActionProvider shareActionProvider;
	File video_file;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_selfie);

		media_url = Uri.parse(getIntent().getStringExtra("media_url"));
		final String rm_url = getIntent().getStringExtra("rm_url");

		try {
			video_file = File.createTempFile("selfie", "mp4");
			DownloadManager downloader = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
			DownloadManager.Request request = new DownloadManager.Request(media_url);
			request.setDestinationUri(Uri.fromFile(video_file));
			long enqueue = downloader.enqueue(request);

			setShareIntent(Uri.fromFile(video_file));
		} catch (Exception exc){

		}


		VideoView video_view = (VideoView) findViewById(R.id.video_view);
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

		video_view.setVideoURI(media_url);
	}

	public void save(View view){
		DownloadManager downloader = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
		DownloadManager.Request request = new DownloadManager.Request(media_url);
		request.setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, media_url.getLastPathSegment());
		long enqueue = downloader.enqueue(request);
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
		}
	}
}
