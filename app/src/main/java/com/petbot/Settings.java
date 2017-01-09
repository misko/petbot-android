package com.petbot;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.error.VolleyError;
import com.android.volley.request.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class Settings extends PreferenceFragment implements SoundRecorderPreference.OnSoundUploadedListener {

	PBConnector pb;
	HashMap<String,String> settings = new HashMap<>();
	boolean settings_retrieved = false;

	@Override
	public void onDestroy() {
		Log.w("asdfasdf", "ANDROID - DESTROY SETTINGS");
		super.onDestroy();

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.fragment_settings);

		ApplicationState application = (ApplicationState) this.getActivity().getApplicationContext();

		JSONObject sounds_info = new JSONObject();
		try {
			sounds_info.put("file_type", "mp3");
			sounds_info.put("start_idx", 0);
			sounds_info.put("end_idx", 0); //TODO: start_idx and end_idx are not used in server
		} catch (JSONException error) {
			//TODO
		}

		SoundRecorderPreference recorder = (SoundRecorderPreference) findPreference("recorder");
		recorder.setOnSoundUploadedListener(this);

		JsonObjectRequest sounds_request = new JsonObjectRequest(
				Request.Method.POST,
				application.HTTPS_ADDRESS_PB_LS + application.server_secret,
				sounds_info,
				new Response.Listener<JSONObject>() {
					@Override
					public void onResponse(JSONObject response) {

						Log.e("petbot", response.toString());
						boolean success = false;
						try {

							success = response.getInt("status") == 1;
							if (success) {

								JSONArray sound_files = response.getJSONArray("files");
								Log.e("asdfasdf", response.toString());
								CharSequence[] sound_names = new CharSequence[sound_files.length()];
								CharSequence[] sound_IDs = new CharSequence[sound_files.length()];

								for(int index = 0; index < sound_files.length(); index++){
									sound_names[index] = sound_files.getJSONArray(index).getString(1);
									sound_IDs[index] = sound_files.getJSONArray(index).getString(0);
								}

								ListPreference alert_sounds = (ListPreference) findPreference("alert_sounds");
								alert_sounds.setEntries(sound_names);
								alert_sounds.setEntryValues(sound_IDs);

								ListPreference selfie_sounds = (ListPreference) findPreference("selfie_sounds");
								selfie_sounds.setEntries(sound_names);
								selfie_sounds.setEntryValues(sound_IDs);
							}
						} catch (JSONException error) {
							//TODO
							Log.e("petbot", error.toString());
						}

					}
				},
				new Response.ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError error) {
						Log.e("petbot", error.toString());
					}
				}
		);

		application.request_queue.add(sounds_request);
		startMessageThread();
	}



	public void onSoundUploaded(String name, String fileID){

		ListPreference alert_sounds = (ListPreference) findPreference("alert_sounds");
		ListPreference selfie_sounds = (ListPreference) findPreference("selfie_sounds");
		int size = alert_sounds.getEntries().length;

		CharSequence[] sound_names = new CharSequence[size + 1];
		System.arraycopy(alert_sounds.getEntries(), 0, sound_names, 0, size);
		sound_names[sound_names.length - 1] = name;

		CharSequence[] fileIDs = new CharSequence[size + 1];
		System.arraycopy(alert_sounds.getEntryValues(), 0, fileIDs, 0, size);
		fileIDs[fileIDs.length - 1] = fileID;

		alert_sounds.setEntries(sound_names);
		alert_sounds.setEntryValues(fileIDs);

		selfie_sounds.setEntries(sound_names);
		selfie_sounds.setEntryValues(fileIDs);
	}

	void startMessageThread(){

		ApplicationState state = (ApplicationState) this.getActivity().getApplicationContext();
		pb = new PBConnector(state.server, state.port, state.server_secret);

		//start up a read thread
		final Thread read_thread = new Thread() {
			@Override
			public void run() {

				int response_mask = PBMsg.PBMSG_SUCCESS | PBMsg.PBMSG_RESPONSE | PBMsg.PBMSG_STRING;

				while (true) {

					Log.w("asdfasdf", "ANDROID - READ MSG");
					final PBMsg m = pb.readPBMsg();
					Log.w("asdfasdf", "ANDROID - READ MSG -DONE");

					if (m == null) {
						Log.w("asdfasdf", "ANDROID - LEAVE READ THREAD");
						break;
					} else if ((m.pbmsg_type ^ (response_mask | PBMsg.PBMSG_CONFIG_GET))==0) {

						getActivity().runOnUiThread(new Runnable() {
							@Override
							public void run() {
								parseSettings(new String(m.pbmsg));
							}
						});

						Log.w("asdfasdf", "received settings " + m.toString());
						break;


					} else {
						Log.w("asdfasdf", "READ" + m.toString());
					}
				}
			}
		};
		read_thread.setDaemon(true);
		read_thread.start();
		pb.getSettings();
	}

	void parseSettings(String settings_string){

		settings_retrieved = true;

		String[] settings_list = settings_string.split("\n");
		for(String pair : settings_list){
			Log.e("asdfasdf pair", pair);
			String[] setting = pair.split("\t");
			Log.e("asdfasdf setting", setting[0]);
			if(setting.length == 2) {
				settings.put(setting[0], setting[1]);
			}
		}

		Preference version = findPreference("VERSION");
		version.setSummary(settings.get("VERSION"));

		NumberPickerPreference timeout = (NumberPickerPreference) findPreference("selfie_timeout");
		timeout.setValue(Integer.parseInt(settings.get("selfie_timeout")) / 3600);

		NumberPickerPreference length = (NumberPickerPreference) findPreference("selfie_length");
		length.setValue(Integer.parseInt(settings.get("selfie_length")));

		SeekBarPreference volume = (SeekBarPreference) findPreference("master_volume");
		volume.setValue(Integer.parseInt(settings.get("master_volume")));
	}

	void saveSettings(){

		Thread save_thread = new Thread() {
			@Override
			public void run() {
				NumberPickerPreference timeout = (NumberPickerPreference) findPreference("selfie_timeout");
				pb.set("selfie_timeout", Integer.toString(timeout.getValue() * 3600));

				NumberPickerPreference length = (NumberPickerPreference) findPreference("selfie_length");
				pb.set("selfie_length", Integer.toString(length.getValue()));

				SeekBarPreference volume = (SeekBarPreference) findPreference("master_volume");
				pb.set("master_volume", Integer.toString(volume.getValue()));
				Log.e("asdfasdf","yoyoyoyoyo");
			}
		};
		Log.e("asdfasdf", "yayayayaya");
		save_thread.start();
		Log.e("asdfasdf","ffffffff");
	}

	@Override
	public void onStop(){
		Log.e("asdfasdf", "here");
		if(settings_retrieved){
			saveSettings();
		}
		Log.e("asdfasdf", "pppppp");
		super.onStop();
	}
}
