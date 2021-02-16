package com.relylabs.InstaHelo.services;

import android.app.NotificationManager;
import android.content.Context;
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

import java.util.Random;


/**
 * Created by nagendra on 9/15/18.
 * *
 */

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String channel_id = "com.relylabs.InstaHelo";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d("debug_data", "onNewToken called: " + token);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d("debug_data", "Inside message receive " + remoteMessage.getData().size());
        if (remoteMessage.getData().size() > 0) {
            Log.d("debug_data", "I am here");

            try {

                Log.d("debug_data", "Message data payload: " + remoteMessage.getData());
                JSONObject jsonObject = new JSONObject(remoteMessage.getData());
                String title  = jsonObject.getString("title");
                String message  = jsonObject.getString("message");
                sendNotification(title, message);
                //sendNotification(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void sendNotification(String title, String message) {
        initChannels(this);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channel_id);

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder nb =
                getIosChannelNotification(title, message);
        assert mNotificationManager != null;
        mNotificationManager.notify(0, nb.build());

    }

    public NotificationCompat.Builder getIosChannelNotification(String title, String body) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, new Random().nextInt(100), intent,
                PendingIntent.FLAG_ONE_SHOT);
        long when = System.currentTimeMillis();
        //mNotifyBuilder.setVibrate(new long[]{1000, 1000, 1000, 1000, 1000, 1000});
        //Uri sound_uri  = Uri.parse("uri://" +R.raw.notification);
        //Uri sound_uri = Uri.parse("android.resource://com.relylabs.phirki/notification.mp3");

        return new NotificationCompat.Builder(this, channel_id)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(body)
                        .setBigContentTitle(title)
                        .setSummaryText("done")
                )
                .setVibrate(new long[]{1000, 1000, 1000, 1000, 1000, 1000})
                .setContentIntent(pendingIntent)
                .setColor(getResources().getColor(R.color.red1))
                .setAutoCancel(true);
    }

    public static Bitmap resize(Context ctx, Bitmap bm) {

        int width = (int) ctx.getResources().getDimension(android.R.dimen.notification_large_icon_width);
        int height = (int) ctx.getResources().getDimension(android.R.dimen.notification_large_icon_height);
        // String msg = bm.getWidth() + "x" + bm.getHeight() + " --> " + width +
        // "x" + height;
        // RLog.d(NotificationUtil.class, msg);
        //Log.d("debug_data", "is null " + (bm == null));
        return Bitmap.createScaledBitmap(bm, width, height, false);
    }

    public void initChannels(Context context) {
        if (Build.VERSION.SDK_INT < 26) {
            return;
        }
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(channel_id,
                "NearTag Notification",
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("Receive NearTag Notification");
        assert notificationManager != null;
        notificationManager.createNotificationChannel(channel);
    }
}