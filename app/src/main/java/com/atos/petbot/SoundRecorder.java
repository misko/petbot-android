/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.atos.petbot;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.atos.petbot.R;


public class SoundRecorder extends LinearLayout
		implements Button.OnClickListener, Recorder.OnStateChangedListener {

	static final String AUDIO_3GPP = "audio/3gpp";
	static final String AUDIO_AMR = "audio/amr";
	static final String AUDIO_ANY = "audio/*";
	static final String ANY_ANY = "*/*";

	WakeLock mWakeLock;
	String mRequestedType = AUDIO_ANY;
	Recorder mRecorder;
	boolean mSampleInterrupted = false;
	String mErrorUiMessage = null; // Some error messages are displayed in the UI,
	// not a dialog. This happens when a recording
	// is interrupted for some reason.

	long mMaxFileSize = -1;        // can be specified in the intent

	final Handler mHandler = new Handler();
	Runnable mUpdateTimer = new Runnable() {
		public void run() { updateAudioProgress(); }
	};
	ImageButton mRecordButton;
	ImageButton mPlayButton;
	ImageButton mStopButton;

	ProgressBar mStateProgressBar;

	public SoundRecorder(Context context){
		super(context);
		init(context);
	}

	public SoundRecorder(Context context, AttributeSet attrs){
		super(context, attrs);
		init(context);
	}

	public SoundRecorder(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	public void init(Context context){

		inflate(context, R.layout.sound_recorder, this);

		if (AUDIO_ANY.equals(mRequestedType) || ANY_ANY.equals(mRequestedType)) {
			mRequestedType = AUDIO_3GPP;
		}

		PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "SoundRecorder");
		initResourceRefs();

		setRecorder(new Recorder());
	}

	public void setRecorder(Recorder recorder){
		mRecorder = recorder;
		mRecorder.setOnStateChangedListener(this);
		updateUi();
	}

	/*
	 * Whenever the UI is re-created (due f.ex. to orientation change) we have
	 * to reinitialize references to the views.
	 */
	private void initResourceRefs() {
		mRecordButton = (ImageButton) findViewById(R.id.recordButton);
		mPlayButton = (ImageButton) findViewById(R.id.playButton);
		mStopButton = (ImageButton) findViewById(R.id.stopButton);



		mStateProgressBar = (ProgressBar) findViewById(R.id.stateProgressBar);

		mRecordButton.setOnClickListener(this);
		mPlayButton.setOnClickListener(this);
		mStopButton.setOnClickListener(this);
	}

	/*
	 * Make sure we're not recording music playing in the background, ask
	 * the MediaPlaybackService to pause playback.
	 */
	private void stopAudioPlayback() {
		AudioManager am = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
		am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
	}
	/*
	 * Handle the buttons.
	 */
	public void onClick(View button) {
		if (!button.isEnabled())
			return;
		switch (button.getId()) {
			case R.id.recordButton:

				stopAudioPlayback();
				if (AUDIO_AMR.equals(mRequestedType)) {
					mRecorder.startRecording(MediaRecorder.OutputFormat.AMR_NB, ".amr", getContext());
				} else if (AUDIO_3GPP.equals(mRequestedType)) {
					mRecorder.startRecording(MediaRecorder.OutputFormat.THREE_GPP, ".3gpp", getContext());
				} else {
					throw new IllegalArgumentException("Invalid output file type requested");
				}

				break;

			case R.id.playButton:
				mRecorder.startPlayback();
				break;
			case R.id.stopButton:
				mRecorder.stop();
				break;
		}
	}


	/*
	 * If we have just recorded a smaple, this adds it to the media data base
	 * and sets the result to the sample's URI.
	 */
	private void saveSample() {

	}

	/**
	 * Update the big MM:SS timer. If we are in playback, also update the
	 * progress bar.
	 */
	private void updateAudioProgress() {
		int state = mRecorder.state();

		boolean ongoing = state == Recorder.RECORDING_STATE || state == Recorder.PLAYING_STATE;
		long total_time = mRecorder.maxSampleLength; //state == Recorder.RECORDING_STATE ? mRecorder.maxSampleLength : mRecorder.sampleLength();

		if (ongoing) {
			mStateProgressBar.setProgress((int)(100 * mRecorder.progress() / total_time));
			mHandler.postDelayed(mUpdateTimer, 1000);
		} else {
			//mStateProgressBar.setProgress(1);
			//Log.w("petbot","RECORDER" + Integer.toString(mRecorder.mSampleLength) + " " + Long.toString(total_time));
			if (total_time>0) {
				mStateProgressBar.setProgress((int)(100 * mRecorder.mSampleLength / total_time));
			}
			//mHandler.postDelayed(mUpdateTimer, 1000);
		}

	}

	/**
	 * Shows/hides the appropriate child views for the new state.
	 */
	private void updateUi() {
		Log.w("petbot","IN UPDATEUI");

		switch (mRecorder.state()) {

			case Recorder.IDLE_STATE:

				mRecordButton.setEnabled(true);
				mRecordButton.setFocusable(true);
				mStopButton.setEnabled(false);
				mStopButton.setFocusable(false);

				if (mRecorder.sampleLength() == 0) {
					mPlayButton.setEnabled(false);
					mPlayButton.setFocusable(false);
					mRecordButton.requestFocus();
				} else {
					mPlayButton.setEnabled(true);
					mPlayButton.setFocusable(true);
				}

				break;

			case Recorder.RECORDING_STATE:

				mRecordButton.setEnabled(false);
				mRecordButton.setFocusable(false);
				mPlayButton.setEnabled(false);
				mPlayButton.setFocusable(false);
				mStopButton.setEnabled(true);
				mStopButton.setFocusable(true);

				break;

			case Recorder.PLAYING_STATE:

				mRecordButton.setEnabled(true);
				mRecordButton.setFocusable(true);
				mPlayButton.setEnabled(false);
				mPlayButton.setFocusable(false);
				mStopButton.setEnabled(true);
				mStopButton.setFocusable(true);

				break;
		}

		updateAudioProgress();
	}

	/*
	 * Called when Recorder changed it's state.
	 */
	public void onStateChanged(int state) {
		if (state == Recorder.PLAYING_STATE || state == Recorder.RECORDING_STATE) {
			mSampleInterrupted = false;
			mErrorUiMessage = null;
			mWakeLock.acquire(); // we don't want to go to sleep while recording or playing
		} else {
			if (mWakeLock.isHeld())
				mWakeLock.release();
		}

		updateUi();
	}

	/*
	 * Called when MediaPlayer encounters an error.
	 */
	public void onError(int error) {
		Resources res = getResources();

		String message = null;
		switch (error) {

			case Recorder.IN_CALL_RECORD_ERROR:
				// TODO: update error message to reflect that the recording could not be
				//       performed during a call.
			case Recorder.INTERNAL_ERROR:
				message = "internal error";
				break;
		}
		if (message != null) {
			new AlertDialog.Builder(getContext())
					.setTitle(R.string.app_name)
					.setMessage(message)
					.setPositiveButton("OK", null)
					.setCancelable(false)
					.show();
		}
	}

}