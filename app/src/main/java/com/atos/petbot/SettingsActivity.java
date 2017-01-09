package com.atos.petbot;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.petbot.R;

public class SettingsActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
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
		//super.onBackPressed();
		//finish();
		//Intent open_main = new Intent(SettingsActivity.this, PetBot.class);
		//startActivity(open_main);
		/*Log.e("asdfasdf", "activity here");
		FragmentTransaction transaction = getFragmentManager().beginTransaction();
		transaction.remove(getFragmentManager().findFragmentById(R.id.settings));
		transaction.comenu items should specify a title"mmit();
		Log.e("asdfasdf", "theretherethere");
		super.onBackPressed();*/
	}
}
