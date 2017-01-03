package com.petbot;

import android.content.Context;
import android.preference.Preference;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import static com.petbot.Recorder.RECORDING_STATE;

public class SoundRecorderPreference extends Preference {

	Recorder recorder = new Recorder();

	public SoundRecorderPreference(Context context) {
		this(context, null, 0);
	}

	public SoundRecorderPreference(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public SoundRecorderPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setLayoutResource(R.layout.preference_soundrecorder);
	}

	@Override
	protected void onBindView(final View view) {
		super.onBindView(view);

		SoundRecorder sound_recorder = (SoundRecorder) view.findViewById(R.id.sound_recorder);
		sound_recorder.setRecorder(recorder);

		final ApplicationState application = (ApplicationState) getContext().getApplicationContext();

		final EditText filename = (EditText) view.findViewById(R.id.filename);
		Button upload_button = (Button) view.findViewById(R.id.upload);
		upload_button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {

				Toast toast = null;
				if(recorder.state() == RECORDING_STATE){
					toast = Toast.makeText(getContext(), "Sound not finished recording.", Toast.LENGTH_SHORT);
				} else if (recorder.sampleLength() == 0){
					toast = Toast.makeText(getContext(), "No sound recorded.", Toast.LENGTH_SHORT);
				} else if (TextUtils.isEmpty(filename.getText().toString())){
					toast = Toast.makeText(getContext(), "Sound name can not be blank.", Toast.LENGTH_SHORT) ;
				} else {
					StringRequest upload_request = new StringRequest(
						Request.Method.POST,
						application.upload_address + application.server_secret,
						new Response.Listener<String>() {
							@Override
							public void onResponse(String response) {

							}
						},
						new Response.ErrorListener() {
							@Override
							public void onErrorResponse(VolleyError error) {

							}
						});
				}

				if(toast != null){
					toast.setGravity(Gravity.CENTER, 0, 0);
					toast.show();
				}
			}
		});
	}
}
