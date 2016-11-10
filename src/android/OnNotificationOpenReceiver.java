package org.apache.cordova.firebase;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import com.redskyit.mobile.rmcv2.firebase.FirebasePlugin;

public class OnNotificationOpenReceiver extends WakefulBroadcastReceiver {

    static String TAG = "Bizboard:Firebase";

    // This is called when a notification (as triggered by FireBasePluginMessagingService
    // onMessageReceived() when the payload contains a notification) is opened by the user.
    //
    // We only create a notification if the client is in background.  When the client is in
    // foreground, onMessageReceived() will send the message directly to the client, even if
    // the payload contained a notification, in which case it sets notification true.

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "RECEIVE OPEN NOTIFICATION " + intent);
        FirebasePlugin.onBroadcastReceive(context, intent);
        completeWakefulIntent(intent);
        Log.d(TAG, "COMPLETE NOTIFICATION " + intent);
    }
}