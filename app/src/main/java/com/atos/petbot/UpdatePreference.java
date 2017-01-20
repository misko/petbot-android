package com.atos.petbot;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;

import static android.content.DialogInterface.BUTTON_NEGATIVE;

public class UpdatePreference extends DialogPreference {

	PBConnector pb;

	public UpdatePreference(Context context) {
		this(context, null);
	}

	public UpdatePreference(Context context, AttributeSet attrs) {
		this(context, attrs, android.R.attr.dialogPreferenceStyle);
	}

	public UpdatePreference(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {

		if(which == BUTTON_NEGATIVE){
			pb.cancelUpdate();
		}

	}

	@Override
	protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
		super.onPrepareDialogBuilder(builder);

		// prevent dismissing of dialog by outside clicks
		builder.setCancelable(false);

		// send command to update PetBot firmware
		ApplicationState state = (ApplicationState) getContext().getApplicationContext();
		pb = new PBConnector(state.server, state.port, state.server_secret, null, null, null, null);
		pb.update();
	}

}
