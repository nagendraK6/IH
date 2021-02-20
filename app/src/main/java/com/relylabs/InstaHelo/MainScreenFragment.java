package com.relylabs.InstaHelo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.Transition;
import androidx.transition.TransitionInflater;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.os.Bundle;
import android.widget.Toast;

import com.github.ybq.android.spinkit.SpinKitView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.shape.CornerFamily;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.SyncHttpClient;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.relylabs.InstaHelo.Utils.Logger;
import com.relylabs.InstaHelo.models.EventElement;
import com.relylabs.InstaHelo.models.User;
import com.relylabs.InstaHelo.BottomFragment;
import com.relylabs.InstaHelo.models.UserSettings;
import com.relylabs.InstaHelo.models.UsersInRoom;
import com.relylabs.InstaHelo.sharing.SendInviteFragment;
import com.squareup.picasso.Picasso;

import org.apache.commons.lang3.ArrayUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import cz.msebera.android.httpclient.Header;
import de.hdodenhof.circleimageview.CircleImageView;
import io.agora.rtc.Constants;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;


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
import io.agora.rtm.SendMessageOptions;

import io.agora.rtc.models.UserInfo;

interface ServerCallBack {
    void onSuccess();
}


public class MainScreenFragment extends Fragment implements NewsFeedAdapter.ItemClickListener, IOnBackPressed {

    void processExit() {
        leave_channel_server_update();
        process_leave_channel();
        is_room_fragment_loaded = false;
        selected_event_id = -1;

        unloadFragmentBottom();
    }

