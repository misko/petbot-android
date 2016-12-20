package com.petbot;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;

public class SoundRecorderPreference extends Preference {

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
}
