package com.petbot;

import android.app.DownloadManager;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.MediaController;
import android.widget.VideoView;

public class SelfieActivity extends Activity {

	Uri media_url;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_selfie);

		media_url = Uri.parse(getIntent().getStringExtra("media_url"));
		final String rm_url = getIntent().getStringExtra("rm_url");

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
}
