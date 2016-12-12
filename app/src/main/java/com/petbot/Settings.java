package com.petbot;

import android.os.Bundle;
import android.preference.PreferenceFragment;

public class Settings extends PreferenceFragment {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.fragment_settings);
	}

}
