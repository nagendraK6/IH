package com.relylabs.InstaHelo;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.Transition;
import androidx.transition.TransitionInflater;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
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
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.relylabs.InstaHelo.Utils.Logger;
import com.relylabs.InstaHelo.models.EventElement;
import com.relylabs.InstaHelo.models.User;
import com.relylabs.InstaHelo.BottomFragment;
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


public class MainScreenFragment extends Fragment implements NewsFeedAdapter.ItemClickListener {

    BroadcastReceiver broadCastNewMessage = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String user_action =  intent
                    .getStringExtra("user_action");
            switch (user_action) {
                case "LEAVE_CHANNEL":
                    process_leave_channel();
                    unloadFragmentBottom();
                    is_room_fragment_loaded = false;
                    break;

                case "MAKE_SPEAKER":
                    Integer uid =  intent
                            .getIntExtra("uid", -1);
                    send_message("MAKE_SPEAKER", String.valueOf(uid));
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
    private boolean is_current_role_speaker = false;
    Boolean is_current_user_admin = false;

    NewsFeedAdapter adapter;
    RecyclerView news_feed_list;

    SpinKitView busy;
    String selected_channel = "";
    String selected_channel_display_name = "";
    ArrayList<String> all_event_display_name;
    ArrayList<String> all_event_channel_name;
    ArrayList<String> all_feeds;
    Boolean is_bottom_sheet_visible = false;
    Boolean is_room_fragment_loaded = false;
    Boolean is_muted = false;


    private RtcEngine mRtcEngine;
    private RtmClient mRtmClient;
    private RtmChannel mRtmChannel;

    TextView invites_count_display;
    Boolean show_inviter_card = true;

    HashMap<Integer, UsersInRoom> speakers_list;
    HashMap<Integer, UsersInRoom>  channel_users;

    void send_message(String action, String uid) {
        RtmMessage action_message = mRtmClient.createMessage();
        action_message.setText(uid);
        mRtmChannel.sendMessage(action_message, new ResultCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
            }

            @Override
            public void onFailure(ErrorInfo errorInfo) {
            }
        });
    }

    private RtmChannelListener mRtmChannelListener = new RtmChannelListener() {

        @Override
        public void onMemberCountUpdated(int i) {
            Log.d("debug_data", "onMemberCountUpdated  Count " + String.valueOf(i));
        }

        @Override
        public void onAttributesUpdated(List<RtmChannelAttribute> list) {

        }

        @Override
        public void onMessageReceived(RtmMessage message, RtmChannelMember fromMember) {
            String uid_str = message.getText();
            Integer speaker_id_to_convert = Integer.parseInt(uid_str);
            if (User.getLoggedInUser().equals(speaker_id_to_convert)) {
                // change the current user role
                mRtcEngine.setClientRole(Constants.CLIENT_ROLE_BROADCASTER);
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
            Log.d("debug_audio", "onMemberJoined");
                Log.d("debug_audio", "joined RTM channel " + member.getUserId() + " Channel id " + member.getChannelId());
              //  if (member.getChannelId().equals(selected_channel)) {

            // if already part of speakers don't add into the channel users
                    if (!speakers_list.containsKey(Integer.parseInt(member.getUserId()))) {
                        Log.d("debug_audio", "Added to all channel users. User id added " + member.getUserId());
                        channel_users.put(Integer.parseInt(member.getUserId()), new UsersInRoom(Boolean.FALSE, Boolean.FALSE, Integer.parseInt(member.getUserId())));
                        Log.d("debug_audio", "Channel Users size " + String.valueOf(channel_users.size()));

                        broadcastfrommain();
                 //   } else {
                   //     Log.d("debug_audio", "Ignored add channed users to be added " + member.getUserId());
                  //  }
                }
        }

        @Override
        public void onMemberLeft(RtmChannelMember member) {
           // if (member.getChannelId().equals(selected_channel)) {
                if (channel_users.containsKey(Integer.parseInt(member.getUserId()))) {
                    channel_users.remove(Integer.parseInt(member.getUserId()));
                    broadcastfrommain();
                }
          //  }

            Log.d("debug_audio", "left RTM channel " + member.getUserId() + " Channel id " + member.getChannelId());
        }

        private void broadcastfrommain() {
            Bundle data_bundle = new Bundle();
            Log.d("debug_audio", "Broadcasting to new fragment " + String.valueOf(getAllUsersInRoomFromList()));
            data_bundle.putParcelableArrayList("speakers_list", getSpeakerUsersInRoomFromList());
            data_bundle.putParcelableArrayList("all_users_list", getAllUsersInRoomFromList());
            data_bundle.putBoolean("is_current_role_speaker", is_current_role_speaker);
            data_bundle.putBoolean("is_current_user_admin", is_current_user_admin);;
            data_bundle.putString("event_title", selected_channel_display_name);
            data_bundle.putBoolean("is_muted", is_muted);
            Intent intent = new Intent("update_from_main");
            intent.putExtras(data_bundle);
            getActivity().sendBroadcast(intent);
        }
    };

    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {

        // Listen for the onUserOffline callback.
        // This callback occurs when the remote user leaves the channel or drops offline.
        @Override
        public void onUserOffline(final int uid, final int reason) {
            Log.d("debug_audio", "Speaker Offline " + String.valueOf(uid) + " Reason " + String.valueOf(reason));
            if (speakers_list.containsKey(uid)) {
                speakers_list.remove((uid));
                broadcastfrommain();
            }
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
            Log.d("debug_audio", "Speaker Joined" + String.valueOf(uid));
            if (!speakers_list.containsKey(uid)) {
                HashMap<Integer, UsersInRoom> new_user  = new HashMap<>();
                speakers_list.put(uid, new UsersInRoom(Boolean.TRUE, Boolean.TRUE, uid));
                broadcastfrommain();
            }
        }


        @Override
        public void onRemoteAudioStateChanged(int uid, int state, int reason, int elapsed) {
            Log.d("debug_audio", "mute/unmute " + String.valueOf(uid) + " Reason : " + String.valueOf(reason) + " State : " + String.valueOf(state));
            if (state == Constants.REMOTE_AUDIO_STATE_STARTING) {
                if (speakers_list.containsKey(uid)) {
                    speakers_list.get((uid)).IsMuted = Boolean.FALSE;
                    broadcastfrommain();
                }
            }

            if (reason == Constants.REMOTE_AUDIO_REASON_REMOTE_UNMUTED) {
                if (speakers_list.containsKey(uid)) {
                    speakers_list.get((uid)).IsMuted = Boolean.FALSE;
                    broadcastfrommain();
                }
            }

            if (reason == Constants.REMOTE_AUDIO_REASON_REMOTE_MUTED) {
                if (speakers_list.containsKey(uid)) {
                    speakers_list.get((uid)).IsMuted = Boolean.TRUE;
                    broadcastfrommain();
                }
            }

            super.onRemoteAudioStateChanged(uid, state, reason, elapsed);
        }

        void broadcastfrommain() {
            Bundle data_bundle = new Bundle();
            data_bundle.putParcelableArrayList("speakers_list", getSpeakerUsersInRoomFromList());
            data_bundle.putParcelableArrayList("all_users_list", getAllUsersInRoomFromList());
            data_bundle.putBoolean("is_current_role_speaker", is_current_role_speaker);
            data_bundle.putBoolean("is_current_user_admin", is_current_user_admin);;
            data_bundle.putString("event_title", selected_channel_display_name);
            data_bundle.putBoolean("is_muted", is_muted);
            Intent intent = new Intent("update_from_main");
            intent.putExtras(data_bundle);
            getActivity().sendBroadcast(intent);
        }
    };

    public void initRTM() {
        try {
            mRtmClient = RtmClient.createInstance(getActivity(), getString(R.string.agora_app_id),
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
        getActivity().registerReceiver(broadCastNewMessage, new_post);
        busy = view.findViewById(R.id.loading_channel_token_fetch);
        initRTM();


        all_event_display_name = new ArrayList<>();
        all_event_channel_name = new ArrayList<>();
        all_feeds = new ArrayList<>();
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        news_feed_list = view.findViewById(R.id.news_feed_list);
        // Create adapter passing in the sample user data
        adapter = new NewsFeedAdapter(getActivity(), all_feeds);
        adapter.setClickListener(this);

        // Attach the adapter to the recyclerview to populate items
        news_feed_list.setAdapter(adapter);
        PreCachingLayoutManager layoutManager = new PreCachingLayoutManager(getActivity());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        layoutManager.setExtraLayoutSpace(DeviceUtils.getScreenHeight(getActivity()));
        news_feed_list.setLayoutManager(layoutManager);
        //news_feed_list.setOnClickListener();

        CircleImageView img = view.findViewById(R.id.user_profile_image);
        User user = User.getLoggedInUser();
        if (!user.ProfilePicURL.equals("")) {
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

        initRTM();
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
            processAndConnectToAChannel(selected_channel);
        } else {
            Toast.makeText(getActivity(), "Permission is needed to start audio call", Toast.LENGTH_LONG).show();
        }
    }

    private void initAgoraEngineAndJoinChannel(String channel_name, String agora_rtc_token, String agora_rtm_token) {
        initializeAgoraEngine();
        joinChannelRTC(channel_name, agora_rtc_token, agora_rtm_token);
    }

    private void initializeAgoraEngine() {
        try {

           // is_current_role_speaker = true;
            if (is_current_user_admin) {
                is_current_role_speaker = Boolean.TRUE;
            }
            mRtcEngine = RtcEngine.create(getActivity(), getString(R.string.agora_app_id), mRtcEventHandler);
            //mRtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
            mRtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
            mRtcEngine.setAudioProfile(Constants.AUDIO_PROFILE_MUSIC_HIGH_QUALITY_STEREO, Constants.AUDIO_SCENARIO_MEETING);
            mRtcEngine.setDefaultAudioRoutetoSpeakerphone(true);
            if (is_current_role_speaker) {
                mRtcEngine.setClientRole(Constants.CLIENT_ROLE_BROADCASTER);
            } else {
                mRtcEngine.setClientRole(Constants.CLIENT_ROLE_AUDIENCE);
            }
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
        if (is_current_role_speaker) {
            speakers_list.put(user.UserID, new UsersInRoom(Boolean.TRUE, Boolean.TRUE, user.UserID));
        }

        channel_users.put(user.UserID, new UsersInRoom(Boolean.FALSE, Boolean.TRUE, user.UserID));

        Log.d("debug_audio", "Added to all channel users. User id added " + String.valueOf(user.UserID));
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


//        hide_busy_indicator();
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

                mRtmChannel.getMembers(new ResultCallback<List<RtmChannelMember>>() {
                    @Override
                    public void onSuccess(List<RtmChannelMember> rtmChannelMembers) {
                        Log.d("debug_audio", "Channel users list. Success");
                        for (int i  =0 ;i < rtmChannelMembers.size(); i++) {
                            Log.d("debug_audio", "Channel member id " + rtmChannelMembers.get(i).getUserId());

                            channel_users.put(Integer.parseInt(rtmChannelMembers.get(i).getUserId()), new UsersInRoom(Boolean.FALSE, Boolean.FALSE, Integer.parseInt(rtmChannelMembers.get(i).getUserId())));

                        }

                        try {
                            // This is to delay the flickering
                            if (speakers_list.size() == 0) {
                                Thread.sleep(1000);
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        hide_busy_indicator();
                        loadRoomFragment();
                    }

                    @Override
                    public void onFailure(ErrorInfo errorInfo) {
                        Log.d("debug_audio", "Channel users list. Error " + errorInfo.toString());
                    }
                });

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
    public void onDestroy() {
        super.onDestroy();
    }


    ArrayList<UsersInRoom> getSpeakerUsersInRoomFromList() {
        return new ArrayList<UsersInRoom>(speakers_list.values());
    }

    ArrayList<UsersInRoom> getAllUsersInRoomFromList() {
        Log.d("debug_audio", "Channel users size in broascast " + String.valueOf(channel_users.size()));
        return new ArrayList<UsersInRoom>(channel_users.values());
    }

    private void loadRoomFragment() {
        unloadFragmentBottom();
        if (!is_room_fragment_loaded) {
            Bundle args = new Bundle();
            args.putParcelableArrayList("speakers_list", getSpeakerUsersInRoomFromList());
            args.putParcelableArrayList("all_users_list", getAllUsersInRoomFromList());
            args.putBoolean("is_current_role_speaker", is_current_role_speaker);
            args.putString("event_title", selected_channel_display_name);
            args.putBoolean("is_current_user_admin", is_current_user_admin);;
            args.putBoolean("is_muted", is_muted);
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
            selected_channel_display_name = all_event_display_name.get(index);
            if (checkPermission(getActivity())) {
                processAndConnectToAChannel(selected_channel);
            }
    }

    private void processAndConnectToAChannel(String channel_name) {
        process_leave_channel();
        final User user = User.getLoggedInUser();
        show_busy_indicator();
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        params.add("channel_name", channel_name);

        speakers_list = new HashMap<>();
        channel_users = new HashMap<>();

        JsonHttpResponseHandler jrep = new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    Log.d("debug_data", "Insde token processing");
                    String agora_rtc_token = response.getString("agora_token");
                    String agora_rtm_token = response.getString("agora_rtm_token");
                    initAgoraEngineAndJoinChannel(channel_name, agora_rtc_token, agora_rtm_token);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String res, Throwable t) {
                WeakHashMap<String, String> log_data = new WeakHashMap<>();
                log_data.put(Logger.STATUS, Integer.toString(statusCode));
                log_data.put(Logger.RES, res);
                log_data.put(Logger.THROWABLE, t.toString());
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable t, JSONObject obj) {
                WeakHashMap<String, String> log_data = new WeakHashMap<>();
                log_data.put(Logger.STATUS, Integer.toString(statusCode));
                log_data.put(Logger.JSON, obj.toString());
                log_data.put(Logger.THROWABLE, t.toString());
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

        getActivity().runOnUiThread(new Runnable() {
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
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        leaveRTMChannel();
    }

    void fetch_all_events() {
        final User user = User.getLoggedInUser();
        show_busy_indicator();
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();

        ArrayList<String> all_event_names, all_channel_names;
        all_channel_names = new ArrayList<>();
        all_event_names = new ArrayList<>();


        JsonHttpResponseHandler jrep = new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    ArrayList<String> all_event_names, all_channel_names;
                    all_channel_names = new ArrayList<>();
                    all_event_names = new ArrayList<>();

                    JSONArray all_info = response.getJSONArray("all_events");
                    for(int i = 0; i < all_info.length(); i++) {
                        JSONObject event = all_info.getJSONObject(i);
                        String event_title = event.getString("event_title");
                        String event_channel_name = event.getString("event_channel_name");
                        all_event_names.add(event_title);
                        all_channel_names.add(event_channel_name);
                    }

                    all_event_display_name = all_event_names;
                    all_event_channel_name = all_channel_names;

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
                WeakHashMap<String, String> log_data = new WeakHashMap<>();
                log_data.put(Logger.STATUS, Integer.toString(statusCode));
                log_data.put(Logger.RES, res);
                log_data.put(Logger.THROWABLE, t.toString());
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable t, JSONObject obj) {
                WeakHashMap<String, String> log_data = new WeakHashMap<>();
                log_data.put(Logger.STATUS, Integer.toString(statusCode));
                log_data.put(Logger.JSON, obj.toString());
                log_data.put(Logger.THROWABLE, t.toString());
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
                //WeakHashMap<String, String> log_data = new WeakHashMap<>();
                //log_data.put(Logger.STATUS, Integer.toString(statusCode));
                //log_data.put(Logger.RES, res);
                //log_data.put(Logger.THROWABLE, t.toString());
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable t, JSONObject obj) {
                //WeakHashMap<String, String> log_data = new WeakHashMap<>();
                //log_data.put(Logger.STATUS, Integer.toString(statusCode));
                //log_data.put(Logger.JSON, obj.toString());
               // log_data.put(Logger.THROWABLE, t.toString());
            }
        };

        client.addHeader("Accept", "application/json");
        client.addHeader("Authorization", "Token " + user.AccessToken);
        client.post(App.getBaseURL() + "registration/get_available_invites", params, jrep);
    }


    void process_mute_unmute() {
        if (is_muted) {
            mRtcEngine.muteLocalAudioStream(false);
            is_muted = false;
        } else {
            mRtcEngine.muteLocalAudioStream(true);
            is_muted = true;
        }

        // update the list of speakers
        speakers_list.get(User.getLoggedInUserID()).IsMuted = is_muted;
    }


    private void loadFragmentInBottom() {
        if (!is_bottom_sheet_visible) {
            Bundle args = new Bundle();
            args.putBoolean("is_current_role_speaker", is_current_role_speaker);
            args.putBoolean("is_muted", is_muted);
            args.putString("event_title", selected_channel_display_name);
            Fragment frg = new BottomFragment();
            FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
            frg.setArguments(args);
            ft.replace(R.id.fragment_bottom, frg, "bottom_sheet");
            ft.commit();
            is_bottom_sheet_visible = true;
        }
    }

    private void unloadFragmentBottom() {
        Fragment fragment = getActivity().getSupportFragmentManager().findFragmentByTag("bottom_sheet");
        if(fragment != null) {
            getActivity().getSupportFragmentManager().beginTransaction().remove(fragment).commit();
            is_bottom_sheet_visible = false;
        }
    }
}