package com.petbot;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

public class SettingsActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
	}

	@Override
	public void onBackPressed(){
		finish();
		Intent open_main = new Intent(SettingsActivity.this, PetBot.class);
		startActivity(open_main);
		/*Log.e("asdfasdf", "activity here");
		FragmentTransaction transaction = getFragmentManager().beginTransaction();
		transaction.remove(getFragmentManager().findFragmentById(R.id.settings));
		transaction.comenu items should specify a title"mmit();
		Log.e("asdfasdf", "theretherethere");
		super.onBackPressed();*/
	}
}
