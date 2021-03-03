package com.relylabs.InstaHelo.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.relylabs.InstaHelo.App;
import com.relylabs.InstaHelo.R;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.relylabs.InstaHelo.MainActivity;
import com.relylabs.InstaHelo.ServerCallBack;
import com.relylabs.InstaHelo.Utils.RoomHelper;
import com.relylabs.InstaHelo.models.EventElement;
import com.relylabs.InstaHelo.models.RelySystem;
import com.relylabs.InstaHelo.models.User;
import com.relylabs.InstaHelo.models.UserSettings;
import com.relylabs.InstaHelo.models.UsersInRoom;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cz.msebera.android.httpclient.Header;
import io.agora.rtc.Constants;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.models.UserInfo;
import io.agora.rtm.ErrorInfo;
import io.agora.rtm.ResultCallback;
import io.agora.rtm.RtmChannel;
import io.agora.rtm.RtmChannelAttribute;
import io.agora.rtm.RtmChannelListener;
import io.agora.rtm.RtmChannelMember;
import io.agora.rtm.RtmClient;
import io.agora.rtm.RtmClientListener;
import io.agora.rtm.RtmFileMessage;
import io.agora.rtm.RtmImageMessage;
import io.agora.rtm.RtmMediaOperationProgress;
import io.agora.rtm.RtmMessage;

public class ActiveRoomService extends Service {
    public static final String CHANNEL_ID = "ForegroundServiceChannel";
    public static final String LOG_TAG = "debug_data";

    String agora_rtc_token = "";
    String agora_rtm_token = "";
    Boolean is_rtm_released = false;

    String selected_channel = "";
    String selected_channel_display_name = "";
    Integer selected_event_id = -1;


    ArrayList<EventElement> all_feeds;
    ArrayList<Integer> moderator_ids;

    private RtcEngine mRtcEngine;
    private RtmClient mRtmClient;
    private RtmChannel mRtmChannel;


    void send_message(String action, Integer uid) {
        JSONObject obj= new JSONObject();
        try {
            obj.put("USER_ACTION", action);
            obj.put("USER_ID", uid);
        } catch (JSONException e) {
            e.printStackTrace();
        }


        RoomHelper.server_update(uid, String.valueOf(selected_event_id), action, new ServerCallBack() {
            @Override
            public void onSuccess() {
                processRTMMessageSend(obj);
            }
        });
    }

