package com.atos.petbot;

import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.WindowManager;

import com.atos.petbot.R;

public class SettingsActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	@Override
	protected void onPause(){
		Log.w("petbot", "onPause - settings activity");
		super.onPause();
	}

	@Override
	public void onBackPressed(){
		Log.w("petbot", "onBackPressed -settings activity");
		if (getFragmentManager().getBackStackEntryCount() > 0) {
			Log.w("petbot", "onBackPressed -settings activity - fragment");
			getFragmentManager().popBackStack();
		} else {
			Log.w("petbot", "onBackPressed -settings activity - not frag");
			super.onBackPressed();
		}
	}
}
