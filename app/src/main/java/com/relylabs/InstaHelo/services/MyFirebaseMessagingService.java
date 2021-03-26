package com.relylabs.InstaHelo.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONObject;

import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.relylabs.InstaHelo.MainActivity;
import com.relylabs.InstaHelo.R;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Random;


/**
 * Created by nagendra on 9/15/18.
 * *
 */

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String channel_id = "com.relylabs.InstaHelo";
    private static final String CHANNEL_ID = "1";

    @Override
    public void onDeletedMessages() {
        super.onDeletedMessages();
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d("debug_data", "onNewToken called: " + token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Log.d("debug_data", "Message received");

        Map<String, String> data = remoteMessage.getData();
        if (data.containsKey("action")) {
            Log.d("debug_data", "Action for screen refresh");
            action_for_screen_refresh();
        }

        createNotificationChannel();
        try {
            sendNotification(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d("data",data.toString());
        super.onMessageReceived(remoteMessage);
    }

    private  void action_for_screen_refresh() {
        Bundle data_bundle = new Bundle();
        data_bundle.putString("update_type", "REFRESH_FEED");
        Intent intent = new Intent("update_from_service");
        intent.putExtras(data_bundle);
        getApplicationContext().sendBroadcast(intent);
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "channel";
            String description = "description";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
    private void sendNotification( Map<String,String > data) throws IOException {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        String title = data.get("title");
        String messageBody = data.get("text");
        intent.putExtra("type", data.get("type"));
        intent.putExtra("value",data.get("value"));
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);
        String pic = data.get("img_url");
        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this,CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(messageBody)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);
        Bitmap bmp = Picasso.get().load(pic).get();
        notificationBuilder.setLargeIcon(bmp);
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());



    }
}
