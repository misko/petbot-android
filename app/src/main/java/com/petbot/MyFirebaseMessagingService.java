package com.petbot;

/**
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

import com.petbot.SelfieActivity;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

	private static final String TAG = "MyFirebaseMsgService";

	/**
	 * Called when message is received.
	 *
	 * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
	 */
	// [START receive_message]
	@Override
	public void onMessageReceived(RemoteMessage remoteMessage) {
		// [START_EXCLUDE]
		// There are two types of messages data messages and notification messages. Data messages are handled
		// here in onMessageReceived whether the app is in the foreground or background. Data messages are the type
		// traditionally used with GCM. Notification messages are only received here in onMessageReceived when the app
		// is in the foreground. When the app is in the background an automatically generated notification is displayed.
		// When the user taps on the notification they are returned to the app. Messages containing both notification
		// and data payloads are treated as notification messages. The Firebase console always sends notification
		// messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options
		// [END_EXCLUDE]

		// TODO(developer): Handle FCM messages here.
		// Not getting messages here? See why this may be: https://goo.gl/39bRNJ
		Log.d(TAG, "From: " + remoteMessage.getFrom());

		// Check if message contains a data payload.
		if (remoteMessage.getData().size() > 0) {
			Log.d(TAG, "Message data payload: " + remoteMessage.getData());
		}

		// Check if message contains a notification payload.
		if (remoteMessage.getNotification() != null) {
			Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
			sendNotification( remoteMessage);
		}

		// Also if you intend on generating your own notifications as a result of a received FCM
		// message, here is where that should be initiated. See sendNotification method below.

		/*sendNotification(remoteMessage.getNotification().getBody());


		String remote_data = remoteMessage.getNotification().getBody();
		try {
			JSONObject remote_json = new JSONObject(remote_data);
			Intent open_selfie = new Intent(MyFirebaseMessagingService.this, SelfieActivity.class);
			//open_main.putExtra("json",response.toString());
			open_selfie.putExtra("media_url", remote_json.getString("media_url"));
			open_selfie.putExtra("rm_url", remote_json.getString("rm_url"));
			MyFirebaseMessagingService.this.startActivity(open_selfie);
		} catch (JSONException error) {

		}*/
	}
	// [END receive_message]

	/**
	 * Create and show a simple notification containing the received FCM message.
	 *
	 * @param remoteMessage FCM message
	 */
	private void sendNotification(RemoteMessage remoteMessage) {
		Intent intent = new Intent(this, SelfieActivity.class);

		Map<String, String> hmap ;
		hmap = remoteMessage.getData();
		//hmap.get("data_info");
		intent.putExtra("media_url", hmap.get("media_url"));
		intent.putExtra("rm_url", hmap.get("rm_url"));
		intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);


		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
				PendingIntent.FLAG_ONE_SHOT);

		NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this);
		notificationBuilder.setSmallIcon(R.mipmap.pblogo)
				.setContentTitle(remoteMessage.getNotification().getTitle())
				.setContentText(remoteMessage.getNotification().getBody())
				.setAutoCancel(true)
				.setContentIntent(pendingIntent);

		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(0, notificationBuilder.build());
	}
}

