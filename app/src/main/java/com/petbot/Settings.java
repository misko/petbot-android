package com.petbot;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.view.View;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class Settings extends PreferenceFragment {

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

		JsonObjectRequest sounds_request = new JsonObjectRequest(
				Request.Method.POST,
				application.sound_list_address + application.server_secret,
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
	}

}