    BroadcastReceiver broadCastNewMessage = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String user_action =  intent
                    .getStringExtra("user_action");
            Integer uid;
            switch (user_action) {
                case "LEAVE_CHANNEL":
                    processExit();
                    break;

                case "MAKE_SPEAKER":
                    uid =  intent
                            .getIntExtra("uid", -1);
                    send_message("MAKE_SPEAKER", uid);
                    switch_roles(1, uid);
                    break;

                case "MAKE_AUDIENCE":
                    uid =  intent
                            .getIntExtra("uid", -1);
                    send_message("MAKE_AUDIENCE", uid);
                    switch_roles(2, uid);
                    break;

                case "MINIMISED":
                    loadFragmentInBottom();
                    is_room_fragment_loaded = false;
                    break;

                case "EXPAND_ROOM":
                    loadRoomFragment();
                    break;

                case "MUTE_UNMUTE_CLICK":
                    process_mute_unmute();
                    break;
            }
            Log.d("debug_data", "received broadcast " + user_action);
        }
    };

    private static final int PERMISSION_REQ_ID_RECORD_AUDIO = 22;
    private static final String LOG_TAG = "debug_data";



    /* ADMIN CONTROL */
    private boolean is_current_role_speaker = true;
    Boolean is_current_user_admin = true;

    NewsFeedAdapter adapter;
    RecyclerView news_feed_list;

    SpinKitView busy;
    String selected_channel = "";
    String selected_channel_display_name = "";
    Integer selected_event_id = -1;
    ArrayList<String> all_event_display_name;
    ArrayList<String> all_event_channel_name;
    ArrayList<Integer> all_event_ids;

    ArrayList<String> all_feeds;
    Boolean is_bottom_sheet_visible = false;
    Boolean is_room_fragment_loaded = false;
    Boolean is_muted = false;


    private RtcEngine mRtcEngine;
    private RtmClient mRtmClient;
    private RtmChannel mRtmChannel;

    TextView invites_count_display;
    Boolean show_inviter_card = true;

    private FragmentActivity activity;


    void send_message(String action, Integer uid) {
        JSONObject obj= new JSONObject();
        try {
            obj.put("USER_ACTION", action);
            obj.put("USER_ID", uid);
        } catch (JSONException e) {
            e.printStackTrace();
        }


        server_update(uid, String.valueOf(selected_event_id), action, new ServerCallBack() {
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
            if (User.getLoggedInUser().UserID.equals(user_id)) {
                if (user_action.equals("MAKE_SPEAKER")) {
                    // change the current user role
                    Log.d("debug_audio", "Changing role");
                    mRtcEngine.setClientRole(Constants.CLIENT_ROLE_BROADCASTER);
                    Log.d("debug_audio", "Changing role 1");
                    mRtcEngine.muteLocalAudioStream(true);
                    Log.d("debug_audio", "Changing role 2");
                    is_current_role_speaker = true;
                    Log.d("debug_audio", "Updating channel users");
                    // remove from audience queue and move to speaker queue and broadcast for the update
                    UsersInRoom u = UsersInRoom.getRecords(user_id);
                    if (u == null) {
                        new UsersInRoom(Boolean.TRUE, Boolean.TRUE, user_id).save();
                    }

                    is_muted = true;
                    Log.d("debug_audio", "Broadcasting from main to receive in room flow");
                    broadcastfrommain("onMessageReceived");
                }

                if (user_action.equals("MAKE_AUDIENCE")) {
                    // change the current user role
                    Log.d("debug_audio", "Changing role");
                    mRtcEngine.setClientRole(Constants.CLIENT_ROLE_AUDIENCE);
                    Log.d("debug_audio", "Changing role 1");
                    mRtcEngine.muteLocalAudioStream(true);
                    Log.d("debug_audio", "Changing role 2");
                    is_current_role_speaker = false;
                    Log.d("debug_audio", "Updating channel users");
                    // remove from audience queue and move to speaker queue and broadcast for the update
                    UsersInRoom u = UsersInRoom.getRecords(user_id);
                    if (u != null) {
                        u.delete();
                    }

                    is_muted = true;
                    Log.d("debug_audio", "Broadcasting from main to receive in room flow");
                    broadcastfrommain("onMessageReceived");
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
          //  data_bundle.putParcelableArrayList("speakers_list", speakers_list);
            data_bundle.putBoolean("is_current_role_speaker", is_current_role_speaker);
            data_bundle.putBoolean("is_current_user_admin", is_current_user_admin);;
            data_bundle.putString("event_title", selected_channel_display_name);
            data_bundle.putBoolean("is_muted", is_muted);
            Intent intent = new Intent("update_from_main");
            intent.putExtras(data_bundle);
            activity.sendBroadcast(intent);
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
            Log.d("debug_audio", "Connection Changed ");
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
           // Log.d("debug_data", "speaking now uid " + String.valueOf(speakers[0].uid) + "   Total volume " + String.valueOf(totalVolume) );
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
     //       data_bundle.putParcelableArrayList("speakers_list", speakers_list);
            data_bundle.putBoolean("is_current_role_speaker", is_current_role_speaker);
            data_bundle.putBoolean("is_current_user_admin", is_current_user_admin);;
            data_bundle.putString("event_title", selected_channel_display_name);
            Log.d("debug_voice", "Sending is_muted " + String.valueOf(is_muted));
            data_bundle.putBoolean("is_muted", is_muted);
            Intent intent = new Intent("update_from_main");
            intent.putExtras(data_bundle);
            activity.sendBroadcast(intent);
        }

        void broadcastmutestate(Integer uid, Boolean muted) {
            Bundle data_bundle = new Bundle();
            data_bundle.putBoolean("is_muted", muted);
            data_bundle.putInt("user_id", uid);
            data_bundle.putString("update_type", "MUTE_UNMUTE");
            Intent intent = new Intent("update_from_main");
            intent.putExtras(data_bundle);
            activity.sendBroadcast(intent);
        }

        void broadcastrefetch() {
            Bundle data_bundle = new Bundle();
            data_bundle.putString("update_type", "CONNECTION_CHANGE");
            Intent intent = new Intent("update_from_main");
            intent.putExtras(data_bundle);
            activity.sendBroadcast(intent);
        }
    };

    public void initRTM() {
        try {
            mRtmClient = RtmClient.createInstance(activity, getString(R.string.agora_app_id),
                    new RtmClientListener() {
                        @Override
                        public void onConnectionStateChanged(int state, int reason) {
                            Log.d("debug_data", "Connection state changes to "
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

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fargment_main_screen, container, false);
        IntentFilter new_post = new IntentFilter("update_from_room");
        activity.registerReceiver(broadCastNewMessage, new_post);
        busy = view.findViewById(R.id.loading_channel_token_fetch);
         UsersInRoom.deleteAllRecords();
 //       speakers_list = new ArrayList<>();
        all_event_display_name = new ArrayList<>();
        all_event_channel_name = new ArrayList<>();
        all_event_ids = new ArrayList<>();
        all_feeds = new ArrayList<>();
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d("debug_activity", "OnView Created called MainScreenFragment");
        news_feed_list = view.findViewById(R.id.news_feed_list);
        // Create adapter passing in the sample user data
        adapter = new NewsFeedAdapter(activity, all_feeds);
        adapter.setClickListener(this);

        // Attach the adapter to the recyclerview to populate items
        news_feed_list.setAdapter(adapter);
        PreCachingLayoutManager layoutManager = new PreCachingLayoutManager(activity);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        layoutManager.setExtraLayoutSpace(DeviceUtils.getScreenHeight(activity));
        news_feed_list.setLayoutManager(layoutManager);
        //news_feed_list.setOnClickListener();

        ShapeableImageView img = view.findViewById(R.id.user_profile_image);
        User user = User.getLoggedInUser();
        if (!user.ProfilePicURL.equals("")) {
            float radius = activity.getResources().getDimension(R.dimen.default_corner_radius_profile);
            img.setShapeAppearanceModel(img.getShapeAppearanceModel()
                    .toBuilder()
                    .setTopRightCorner(CornerFamily.ROUNDED,radius)
                    .setTopLeftCorner(CornerFamily.ROUNDED,radius)
                    .setBottomLeftCorner(CornerFamily.ROUNDED,radius)
                    .setBottomRightCorner(CornerFamily.ROUNDED,radius)
                    .build());
            Picasso.get().load(user.ProfilePicURL).into(img);
        }

        fetch_all_events();


        View invited_view = view.findViewById(R.id.invite_card);
        invited_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               loadFragmentWithoutupdate(new SendInviteFragment());
               // openBottomSheetDialog();
            }
        });

        invites_count_display = view.findViewById(R.id.invites_count_display);
        fetch_all_invites_count();


        ImageView invite = view.findViewById(R.id.invite);
        if (user.ShowWelcomeScreen) {
            changeViewToShow(view);
            user.ShowWelcomeScreen = Boolean.FALSE;
            user.save();
            show_inviter_card = false;
        } else {
            hideInvitesView(view);
        }
        invite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (show_inviter_card) {
                    hideInvitesView(view);
                } else {
                    changeViewToShow(view);
                }
            }
        });
    }


    private  void changeViewToShow(View v) {
        RelativeLayout inviter_card = v.findViewById(R.id.invite_card);
        RelativeLayout.LayoutParams params= new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.BELOW, R.id.app_bar);
        params.setMarginStart(50);
        params.setMarginEnd(50);
        inviter_card.setLayoutParams(params);
        inviter_card.setVisibility(View.VISIBLE);
        show_inviter_card = true;
    }


    private void hideInvitesView(View v) {
        RelativeLayout inviter_card = v.findViewById(R.id.invite_card);
        RelativeLayout.LayoutParams params= new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0);
        params.addRule(RelativeLayout.BELOW, R.id.app_bar);
        params.setMarginStart(50);
        params.setMarginEnd(50);
        inviter_card.setLayoutParams(params);
        inviter_card.setVisibility(View.INVISIBLE);
        show_inviter_card = false;
    }

    public boolean checkPermission(final Context context) {
        int currentAPIVersion = Build.VERSION.SDK_INT;
        if (currentAPIVersion >= android.os.Build.VERSION_CODES.M) {
            if (
                    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
            ) {
                if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, android.Manifest.permission.RECORD_AUDIO)) {
                    requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, PERMISSION_REQ_ID_RECORD_AUDIO);
                } else {
                    requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, PERMISSION_REQ_ID_RECORD_AUDIO);
                }
                return false;
            } else {
                return true;
            }
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int RC, String per[], int[] PResult) {
        super.onRequestPermissionsResult(RC, per, PResult);
        if (PResult.length > 0 && PResult[0] == PackageManager.PERMISSION_GRANTED) {
            processAndConnectToAChannel(selected_channel, selected_event_id);
        } else {
            Toast.makeText(activity, "Permission is needed to start audio call", Toast.LENGTH_LONG).show();
        }
    }

    private void initAgoraEngineAndJoinChannel(String channel_name, String agora_rtc_token, String agora_rtm_token) {
        initializeAgoraEngine();
        Integer role = -1;
        if (is_current_role_speaker) {
            role = Constants.CLIENT_ROLE_BROADCASTER;
        } else {
            role = Constants.CLIENT_ROLE_AUDIENCE;
        }
        update_role_user(role, new ServerCallBack() {
            @Override
            public void onSuccess() {
                hide_busy_indicator();
                joinChannelRTC(channel_name, agora_rtc_token, agora_rtm_token);
            }
        });

    }

    private void initializeAgoraEngine() {
        try {

           // is_current_role_speaker = true;
            if (is_current_user_admin) {
                is_current_role_speaker = Boolean.TRUE;
            }
            mRtcEngine = RtcEngine.create(activity, getString(R.string.agora_app_id), mRtcEventHandler);
            //mRtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
            mRtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
            mRtcEngine.setAudioProfile(Constants.AUDIO_PROFILE_MUSIC_HIGH_QUALITY_STEREO, Constants.AUDIO_SCENARIO_MEETING);
            mRtcEngine.setDefaultAudioRoutetoSpeakerphone(true);
            Integer role = -1;
            if (is_current_role_speaker) {
                role = Constants.CLIENT_ROLE_BROADCASTER;
            } else {
                role = Constants.CLIENT_ROLE_AUDIENCE;
            }
            mRtcEngine.setClientRole(role);
            is_muted = true;
            mRtcEngine.muteLocalAudioStream(true);
            mRtcEngine.disableVideo();
            mRtcEngine.enableAudioVolumeIndication(200, 3, true);
        } catch (Exception e) {
            Log.e(LOG_TAG, Log.getStackTraceString(e));

            throw new RuntimeException("NEED TO check rtc sdk init fatal error\n" + Log.getStackTraceString(e));
        }
    }

    private void joinChannelRTC(String channel_name, String accessToken, String agora_rtm_token) {// Call the joinChannel method to join a channel.
        // The uid is not specified. The SDK will assign one automatically.
        final User user = User.getLoggedInUser();
        mRtcEngine.joinChannel(accessToken,channel_name, "Extra Optional Data", user.UserID);

        mRtcEngine.muteLocalAudioStream(is_muted);
        UserInfo t = new UserInfo();
        mRtcEngine.getUserInfoByUid(user.UserID, t);

        if (is_current_role_speaker) {
            new UsersInRoom(Boolean.TRUE, Boolean.TRUE, user.UserID, user.FirstName, user.ProfilePicURL).save();
        }
        loadRoomFragment();
        initRTM();
        joinChannelRTM(channel_name, agora_rtm_token);
    }

    private void joinChannelRTM(String channel_name, String accessToken) {// Call the joinChannel method to join a channel.
        // The uid is not specified. The SDK will assign one automatically.
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
        JsonHttpResponseHandler jrep = new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                Log.d("debug_audio", "update to server role");
                cs.onSuccess();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String res, Throwable t) {
                Log.d("debug_audio", "update to server role. Error 1");

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable t, JSONObject obj) {
                Log.d("debug_audio", "update to server role. Erroe 2  " + String.valueOf(statusCode));
                Log.d("debug_audio", "update to server role. Erroe 2  " + t.getMessage());
                Log.d("debug_audio", "update to server role. Erroe 2  " + obj.toString());

                Log.d("debug_audio", t.getMessage());

            }
        };

        client.addHeader("Accept", "application/json");
        client.addHeader("Authorization", "Token " + user.AccessToken);
        client.post(App.getBaseURL() + "page/update_role", params, jrep);
    }


    private void switch_roles(Integer role, Integer user_id) {
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

    private void leave_channel_server_update() {
        if (is_current_user_admin || is_current_role_speaker) {
            return;
        }
        final User user = User.getLoggedInUser();
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        params.add("event_id", String.valueOf(selected_event_id));
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

            mRtmClient.logout(new ResultCallback<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d("debug_data", "RTM logout success");
                }

                @Override
                public void onFailure(ErrorInfo errorInfo) {

                }
            });

        } catch (Exception ex) {
            Log.d("debug_data", "RTM cleanup issue");
        }

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Activity){
            activity=(FragmentActivity) context;
        }
    }

    @Override
    public void onDestroy() {
        activity.unregisterReceiver(broadCastNewMessage);
        super.onDestroy();
    }




    private void loadRoomFragment() {
        unloadFragmentBottom();
        if (!is_room_fragment_loaded) {
            Bundle args = new Bundle();
            args.putString("update_type", "LIST_CHANGE");
       //     args.putParcelableArrayList("speakers_list", speakers_list);
            args.putBoolean("is_current_role_speaker", is_current_role_speaker);
            args.putString("event_title", selected_channel_display_name);
            args.putBoolean("is_current_user_admin", is_current_user_admin);;
            args.putBoolean("is_muted", is_muted);
            args.putInt("event_id", selected_event_id);
            Fragment fr = new RoomDisplayFragment();
            FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
            ft.setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_top);
            fr.setArguments(args);
            ft.add(R.id.fragment_holder, fr);
            ft.commit();
            is_room_fragment_loaded = true;
        }
    }


    private void loadFragmentWithoutupdate(Fragment fragment_to_start) {
            FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
            ft.add(R.id.fragment_holder, fragment_to_start);
            ft.commit();
    }



    @Override
    public void onTagClick(int index) {
        selected_channel = all_event_channel_name.get(index);
        if (all_event_ids.get(index).equals(selected_event_id)) {
            loadRoomFragment();
            return;

        }
        selected_event_id = all_event_ids.get(index);

        selected_channel_display_name = all_event_display_name.get(index);
            if (checkPermission(activity)) {
                processAndConnectToAChannel(selected_channel, selected_event_id);
            }
    }


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d("debug_audio", "on save instance called");
    }



    private void processAndConnectToAChannel(String channel_name, Integer event_id) {
        UsersInRoom.deleteAllRecords();
        UserSettings.deleteAll();

        Log.d("debug_audio", "speaker list clear");
        leave_channel_server_update();
        process_leave_channel();
        final User user = User.getLoggedInUser();
        show_busy_indicator();
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        params.add("channel_name", channel_name);
        params.add("channel_id", String.valueOf(event_id));

        JsonHttpResponseHandler jrep = new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    Log.d("debug_data", "Insde token processing");
                    String agora_rtc_token = response.getString("agora_token");
                    String agora_rtm_token = response.getString("agora_rtm_token");
                    Integer current_role_id = response.getInt("my_current_role");

                    if (current_role_id.equals(0)) {
                        is_current_role_speaker = Boolean.TRUE;
                        is_current_user_admin = Boolean.TRUE;
                    } else if (current_role_id.equals(1)) {
                        is_current_role_speaker = Boolean.TRUE;
                        is_current_user_admin = Boolean.FALSE;
                    } else {
                        is_current_role_speaker = Boolean.FALSE;
                        is_current_user_admin = Boolean.FALSE;
                    }

                    initAgoraEngineAndJoinChannel(channel_name, agora_rtc_token, agora_rtm_token);
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

    void show_busy_indicator() {
        busy.setVisibility(View.VISIBLE);
    }

    void hide_busy_indicator() {

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //Your code
                busy.setVisibility(View.INVISIBLE);
            }
        });
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

        leaveRTMChannel();
        Log.d("debug_channel", "Leave channel success");
    }

    void fetch_all_events() {
        final User user = User.getLoggedInUser();
        Log.d("debug_audio", "fetch all events");
        show_busy_indicator();
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();

        JsonHttpResponseHandler jrep = new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    ArrayList<String> all_event_names, all_channel_names;
                    ArrayList<Integer> all_ids;
                    all_channel_names = new ArrayList<>();
                    all_event_names = new ArrayList<>();
                    all_ids = new ArrayList<>();

                    JSONArray all_info = response.getJSONArray("all_events");
                    for(int i = 0; i < all_info.length(); i++) {
                        JSONObject event = all_info.getJSONObject(i);
                        String event_title = event.getString("event_title");
                        String event_channel_name = event.getString("event_channel_name");
                        Integer event_id = event.getInt("event_id");
                        all_event_names.add(event_title);
                        all_channel_names.add(event_channel_name);
                        all_ids.add(event_id);
                    }

                    all_event_display_name = all_event_names;
                    all_event_channel_name = all_channel_names;
                    all_event_ids = all_ids;
                    all_feeds.clear();
                    all_feeds.addAll(all_event_display_name);
                    adapter.notifyDataSetChanged();


                    hide_busy_indicator();
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
        client.post(App.getBaseURL() + "page/all_events", params, jrep);
    }

    void fetch_all_invites_count() {
        final User user = User.getLoggedInUser();
        show_busy_indicator();
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();

        JsonHttpResponseHandler jrep = new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    Integer available_invites_count = response.getInt("invites_count");
                    User user = User.getLoggedInUser();
                    user.InvitesCount = available_invites_count;
                    user.save();

                    if (user.InvitesCount > 0) {
                        String s = "You can send " + String.valueOf(user.InvitesCount) + " invites now !";
                        invites_count_display.setText(s);
                    }
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
        client.post(App.getBaseURL() + "registration/get_available_invites", params, jrep);
    }


    void process_mute_unmute() {
        Boolean new_state = !is_muted;
        server_update(User.getLoggedInUserID(), String.valueOf(selected_event_id), is_muted ? "unmute" : "mute", new ServerCallBack() {
            @Override
            public void onSuccess() {
                if (is_muted) {
                    mRtcEngine.muteLocalAudioStream(false);
                    is_muted = false;
                } else {
                    mRtcEngine.muteLocalAudioStream(true);
                    is_muted = true;
                }
            }
        });

        UsersInRoom.changeMuteState(User.getLoggedInUserID(), new_state);

        // update the list of speakers
//        speakers_list.get(User.getLoggedInUserID()).IsMuted = is_muted;
    }


    private void loadFragmentInBottom() {
        if (!is_bottom_sheet_visible) {
            Bundle args = new Bundle();
            args.putBoolean("is_current_role_speaker", is_current_role_speaker);
            args.putBoolean("is_muted", is_muted);
            args.putString("event_title", selected_channel_display_name);
            Fragment frg = new BottomFragment();
            FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
            frg.setArguments(args);
            ft.replace(R.id.fragment_bottom, frg, "bottom_sheet");
            ft.commit();
            is_bottom_sheet_visible = true;
        }
    }

    private void unloadFragmentBottom() {
        if (activity != null) {
            Fragment fragment = activity.getSupportFragmentManager().findFragmentByTag("bottom_sheet");
            if(fragment != null) {
                activity.getSupportFragmentManager().beginTransaction().remove(fragment).commit();
                is_bottom_sheet_visible = false;
            }
        }
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


    void showDialogExit() {
        String txt = "You are part of the room with active conversation";

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(txt);
        builder.setTitle("Active Conversation");
        builder.setPositiveButton("Don't Leave", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {

                // Do nothing, but close the dialog
                dialog.dismiss();
            }
        });

        builder.setNegativeButton("Leave", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                // Do nothing
                processExit();
                activity.finish();
                dialog.dismiss();
            }
        });
        builder.show();
    }


    @Override
    public boolean onBackPressed() {
        //save the state of the client
        showDialogExit();
//        processExit();
        return true;
    }
}