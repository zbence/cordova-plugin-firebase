package org.apache.cordova.firebase;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.text.TextUtils;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;
import java.util.Set;


public class FirebasePluginMessagingService extends FirebaseMessagingService {

    private static final String TAG = "Bizboard:Firebase";

    public FirebasePluginMessagingService() {
        super();
    }

    @Override
    public void onRebind(Intent intent) {
        Log.d(TAG,"rebind " + intent);
        super.onRebind(intent);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG,"destroy");
        super.onDestroy();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG,"unbind " + intent);
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        Log.d(TAG,"create");
        super.onCreate();
    }

    @Override
    public void onDeletedMessages() {
        Log.d(TAG, "onDeletedMessages");
        super.onDeletedMessages();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d(TAG, "onTaskRemoved");
        super.onTaskRemoved(rootIntent);
    }

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        Log.d(TAG, "onMessageReceived");

        // [START_EXCLUDE]
        // There are two types of messages data messages and notification messages. Data messages are handled
        // here in onMessageReceived whether the app is in the foreground or background. Data messages are the type
        // traditionally used with GCM. Notification messages are only received here in onMessageReceived when the app
        // is in the foreground. When the app is in the background an automatically generated notification is displayed.
        // When the user taps on the notification they are returned to the app. Messages containing both notification
        // and data payloads are treated as notification messages. The Firebase console always sends notification
        // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options
        // [END_EXCLUDE]

        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        String title = null;
        String text = null;
        String id = remoteMessage.getMessageId();

        Log.d(TAG, "From: " + remoteMessage.getFrom());
        Log.d(TAG, "Remote Message ID: " + id);

        // Is this a notification or a data only message
        RemoteMessage.Notification notification = remoteMessage.getNotification();
        Bundle data = map2bundle(remoteMessage.getData());

        if (notification != null) {

            // get notification details
            title = notification.getTitle();
            text = notification.getBody();

            Log.d(TAG, "Notification Message Title: " + title);
            Log.d(TAG, "Notification Message Body: " + text);

            if (!TextUtils.isEmpty(text) || !TextUtils.isEmpty(title)) {

                // The notification has something to display, as long as the client is
                // not active, then generate a notification
                if (FirebasePlugin.isPaused()) {
                    Log.d(TAG, "NOTIFICATION RECEIVED WHILE PAUSED, SEND NOTIFICATION");
                    sendNotification(id, title, text, data);
                    return;
                }

                // we have received a push message with a notification element while the client
                // is in foreground, in this case we don't want to show a notification, which would
                // just sit there until the user opened or dismissed it, but we want to send
                // the notification as data to the client for it to deal with immediately.
                Log.d(TAG, "NOTIFICATION RECEIVED WHILE NOT PAUSED, SEND AS DATA");
                Bundle notify = new Bundle();
                notify.putString("title",title);
                notify.putString("body",text);
                notify.putString("sound", notification.getSound());
                notify.putString("tag", notification.getTag());
                notify.putString("icon", notification.getIcon());
                notify.putString("color", notification.getColor());
                notify.putString("clickAction", notification.getClickAction());
                data.putBundle("notification", notify);
            }
        }

        // Wasn't a valid notification, send data to the client
        if (data != null) {
            title = data.getString("title");
            text = data.getString("text");
            Log.d(TAG, "Data Message Title: " + title);
            Log.d(TAG, "Data Message Text: " + text);

            // Send the data directly to the client
            sendData(id, data);
        }
    }

    // Convert a string map to a bundle
    private Bundle map2bundle(Map<String,String> data) {
        Bundle bundle = new Bundle();
        if (null != data) {
            for (String key : data.keySet()) {
                bundle.putString(key, data.get(key));
            }
        }
        return bundle;
    }

    private void sendData(String id, Bundle data) {
        data.putBoolean("firebase.is_push", true);
        data.putBoolean("firebase.is_notify", false);
        Log.d(TAG, "sendData " + id + ", " + data);
        Intent intent = new Intent(this, OnNotificationOpenReceiver.class);
        intent.putExtras(data);
        Log.d(TAG, "send broadcast " + intent);
        sendBroadcast(intent);
        Log.d(TAG, "done broadcast " + intent);
    }

    private void sendNotification(String id, String title, String messageBody, Bundle data) {
        data.putBoolean("firebase.is_push", true);
        data.putBoolean("firebase.is_notify", true);
        Intent intent = new Intent(this, OnNotificationOpenReceiver.class);
        intent.putExtras(data);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, id.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(getApplicationInfo().icon)
                .setContentTitle(title)
                .setContentText(messageBody)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(messageBody))
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(id.hashCode(), notificationBuilder.build());
    }
}