package com.relylabs.InstaHelo.services;

import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONObject;

import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.relylabs.InstaHelo.MainActivity;
import com.relylabs.InstaHelo.R;
import androidx.annotation.NonNull;

import java.util.Map;
import java.util.Random;


/**
 * Created by nagendra on 9/15/18.
 * *
 */

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String channel_id = "com.relylabs.InstaHelo";

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
        super.onMessageReceived(remoteMessage);
    }

    private  void action_for_screen_refresh() {
        Bundle data_bundle = new Bundle();
        data_bundle.putString("update_type", "REFRESH_FEED");
        Intent intent = new Intent("update_from_service");
        intent.putExtras(data_bundle);
        getApplicationContext().sendBroadcast(intent);
    }
}