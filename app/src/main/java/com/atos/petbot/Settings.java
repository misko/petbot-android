package com.atos.petbot;

import android.app.Application;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.util.Log;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.error.VolleyError;
import com.android.volley.request.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class Settings extends PreferenceFragment implements SoundRecorderPreference.OnSoundUploadedListener {

	SeekBarPreference volume_preference;
	UpdatePreference update_preference;
	SeekBarPreference selfie_sensitivity_slider;
	SeekBarPreference motion_sensitivity_slider;
	SoundRecorderPreference recorder;
	NumberPickerPreference selfie_timeout;
	NumberPickerPreference selfie_length;
	Preference reboot_preference;


	PBConnector pb;
	boolean settings_retrieved = false;

	@Override
	public void onDestroy() {
		Log.w("asdfasdf", "ANDROID - DESTROY SETTINGS");
		super.onDestroy();
	}

	private void setup() {
		ApplicationState application = (ApplicationState) this.getActivity().getApplicationContext();



		update_preference = (UpdatePreference) findPreference("update_preference");
		update_preference.setEnabled(!application.updateable.isEmpty());

		reboot_preference = (Preference) findPreference("reboot_preference");
		reboot_preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				pb.reboot();
				return false;
			}
		});

		volume_preference = (SeekBarPreference) findPreference("master_volume");
		volume_preference.setEnabled(false);
		volume_preference.setMax(63);
		volume_preference.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				pb.set("master_volume", Integer.toString(seekBar.getProgress()));

			}
		});

		selfie_sensitivity_slider = (SeekBarPreference) findPreference("selfie_sensitivity_slider");
		selfie_sensitivity_slider.setEnabled(false);
		selfie_sensitivity_slider.setMax(100);
		selfie_sensitivity_slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				float value = ((float)seekBar.getProgress())/100;
				pb.set("selfie_pet_sensitivity", Float.toString(value));
			}
		});

		motion_sensitivity_slider = (SeekBarPreference) findPreference("motion_sensitivity_slider");
		motion_sensitivity_slider.setEnabled(false);
		motion_sensitivity_slider.setMax(100);
		motion_sensitivity_slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {

				float value = ((float)seekBar.getProgress())/100;
				pb.set("selfie_mot_sensitivity", Float.toString(value));
			}
		});

		recorder = (SoundRecorderPreference) findPreference("recorder");
		recorder.setOnSoundUploadedListener(this);

		selfie_timeout = (NumberPickerPreference) findPreference("selfie_timeout");
		selfie_timeout.setEnabled(false);
		selfie_timeout.setOnPreferenceChangeListener(
				new Preference.OnPreferenceChangeListener() {
					@Override
					public boolean onPreferenceChange(Preference preference,Object timeout) {
						Log.w("petbot","setting selfie_timeout to " + Integer.toString((Integer)timeout * 3600));
						pb.set("selfie_timeout", Integer.toString((Integer)timeout * 3600));
						return true;
					}
				});

		selfie_length = (NumberPickerPreference) findPreference("selfie_length");
		selfie_length.setEnabled(false);
		selfie_length.setOnPreferenceChangeListener(
				new Preference.OnPreferenceChangeListener() {
					@Override
					public boolean onPreferenceChange(Preference preference, Object length) {
						Log.w("petbot","setting selfie_length to " + Integer.toString((Integer)length));
						pb.set("selfie_length", Integer.toString((Integer)length));
						return true;
					}
				}
		);

		findPreference("LED").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object checked) {
				pb.set("pb_led_enable", (Boolean) checked ? "1" : "0");
				return true;
			}
		});
		findPreference("pb_selfie_enable").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object checked) {
				pb.set("pb_selfie_enable", (Boolean) checked ? "1" : "0");
				return true;
			}
		});
	}

	@Override
	public void onResume() {
		super.onResume();




	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.w("asdfasdf", "ANDROID - CREATE SETTINGS");
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.fragment_settings);

		startMessageThread();


	}

	private void removeFile(String fid ) {

		enableSounds(false);
		final ApplicationState application = (ApplicationState) this.getActivity().getApplicationContext();
		JSONObject json_request = new JSONObject(); //TODO CAN REMOVE EMPTY REQUEST?

		JsonObjectRequest remove_request = new JsonObjectRequest(
				Request.Method.POST,
				ApplicationState.HTTPS_ADDRESS_PB_RM + application.server_secret + "/"+fid,
				json_request,
				new Response.Listener<JSONObject>() {
					@Override
					public void onResponse(JSONObject response) {


						boolean success = false;
						try {
							success = response.getInt("status") == 1;
						} catch (JSONException error) {
							//TODO
						}

						if (success) {
							Toast toast = Toast.makeText(application.getApplicationContext(), "Removed", Toast.LENGTH_SHORT);
							toast.setGravity(Gravity.TOP|Gravity.CENTER, 0, 0);
							toast.show();
							updateSoundsList();
						}
					}
				},
				new Response.ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError error) {
							Toast toast = Toast.makeText(application.getApplicationContext(), error.toString(), Toast.LENGTH_SHORT);
							toast.setGravity(Gravity.TOP|Gravity.CENTER, 0, 0);
							toast.show();
					}
				}
		);
		application.request_queue.add(remove_request);
		return;
	}

	private void enableSounds(boolean x) {
		ListPreference alert_sounds = (ListPreference) findPreference("alert_sounds");
		ListPreference selfie_sounds = (ListPreference) findPreference("selfie_sounds");
		ListPreference remove_sounds = (ListPreference) findPreference("remove_sounds");
		alert_sounds.setEnabled(x);
		selfie_sounds.setEnabled(x);
		remove_sounds.setEnabled(x);
	}

	private void updateSoundsList() {

		enableSounds(false);

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

								for (int index = 0; index < sound_files.length(); index++) {
									sound_names[index] = sound_files.getJSONArray(index).getString(1);
									sound_IDs[index] = sound_files.getJSONArray(index).getString(0);
								}

								ListPreference alert_sounds = (ListPreference) findPreference("alert_sounds");
								alert_sounds.setEntries(sound_names);
								alert_sounds.setEntryValues(sound_IDs);

								ListPreference selfie_sounds = (ListPreference) findPreference("selfie_sounds");
								selfie_sounds.setEntries(sound_names);
								selfie_sounds.setEntryValues(sound_IDs);
								selfie_sounds.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
									@Override
									public boolean onPreferenceChange(Preference preference, Object newValue) {
										String fid = (String) newValue;

										ApplicationState state = (ApplicationState) getActivity().getApplicationContext();
										String url = ApplicationState.HTTPS_ADDRESS_PB_DL +  state.server_secret  + "/"+ fid;
										pb.set("selfie_sound_url",url);
										return false;
									}
								});

								ListPreference remove_sounds = (ListPreference) findPreference("remove_sounds");
								remove_sounds.setEntries(sound_names);
								remove_sounds.setEntryValues(sound_IDs);
								remove_sounds.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
									@Override
									public boolean onPreferenceChange(Preference preference, Object newValue) {
										String fid = (String) newValue;
										removeFile(fid);
										return false;
									}
								});



								enableSounds(true);
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

	public void onSoundUploaded(String name, String fileID){

		ListPreference alert_sounds = (ListPreference) findPreference("alert_sounds");
		ListPreference selfie_sounds = (ListPreference) findPreference("selfie_sounds");
		ListPreference remove_sounds = (ListPreference) findPreference("remove_sounds");
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

		remove_sounds.setEntries(sound_names);
		remove_sounds.setEntryValues(fileIDs);
	}

	void startMessageThread(){
		//start up a read thread
		final Thread read_thread = new Thread() {
			@Override
			public void run() {

				ApplicationState state = (ApplicationState) getActivity().getApplicationContext();
				pb = new PBConnector(state.server, state.port, state.server_secret, null, null, null, null);
				pb.getSettings();

				getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						setup();
						updateSoundsList();
					}
				});

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
	}

	void parseSettings(String settings_string){

		settings_retrieved = true;

		String[] settings_list = settings_string.split("\n");
		for(String pair : settings_list){
			Log.e("asdfasdf pair", pair);
			String[] setting = pair.split("\t");
			Log.e("asdfasdf setting", setting[0]);
			if(setting.length == 2) {
				String setting_name = setting[0];
				String setting_value = setting[1];
				if (setting_name.equals("VERSION")) {
					Preference version = findPreference("VERSION");
					version.setSummary(setting_value);
				} else if (setting_name.equals("selfie_timeout")) {
					selfie_timeout.setValue(Integer.parseInt(setting_value) / 3600);
					selfie_timeout.setEnabled(true);
				} else if (setting_name.equals("selfie_length")) {
					selfie_length.setValue(Integer.parseInt(setting_value));
					selfie_length.setEnabled(true);
				} else if (setting_name.equals("master_volume")) {
					volume_preference.setValue(Integer.parseInt(setting_value));
					volume_preference.setEnabled(true);
				} else if (setting_name.equals("selfie_pet_sensitivity")) {
					selfie_sensitivity_slider.setValue(Math.round(Float.parseFloat(setting_value)*100));
					selfie_sensitivity_slider.setEnabled(true);
				} else if (setting_name.equals("selfie_mot_sensitivity")) {
					motion_sensitivity_slider.setValue(Math.round(Float.parseFloat(setting_value)*100));
					motion_sensitivity_slider.setEnabled(true);
				} else if (setting_name.equals("pb_led_enable")) {
					SwitchPreference led_enable_switch = (SwitchPreference) findPreference("LED");
					led_enable_switch.setEnabled(true);
					led_enable_switch.setChecked(setting_value.equals("1"));
				} else if (setting_name.equals("pb_selfie_enable")) {
					SwitchPreference selfie_enable_switch = (SwitchPreference) findPreference("pb_selfie_enable");
					selfie_enable_switch.setEnabled(true);
					selfie_enable_switch.setChecked(setting_value.equals("1"));
				}
			}
		}

	}

	@Override
	public void onStop(){
		super.onStop();
	}
}
