package com.atos.petbot;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.atos.petbot.R;

public class SeekBarPreference extends Preference implements OnSeekBarChangeListener {
	private SeekBar mSeekBar;
	private int mProgress;
	private OnSeekBarChangeListener s=null;
	private int seekMax = 100;

	public SeekBarPreference(Context context) {
		this(context, null, 0);
	}

	public SeekBarPreference(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public SeekBarPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setLayoutResource(R.layout.preference_seekbar);
	}

	public void setOnSeekBarChangeListener(OnSeekBarChangeListener s) {
		Log.w("petbot","SETTING VALUE AT xxx yy zz ");
		this.s=s;
	}

	@Override
	protected void onBindView(View view) {
		super.onBindView(view);
		mSeekBar = (SeekBar) view.findViewById(R.id.seekbar);
		mSeekBar.setProgress(mProgress);
		if (s==null) {
			Log.w("petbot","SETTING VALUE AT bind null");
			mSeekBar.setOnSeekBarChangeListener(this);
		} else {
			Log.w("petbot","SETTING VALUE AT bind not null");
			mSeekBar.setOnSeekBarChangeListener(s);
		}
		mSeekBar.setMax(seekMax);
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		Log.w("petbot","SETTING VALUE AT xxx" + Integer.toString(progress));
		if (!fromUser)
			return;

		setValue(progress);
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		// not used
		Log.w("petbot","SETTING VALUE AT xxx yy ");
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		Log.w("petbot","SETTING VALUE AT xxx yy ");
		// not used
	}

	@Override
	protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
		setValue(restoreValue ? getPersistedInt(mProgress) : (Integer) defaultValue);
	}

	public void setMax(int value) {
		seekMax=value;
	}

	public void setValue(int value) {

		Log.w("petbot","SETTING VALUE AT " + Integer.toString(value));
		if (shouldPersist()) {
			persistInt(value);
		}

		if (value != mProgress) {
			mProgress = value;
			notifyChanged();
		}
	}

	public float getFloatValue() {
		return ((float)mSeekBar.getProgress())/100;
	}

	public int getValue(){
		return mProgress;
	}

	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		return a.getInt(index, 0);
	}
}

