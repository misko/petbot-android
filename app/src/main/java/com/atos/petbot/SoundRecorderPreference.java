package com.atos.petbot;

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
import com.android.volley.error.VolleyError;
import com.android.volley.request.SimpleMultiPartRequest;
import com.petbot.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

import static com.atos.petbot.Recorder.RECORDING_STATE;

public class SoundRecorderPreference extends Preference {

	public interface OnSoundUploadedListener {
		void onSoundUploaded(String name, String fileID);
	}
	private OnSoundUploadedListener onSoundUploadedListener = null;
	public void setOnSoundUploadedListener(OnSoundUploadedListener listener){
		onSoundUploadedListener = listener;
	}

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

					// rename file to user entered name
					final String sound_name = filename.getText().toString();
					File sound_file = new File(recorder.sampleFile().getParent(), sound_name + ".3gpp");
					boolean foo = recorder.sampleFile().renameTo(sound_file);
					Log.e("asdfasdf", "file rename success; " + foo);

					SimpleMultiPartRequest upload_request = new SimpleMultiPartRequest(
							Request.Method.POST,
							application.upload_address + application.server_secret,
							new Response.Listener<String>() {
								@Override
								public void onResponse(String json_string) {

									try {
										JSONObject response = new JSONObject(json_string);
										boolean success = response.getInt("status") == 1;

										if(success) {
											String fileID = response.getString("fileid");
											if(onSoundUploadedListener != null){
												onSoundUploadedListener.onSoundUploaded(sound_name, fileID);
											}
										}

									} catch (JSONException exc){
										exc.printStackTrace();
									}

									Toast toast = Toast.makeText(getContext(), "Sound uploaded.", Toast.LENGTH_SHORT);
									toast.setGravity(Gravity.CENTER, 0, 0);
									toast.show();
								}
							},
							new Response.ErrorListener() {
								@Override
								public void onErrorResponse(VolleyError error) {
									Log.e("asdfasdf", error.toString());
								}
							}
					);

					Log.e("asdfasdf", sound_file.getPath());
					upload_request.addFile("file", sound_file.getPath());
					application.request_queue.add(upload_request);
				}

				if(toast != null){
					toast.setGravity(Gravity.CENTER, 0, 0);
					toast.show();
				}
			}
		});
	}
}
