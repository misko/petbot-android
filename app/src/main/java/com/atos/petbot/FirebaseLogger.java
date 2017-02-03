package com.atos.petbot;

import android.content.Context;
import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;

public class FirebaseLogger {

	private static log_level threshold = log_level.ERROR;
	private static FirebaseAnalytics firebase;

	enum log_level {
		ERROR, WARN, INFO, DEBUG, TRACE
	}

	private FirebaseLogger() {}

	public static void initialize(Context context) {
		firebase = FirebaseAnalytics.getInstance(context);
	}

	private static void log(log_level level, String message) {

		if(threshold.compareTo(level) < 0) return;

		Bundle log_data = new Bundle();
		log_data.putString("MESSAGE", message);
		firebase.logEvent(level.name(), log_data);
	}

	public static void setLogLevel(log_level level) {
		threshold = level;
	}

	public static void logError(String message) {
		log(log_level.ERROR, message);
	}

	public static void logWarn(String message) {
		log(log_level.WARN, message);
	}

	public static void logInfo(String message) {
		log(log_level.INFO, message);
	}

	public static void logDebug(String message) {
		log(log_level.DEBUG, message);
	}

	public static void logTrace(String message) {
		log(log_level.TRACE, message);
	}
}