    void processRTMMessageSend(JSONObject obj) {
        RtmMessage action_message = mRtmClient.createMessage();
        action_message.setText(obj.toString());
        Log.d("debug_audio", "send message to user" + obj.toString());
        mRtmChannel.sendMessage(action_message, new ResultCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.d("debug_audio", "message sent in channel: success");
            }

            @Override
            public void onFailure(ErrorInfo errorInfo) {
                Log.d("debug_audio", "message sent in channel: failed");
            }
        });
    }

    void processExit(Boolean should_shut_down_service, Integer room_id) {
        stopTimer();
        leave_channel_server_update(String.valueOf(room_id));
        process_leave_channel();
        selected_event_id = -1;
        UserSettings us = UserSettings.getSettings();
        us.selected_event_id = -1;
        us.save();
        if (should_shut_down_service) {
            Log.d("debug_audio", "Service shut down");
            stopSelf();
        }

        Log.d("debug_data", "Channel exit success");
    }

    BroadcastReceiver broadcastReceiverFromFragment = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String user_action =  intent
                    .getStringExtra("user_action");
            Integer uid;
            switch (user_action) {
                // audience sending MAKE_SPEAKER_REQUEST
                case "MAKE_SPEAKER_REQUEST":
                    uid =  intent
                            .getIntExtra("uid", -1);
                    // add request in server
                    RoomHelper.send_hand_raise_request(selected_event_id, false);
                    // send message to notifiy admins
                    send_message("MAKE_SPEAKER_REQUEST", uid);
                    break;

                // audience sending MAKE_SPEAKER_REQUEST
                case "SWITCH_CHANNEL":
                    String channel_name =  intent
                            .getStringExtra("channel_name");
                    selected_channel = channel_name;
                    String channel_display_name =  intent
                            .getStringExtra("channel_display_name");
                    Integer new_selected_event_id =  intent
                            .getIntExtra("room_id", -1);
                    processExit(false, selected_event_id);
                    // process remove old channel
                    selected_event_id = new_selected_event_id;
                    UserSettings us = UserSettings.getSettings();
                    us.selected_event_id = selected_event_id;
                    us.save();
                    // rejoin new channel
                    processAndConnectToAChannel(channel_name,  selected_event_id);
                    // re join send the message for the
                    break;

                case "HAND_RAISE_CLEAR":
                    RoomHelper.send_hand_raise_request(selected_event_id, true);
                    break;


                case "ROOM_CREATE":
                    String room_title =  intent
                            .getStringExtra("room_title");
                    String room_type =  intent
                            .getStringExtra("room_type");

                   /* send_create_room_request(room_title, room_type); */
                    break;


                case "REJECT_SPEAKER":
                    uid =  intent
                            .getIntExtra("uid", -1);
                    send_message("REJECT_SPEAKER", uid);
                    switch_roles_on_server(2, uid);
                    break;

                // from the explicit admin action
                case "MAKE_AUDIENCE":
                    uid =  intent
                            .getIntExtra("uid", -1);
                    send_message("MAKE_AUDIENCE", uid);
                    switch_roles_on_server(2, uid);
                    break;


                // from the explicit admin action
                case "MAKE_SPEAKER":
                    uid =  intent
                            .getIntExtra("uid", -1);
                    send_message("MAKE_SPEAKER", uid);
                    switch_roles_on_server(1, uid);
                    break;


                case "MUTE_UNMUTE_CLICK":
                    process_mute_unmute();
                    break;

                case "MUTE_UNMUTE_CLICK_RECONNECT":
                    process_channel_disconnect_reconnect();
                    break;

                case "LEAVE_CHANNEL_EXIT":
                    processExit(true, selected_event_id);
                    break;
            }
            Log.d("debug_data", "received broadcast " + user_action);
        }
    };


    private void initAgoraEngineAndJoinChannel(String channel_name, String agora_rtc_token, String agora_rtm_token, Boolean muted, Boolean load_room) {
        initializeAgoraEngine(muted);
        Integer role = -1;
        UserSettings us = UserSettings.getSettings();
        if (us.is_current_role_speaker) {
            role = Constants.CLIENT_ROLE_BROADCASTER;
        } else {
            role = Constants.CLIENT_ROLE_AUDIENCE;
        }
        update_role_user(role, new ServerCallBack() {
            @Override
            public void onSuccess() {
                runTimer();
                joinChannelRTC(channel_name, agora_rtc_token, agora_rtm_token, muted, load_room);
            }
        });

    }

    private void loadRoomFragment() {
        // seend notifiation to calling fragment to start stop
        Log.d("debug_audio", "Ready to load room fragment");
        Bundle data_bundle = new Bundle();
        data_bundle.putString("update_type", "JOINED_CONNECTION");
        Intent intent = new Intent("update_from_service");
        intent.putExtras(data_bundle);
        getApplicationContext().sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(broadcastReceiverFromFragment);
        super.onDestroy();
    }

    private void processAndConnectToAChannel(String channel_name, Integer event_id) {
        UserSettings us = UserSettings.getSettings();
        us.UserID = User.getLoggedInUserID();
        us.save();

        moderator_ids = new ArrayList<>();
        Log.d("debug_audio", "speaker list clear");
        final User user = User.getLoggedInUser();
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        params.add("channel_name", channel_name);
        params.add("channel_id", String.valueOf(event_id));

        JsonHttpResponseHandler jrep = new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    Log.d("debug_data", "Insde token processing");
                    agora_rtc_token = response.getString("agora_token");
                    agora_rtm_token = response.getString("agora_rtm_token");
                    Integer current_role_id = response.getInt("my_current_role");
                    JSONArray all_moderator_ids = response.getJSONArray("moderator_user_ids");
                    for (int i = 0; i < all_moderator_ids.length(); i++) {
                        moderator_ids.add(all_moderator_ids.getInt(i));
                    }

                    UserSettings us = UserSettings.getSettings();
                    if (current_role_id.equals(0)) {
                        us.is_current_role_speaker = Boolean.TRUE;
                        us.is_current_user_admin = Boolean.TRUE;
                    } else if (current_role_id.equals(1)) {
                        us.is_current_role_speaker = Boolean.TRUE;
                        us.is_current_user_admin = Boolean.FALSE;
                    } else {
                        us.is_current_role_speaker = Boolean.FALSE;
                        us.is_current_user_admin = Boolean.FALSE;
                    }
                    us.save();

                    initAgoraEngineAndJoinChannel(channel_name, agora_rtc_token, agora_rtm_token, true, true);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String res, Throwable t) {
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable t, JSONObject obj) {
            }
        };

        client.addHeader("Accept", "application/json");
        client.addHeader("Authorization", "Token " + user.AccessToken);
        client.post(App.getBaseURL() + "registration/get_token_for_channel", params, jrep);
    }


    private void initializeAgoraEngine(boolean muted) {
        try {
            UserSettings us = UserSettings.getSettings();

            // dele all users in room
            UsersInRoom.deleteAllRecords();

            // is_current_role_speaker = true;
            if (us.is_current_user_admin) {
                us.is_current_role_speaker = Boolean.TRUE;
                us.save();
            }

            mRtcEngine = RtcEngine.create(getApplicationContext(), getString(R.string.agora_app_id), mRtcEventHandler);
            mRtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
            mRtcEngine.setAudioProfile(Constants.AUDIO_PROFILE_MUSIC_HIGH_QUALITY_STEREO, Constants.AUDIO_SCENARIO_MEETING);
            mRtcEngine.setDefaultAudioRoutetoSpeakerphone(true);
            Integer role = -1;
            if (us.is_current_role_speaker) {
                role = Constants.CLIENT_ROLE_BROADCASTER;
            } else {
                role = Constants.CLIENT_ROLE_AUDIENCE;
            }

            mRtcEngine.setClientRole(role);
            us.is_muted = muted;
            us.save();
            mRtcEngine.disableVideo();
            mRtcEngine.enableAudio();
            mRtcEngine.enableAudioVolumeIndication(1000, 3, true);
        } catch (Exception e) {
            Log.e(LOG_TAG, Log.getStackTraceString(e));

            throw new RuntimeException("NEED TO check rtc sdk init fatal error\n" + Log.getStackTraceString(e));
        }
    }

    private void joinChannelRTC(String channel_name, String accessToken, String agora_rtm_token, Boolean muted, Boolean load_fragment) {
        // The uid is not specified. The SDK will assign one automatically.
        final User user = User.getLoggedInUser();
        mRtcEngine.joinChannel(accessToken,channel_name, "Extra Optional Data", user.UserID);

        mRtcEngine.muteLocalAudioStream(muted);
        UserInfo t = new UserInfo();
        mRtcEngine.getUserInfoByUid(user.UserID, t);

        UserSettings us = UserSettings.getSettings();
        if (us.is_current_role_speaker) {
            new UsersInRoom(Boolean.TRUE, muted, user.UserID, user.FirstName, user.ProfilePicURL).save();
        }

        if (load_fragment) {
            loadRoomFragment();
        } else {
            Log.d("debug_audio", "skip room load");
        }
        initRTM();
        joinChannelRTM(channel_name, agora_rtm_token);
    }

    private void joinChannelRTM(String channel_name, String accessToken) {
        final User user = User.getLoggedInUser();
        mRtmClient.login(accessToken, String.valueOf(user.UserID), new ResultCallback<Void>() {
            @Override
            public void onSuccess(Void responseInfo) {
                Log.d("debug_data", "Channel login success!");
                joinRTMChannel(channel_name);
            }
            @Override
            public void onFailure(ErrorInfo errorInfo) {
                // = false;
                Log.d("debug_data", "Channel login failure!");
            }
        });
    }

    private void update_role_user(Integer role, ServerCallBack cs) {
        final User user = User.getLoggedInUser();
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        params.add("event_id", String.valueOf(selected_event_id));
        String role_server = "2";
        if (role == Constants.CLIENT_ROLE_BROADCASTER) {
            role_server = "1";
        }
        params.add("role", role_server);
        Log.d("debug_audio", "server state update " + String.valueOf(selected_event_id));
        JsonHttpResponseHandler jrep = new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                Log.d("debug_audio", "update to server role success");
                cs.onSuccess();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String res, Throwable t) {
                Log.d("debug_audio", "update to server role. Error 1");

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable t, JSONObject obj) {
            }
        };

        client.addHeader("Accept", "application/json");
        client.addHeader("Authorization", "Token " + user.AccessToken);
        client.post(App.getBaseURL() + "page/update_role", params, jrep);
    }


    private void switch_roles_on_server(Integer role, Integer user_id) {
        final User user = User.getLoggedInUser();
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        params.add("event_id", String.valueOf(selected_event_id));
        params.add("role", String.valueOf(role));
        params.add("user_id", String.valueOf(user_id));

        JsonHttpResponseHandler jrep = new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String res, Throwable t) {
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable t, JSONObject obj) {
            }
        };

        client.addHeader("Accept", "application/json");
        client.addHeader("Authorization", "Token " + user.AccessToken);
        client.post(App.getBaseURL() + "page/switch_role", params, jrep);
    }

    private void leave_channel_server_update(String room_id) {
        final User user = User.getLoggedInUser();
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        params.add("event_id", room_id);
        JsonHttpResponseHandler jrep = new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                Log.d("debug_data", "Updated server for channel leave " + room_id);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String res, Throwable t) {
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable t, JSONObject obj) {
            }
        };

        client.addHeader("Accept", "application/json");
        client.addHeader("Authorization", "Token " + user.AccessToken);
        client.post(App.getBaseURL() + "page/leave_channel", params, jrep);
    }

    private  void joinRTMChannel(String channel_name) {
        try {
            mRtmChannel = mRtmClient.createChannel(channel_name, mRtmChannelListener);
        } catch (RuntimeException e) {
            Log.e("debug_data", "Fails to create channel. Maybe the channel ID is invalid," +
                    " or already in use. See the API Reference for more information.");
        }

        if (mRtmChannel == null) {
            return;
        }

        mRtmChannel.join(new ResultCallback<Void>() {
            @Override
            public void onSuccess(Void responseInfo) {
                Log.d("debug_audio", "Successfully joins the channel!");
            }

            @Override
            public void onFailure(ErrorInfo errorInfo) {
                Log.d("debug_audio", "join channel failure! errorCode = "
                        + errorInfo.getErrorCode());
            }
        });


    }

    private  void leaveRTMChannel() {
        try {
            mRtmChannel.leave(new ResultCallback<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d("debug_data", "RTM channel leave success");
                    mRtmChannel.release();
                }

                @Override
                public void onFailure(ErrorInfo errorInfo) {

                }
            });
        } catch (Exception ex) {
            Log.d("debug_data", "RTM cleanup issue");
        }

        is_rtm_released = true;
    }

    void process_mute_unmute() {
        UserSettings us = UserSettings.getSettings();
        RoomHelper.server_update(User.getLoggedInUserID(), String.valueOf(selected_event_id), us.is_muted ? "mute" : "unmute", new ServerCallBack() {
            @Override
            public void onSuccess() {
                mRtcEngine.enableAudio();
                mRtcEngine.muteLocalAudioStream(us.is_muted);
                Log.d("debug_audio", "Changed logcal mute unmute state to " + String.valueOf(us.is_muted));
            }
        });

        UsersInRoom.changeMuteState(User.getLoggedInUserID(), us.is_muted);
    }


    void process_channel_disconnect_reconnect() {
        // don't send instruction for reload fragment
        process_leave_channel();
        initAgoraEngineAndJoinChannel(selected_channel, agora_rtc_token, agora_rtm_token, false, false);
    }


    private final Handler handler = new Handler();

    private Runnable runnable = new Runnable() {
        public void run() {
            Log.d("debug_ping", "Sending ping");
            handler.postDelayed(this, 20000);
            RoomHelper.sendPing(selected_event_id);
        }
    };

    private final Handler handlerExit = new Handler();

    private Runnable runnableExit = new Runnable() {
        public void run() {
            Log.d("debug_ping", "Sending Leave Request: Check");
            if (UsersInRoom.getAllSpeakers().size() == 0) {
                askforexit();
                Log.d("debug_ping", "Sending Leave Request: Success");
                processExit(true, selected_event_id);
            }

            // check if the app is in background > 30 mins
            RelySystem rs = RelySystem.getSystemSettings();
            if (!rs.is_foreground && System.currentTimeMillis()/1000 - rs.timestamp_updated > 3600) {
                Log.d("debug_ping", "App in background for more than 30 mins");
                askforexit();
                processExit(true, selected_event_id);
            }
            handlerExit.postDelayed(this, 60000);
        }
    };


    private void askforexit() {
        Bundle data_bundle = new Bundle();
        data_bundle.putString("update_type", "EXIT_ROOM");
        Intent intent = new Intent("update_from_main");
        intent.putExtras(data_bundle);
        getApplicationContext().sendBroadcast(intent);
    }

    private void runTimer() {
        handler.postDelayed(runnable, 20000);
        handlerExit.postDelayed(runnableExit, 60000);
    }

    private void stopTimer() {
        if(runnable != null){
            handler.removeCallbacks(runnable);
            runnable = null;
            Log.d("debug_ping", "Timer stopped");
        }

        if(runnableExit != null){
            handlerExit.removeCallbacks(runnableExit);
            Log.d("debug_ping", "Timer stopped Exit");
            runnableExit = null;
        }
    }

    private void stopPings() {

    }

    private void server_update(Integer user_id, String event_id, String user_action, ServerCallBack callback) {
        final User user = User.getLoggedInUser();
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        params.add("user_id", String.valueOf(user_id));
        params.add("event_id", event_id);
        params.add("user_action", user_action);

        JsonHttpResponseHandler jrep = new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                callback.onSuccess();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String res, Throwable t) {
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable t, JSONObject obj) {
            }
        };

        client.addHeader("Accept", "application/json");
        client.addHeader("Authorization", "Token " + user.AccessToken);
        client.post(App.getBaseURL() + "page/update_action", params, jrep);
    }



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("debug_data", "Service started");
        Integer room_id = intent.getIntExtra("room_id", -1);
        String channel_name = intent.getStringExtra("channel_name");
        String channel_display_name = intent.getStringExtra("channel_display_name");
        createNotificationChannel();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("InstaHelo Room")
                .setContentText(channel_display_name)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1, notification);

        IntentFilter data_from_fragment = new IntentFilter("data_from_fragment");
        registerReceiver(broadcastReceiverFromFragment, data_from_fragment);


        moderator_ids = new ArrayList<>();
        selected_event_id = room_id;
        selected_channel = channel_name;
        processAndConnectToAChannel(channel_name, room_id);
        return START_NOT_STICKY;
    }


    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d("debug_data", "Task removed");
        processExit(true,selected_event_id);
        super.onTaskRemoved(rootIntent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private RtmChannelListener mRtmChannelListener = new RtmChannelListener() {

        @Override
        public void onMemberCountUpdated(int i) {
            Log.d("debug_data", "onMemberCountUpdated  Count " + String.valueOf(i));
            broadcastfrommain("onMemberCountUpdated");
        }

        @Override
        public void onAttributesUpdated(List<RtmChannelAttribute> list) {

        }

        @Override
        public void onMessageReceived(RtmMessage message, RtmChannelMember fromMember) {
            String uid_str = message.getText();
            Log.d("debug_audio", "message received in channel " + uid_str);
            String user_action = "";
            Integer user_id = -1;

            try {
                JSONObject jsonObject = new JSONObject(uid_str);
                user_action = jsonObject.getString("USER_ACTION");
                user_id = jsonObject.getInt("USER_ID");
            } catch (JSONException err){
                Log.d("Error", err.toString());
            }

            Log.d("debug_audio", "user action " + user_action);
            UserSettings us = UserSettings.getSettings();
            if (us.is_current_user_admin && user_action.equals("MAKE_SPEAKER_REQUEST")) {
                us.audience_hand_raised = true;
                us.save();
                askforRefresh();
            }

            if (User.getLoggedInUser().UserID.equals(user_id)) {

                if (user_action.equals("MAKE_SPEAKER")) {
                    mRtcEngine.setClientRole(Constants.CLIENT_ROLE_BROADCASTER);
                    mRtcEngine.muteLocalAudioStream(true);
                    us = UserSettings.getSettings();
                    us.is_current_role_speaker = true;
                    us.is_muted = true;
                    us.is_self_hand_raised = false;
                    us.save();

                    // remove from audience queue and move to speaker queue and broadcast for the update
                    UsersInRoom u = UsersInRoom.getRecords(user_id);
                    if (u == null) {
                        new UsersInRoom(Boolean.TRUE, Boolean.TRUE, user_id).save();
                    }


                    broadcastfrommain("onMessageReceived");
                }

                if (user_action.equals("MAKE_AUDIENCE")) {
                    // change the current user role
                    mRtcEngine.setClientRole(Constants.CLIENT_ROLE_AUDIENCE);
                    mRtcEngine.muteLocalAudioStream(true);
                    us  = UserSettings.getSettings();
                    us.is_current_role_speaker = false;

                    us.is_muted = true;
                    us.save();
                    Log.d("debug_audio", "Updating channel users");
                    // remove from audience queue and move to speaker queue and broadcast for the update
                    UsersInRoom u = UsersInRoom.getRecords(user_id);
                    if (u != null) {
                        u.delete();
                    }

                    broadcastfrommain("onMessageReceived");
                }

                if (user_action.equals("REJECT_SPEAKER")) {
                    // once state changes clear the flag
                    //  UserSettings us  = UserSettings.getSettings();
                    us.is_self_hand_raised = false;
                    us.is_muted = true;
                    us.save();

                    askforRefresh();
                    RoomHelper.broadcastToMainThread(getApplicationContext(), "HAND_RAISE_CLEAR_USER");
                }
            }
        }

        @Override
        public void onImageMessageReceived(RtmImageMessage rtmImageMessage, RtmChannelMember rtmChannelMember) {

        }

        @Override
        public void onFileMessageReceived(RtmFileMessage rtmFileMessage, RtmChannelMember rtmChannelMember) {

        }

        @Override
        public void onMemberJoined(RtmChannelMember member) {

            broadcastfrommain("member joined");
        }

        @Override
        public void onMemberLeft(RtmChannelMember member) {
            broadcastfrommain("member left");
        }

        private void broadcastfrommain(String location) {
            Bundle data_bundle = new Bundle();
            data_bundle.putString("update_type", "LIST_CHANGE");
            Intent intent = new Intent("update_from_service");
            intent.putExtras(data_bundle);
            getApplicationContext().sendBroadcast(intent);
        }

        private void askforRefresh() {
            Bundle data_bundle = new Bundle();
            data_bundle.putString("update_type", "REFRESH_SETTINGS");
            Intent intent = new Intent("update_from_service");
            intent.putExtras(data_bundle);
            getApplicationContext().sendBroadcast(intent);
        }
    };

    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {

        // Listen for the onUserOffline callback.
        // This callback occurs when the remote user leaves the channel or drops offline.
        @Override
        public void onUserOffline(final int uid, final int reason) {
            Log.d("debug_audio", "Speaker Offline " + String.valueOf(uid) + " Reason " + String.valueOf(reason));
            UsersInRoom u = UsersInRoom.getRecords(uid);
            if (u != null) {
                u.delete();
            }

            broadcastfrommain("onUserOffline");
        }

        @Override
        public void onConnectionStateChanged(int state, int reason) {
            super.onConnectionStateChanged(state, reason);
            Log.d("debug_audio", "Connection Changed  state " + String.valueOf(state) + " Reason " + String.valueOf(reason));
            broadcastrefetch();
        }

        @Override
        public void onConnectionLost() {
            Log.d("debug_audio", "Connection lost");
            super.onConnectionLost();
        }



        @Override
        public void onAudioVolumeIndication(AudioVolumeInfo[] speakers, int totalVolume) {
            super.onAudioVolumeIndication(speakers, totalVolume);
            Log.d("debug_speaker", "speaking now uid " + String.valueOf(speakers[0].uid) + "   Total volume " + String.valueOf(totalVolume) );
            ArrayList<Integer> uids = new ArrayList<>();
            ArrayList<Integer> speaking_on_off = new ArrayList<>();
            for (int i = 0 ;i < speakers.length; i++) {
                if (speakers[i].uid == 0) {
                    //self user
                    uids.add(User.getLoggedInUserID());
                } else {
                    uids.add(speakers[i].uid);
                }

                if (totalVolume > 10) {
                    speaking_on_off.add(1);
                } else {
                    speaking_on_off.add(0);
                }
            }

            Bundle data_bundle = new Bundle();
            data_bundle.putIntegerArrayList("uids", uids);
            data_bundle.putIntegerArrayList("speaking_on_off", speaking_on_off);
            data_bundle.putString("update_type", "VOLUME_INDICATOR");
            Intent intent = new Intent("update_from_service");
            intent.putExtras(data_bundle);
            getApplicationContext().sendBroadcast(intent);

        }

        public void onActiveSpeaker(int uid) {
            Log.d("debug_audio", "Active Speaker " + String.valueOf(uid));
        }

        @Override
        public void onUserJoined(int uid, int elapsed) {
            super.onUserJoined(uid, elapsed);
            Log.d("debug_audio", "Speaker Joined  " + String.valueOf(uid));
            UsersInRoom u = UsersInRoom.getRecords(uid);
            if (u == null) {
                new UsersInRoom(Boolean.TRUE, Boolean.TRUE, uid).save();
            }
            broadcastfrommain("onUserJoined");
        }


        @Override
        public void onRemoteAudioStateChanged(int uid, int state, int reason, int elapsed) {
            Log.d("debug_audio", "mute/unmute " + String.valueOf(uid) + " Reason : " + String.valueOf(reason) + " State : " + String.valueOf(state));
            if (state == Constants.REMOTE_AUDIO_STATE_STARTING) {
                UsersInRoom u = UsersInRoom.getRecords(uid);
                if (u != null) {
                    UsersInRoom.changeMuteState(uid, Boolean.FALSE);
                    broadcastmutestate(uid, false);
                }
            }

            if (reason == Constants.REMOTE_AUDIO_REASON_REMOTE_UNMUTED) {
                UsersInRoom u = UsersInRoom.getRecords(uid);
                if (u != null) {
                    UsersInRoom.changeMuteState(uid, Boolean.FALSE);
                    broadcastmutestate(uid, false);
                }
            }

            if (reason == Constants.REMOTE_AUDIO_REASON_REMOTE_MUTED) {
                UsersInRoom u = UsersInRoom.getRecords(uid);
                if (u != null) {
                    UsersInRoom.changeMuteState(uid, Boolean.TRUE);
                    broadcastmutestate(uid, true);
                }
            }

            super.onRemoteAudioStateChanged(uid, state, reason, elapsed);
        }

        void broadcastfrommain(String location) {
            Bundle data_bundle = new Bundle();
            data_bundle.putString("update_type", "LIST_CHANGE");
            Intent intent = new Intent("update_from_service");
            intent.putExtras(data_bundle);
            getApplicationContext().sendBroadcast(intent);
        }

        void broadcastmutestate(Integer uid, Boolean muted) {
            Bundle data_bundle = new Bundle();
            data_bundle.putBoolean("is_muted", muted);
            data_bundle.putInt("user_id", uid);
            data_bundle.putString("update_type", "MUTE_UNMUTE");
            Intent intent = new Intent("update_from_service");
            intent.putExtras(data_bundle);
            getApplicationContext().sendBroadcast(intent);
        }

        void broadcastrefetch() {
            Bundle data_bundle = new Bundle();
            data_bundle.putString("update_type", "CONNECTION_CHANGE");
            Intent intent = new Intent("update_from_service");
            intent.putExtras(data_bundle);
            getApplicationContext().sendBroadcast(intent);
        }
    };

    public void initRTM() {
        try {
            mRtmClient = RtmClient.createInstance(getApplicationContext(), getString(R.string.agora_app_id),
                    new RtmClientListener() {
                        @Override
                        public void onConnectionStateChanged(int state, int reason) {
                            Log.d("debug_data", "RTM Connection state changes to "
                                    + state + " reason: " + reason);
                        }

                        @Override
                        public void onMessageReceived(RtmMessage rtmMessage, String peerId) {
                            String msg = rtmMessage.getText();
                            Log.d("debug_data", "Message received " + " from " + peerId + msg
                            );
                        }

                        @Override
                        public void onImageMessageReceivedFromPeer(RtmImageMessage rtmImageMessage, String s) {

                        }

                        @Override
                        public void onFileMessageReceivedFromPeer(RtmFileMessage rtmFileMessage, String s) {

                        }

                        @Override
                        public void onMediaUploadingProgress(RtmMediaOperationProgress rtmMediaOperationProgress, long l) {

                        }

                        @Override
                        public void onMediaDownloadingProgress(RtmMediaOperationProgress rtmMediaOperationProgress, long l) {

                        }

                        @Override
                        public void onTokenExpired() {
                            Log.d("debug_info", "Token expired");

                        }

                        @Override
                        public void onPeersOnlineStatusChanged(Map<String, Integer> map) {

                        }
                    });
        } catch (Exception e) {
            Log.d("debug_data", "RTM SDK initialization fatal error!");
            throw new RuntimeException("You need to check the RTM initialization process.");
        }
    }

    void process_leave_channel() {
        try {
            if (mRtcEngine!= null) {
                mRtcEngine.leaveChannel();
                Log.d("debug_channel", "Leave channel success");
                mRtcEngine.destroy();
                Log.d("debug_channel", "RTC Engine destroy success");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!is_rtm_released) {
            leaveRTMChannel();
        }
        Log.d("debug_channel", "Leave channel success");
    }
}
