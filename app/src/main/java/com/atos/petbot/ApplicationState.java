package com.atos.petbot;

import android.app.Application;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

public class ApplicationState extends Application {


	public static final String HTTPS_ADDRESS = "https://petbot.ca:5000/";
	public static final String HTTPS_ADDRESS_DEAUTH = HTTPS_ADDRESS +"DEAUTH";
	public static final String HTTPS_ADDRESS_AUTH = HTTPS_ADDRESS + "AUTH";
	public static final String HTTPS_ADDRESS_QRCODE_JSON = HTTPS_ADDRESS + "PB_QRCODE_JSON";
	public static final String HTTPS_ADDRESS_SETUP_CHECK = HTTPS_ADDRESS + "SETUP/CHECK";
	public static final String HTTPS_ADDRESS_PB_REGISTER = HTTPS_ADDRESS + "PB_REGISTER";
	public static final String HTTPS_ADDRESS_PB_LISTEN = HTTPS_ADDRESS + "SETUP/LISTEN";
	public static final String HTTPS_ADDRESS_PB_PING = HTTPS_ADDRESS + "SETUP/PING";
	public static final String HTTPS_ADDRESS_PB_LS = HTTPS_ADDRESS + "FILES_LS/";
	public static final String HTTPS_ADDRESS_PB_DL = HTTPS_ADDRESS + "FILES_DL/";
	public static final String HTTPS_ADDRESS_PB_UL = HTTPS_ADDRESS + "FILES_UL/";
	public static final String HTTPS_ADDRESS_PB_SELFIE = HTTPS_ADDRESS + "FILES_SELFIE/";
	public static final String HTTPS_ADDRESS_PB_RM = HTTPS_ADDRESS + "FILES_RM/";
	public static final String HTTPS_ADDRESS_PB_WAIT = HTTPS_ADDRESS + "WAIT";
	public static final String HTTPS_ADDRESS_PB_SELFIE_COUNT = HTTPS_ADDRESS + "FILES_SELFIE_COUNT/";
	public static final String HTTPS_ADDRESS_PB_SELFIE_LAST = HTTPS_ADDRESS + "FILES_SELFIE_LAST/";

	RequestQueue request_queue;
	String server;
	int port;
	String username;
	String server_secret;
	String status="";

	String stun_server ="";
	String stun_port="";
	String stun_username="";
	String stun_password="";

	@Override
	public void onCreate() {
		super.onCreate();

		request_queue = Volley.newRequestQueue(this.getApplicationContext());
	}
}
