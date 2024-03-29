package com.relylabs.InstaHelo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.os.Bundle;
import android.widget.Toast;

import com.github.ybq.android.spinkit.SpinKitView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.shape.CornerFamily;
import com.loopj.android.http.AsyncHttpClient;

import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.relylabs.InstaHelo.Utils.Helper;
import com.relylabs.InstaHelo.Utils.RoomHelper;
import com.relylabs.InstaHelo.bottomsheet.BottomScheduleRoom;
import com.relylabs.InstaHelo.models.EventCardUserElement;
import com.relylabs.InstaHelo.models.EventElement;
import com.relylabs.InstaHelo.models.User;
import com.relylabs.InstaHelo.models.UserSettings;
import com.relylabs.InstaHelo.models.UsersInRoom;
import com.relylabs.InstaHelo.notification.NotificationList;
import com.relylabs.InstaHelo.rooms.RoomCreateBottomSheetDialogFragment;
import com.relylabs.InstaHelo.rooms.RoomsSpeakerAcceptRejectRequestDialogFragment;
import com.relylabs.InstaHelo.services.ActiveRoomService;
import com.relylabs.InstaHelo.sharing.ExploreFragment;
import com.relylabs.InstaHelo.sharing.SendInviteFragment;
import com.squareup.picasso.Picasso;

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

import io.agora.rtc.models.UserInfo;


public class MainScreenFragment extends Fragment implements NewsFeedAdapter.ItemClickListener, IOnBackPressed {
    private static final int PERMISSION_REQ_ID_RECORD_AUDIO = 0;
    private static final int PERMISSION_REQ_ID_RECORD_AUDIO_INIT = 1;
    ImageView notification_btn;
    View fragment_view;
    NewsFeedAdapter active_rooms_adapter, scheduled_rooms_adapter;
    RecyclerView active_rooms_list, schedule_rooms_list;
    ImageView invite = null;

    ProgressBar busy;

    TextView invites_count_display;
    Boolean show_inviter_card = true;
    Integer min_no_of_followers_needed = 0;
    private FragmentActivity activity;
    ArrayList<EventElement> all_active_rooms, all_scheduled_rooms;
    Boolean is_room_fragment_loaded = false;
    TextView uc;
    
    /// this is what service returns to the end  main fragment and main fragment decides.
    // .room fragment to load

    BroadcastReceiver broadcastReceiverFromService = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String user_action =  intent
                    .getStringExtra("update_type");
            Integer uid;
            switch (user_action) {
                case "JOINED_CONNECTION":
                    Log.d("debug_data", "Receive request to load fragment");
                    hide_busy_indicator();
                    loadRoomFragment();
                    break;

                case "SHOW_DIALOG":
                    String sender_speaker_id =  intent
                            .getStringExtra("uid");
                    show_accept_reject_dialog(sender_speaker_id);
                    break;

                case "REFRESH_FEED":
                    Log.d("debug_data", "Request for refresh feed");
                    fetch_all_events(false);
                    break;
            }
            Log.d("debug_data", "received broadcast " + user_action);
        }
    };


    // room fragment and bottom fragment sends to main fragment to change ui if needed
    BroadcastReceiver broadCastNewMessage = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String user_action =  intent
                    .getStringExtra("user_action");
            Integer uid;
            switch (user_action) {
                case "ROOM_REPORTED":
                    String toast_text = "Thanks for reporting the room. We will review the report as soon as possible and will take the appropriate action";
                    Helper.showToast(activity,fragment_view,getLayoutInflater(),toast_text,R.drawable.toast_red_background);
                    break;

                case "LEAVE_CHANNEL":
                    process_leave_channel();
                    unloadFragmentBottom();
                    fetch_all_events(false);
                    break;

                // audience sending MAKE_SPEAKER_REQUEST
                case "MAKE_SPEAKER_REQUEST":
                    RoomHelper.send_hand_raise_request_service(activity, false);
                    break;


                case "HAND_RAISE_CLEAR":
                    RoomHelper.send_hand_raise_request_service(activity, true);
                    break;


                case "ROOM_CREATE":
                    String room_title =  intent
                            .getStringExtra("room_title");
                    String room_type =  intent
                            .getStringExtra("room_type");
                    long schedule_timestamp =  intent
                            .getLongExtra("timestamp",0);
                    send_create_room_request(room_title, room_type,schedule_timestamp);
                    break;


                // admin rejecting speaker
                case "REJECT_SPEAKER":
                    uid =  intent
                            .getIntExtra("uid", -1);
                    RoomHelper.send_reject_speaker_request_admin(activity, uid);
                    break;

                // admin  making speaker
                case "MAKE_AUDIENCE":
                    uid =  intent
                            .getIntExtra("uid", -1);
                    RoomHelper.send_make_audience_request_admin(activity, uid);
                    break;


                // admin making speaker
                case "MAKE_SPEAKER":
                    uid =  intent
                            .getIntExtra("uid", -1);

                    RoomHelper.send_make_speaker_request_admin(activity, uid);
                    break;


                case "MINIMISED":
                    loadFragmentInBottom();
                    is_room_fragment_loaded = false;
                     fetch_all_events(false);
                    break;

                case "EXPAND_ROOM":
                    loadRoomFragment();
                    break;

                case "MUTE_UNMUTE_CLICK":
                    if (checkPermission(activity)) {
                        process_mute_unmute();
                    }
                    break;

                case "JOIN_ROOM":
                    Integer room_id =  intent
                            .getIntExtra("room_id", -1);
                    room_title =  intent
                            .getStringExtra("room_title");
                    String channel_name =  intent
                            .getStringExtra("channel_name");
                    start_join_room(room_id, channel_name, room_title);
                    break;


            }
            Log.d("debug_data", "received broadcast " + user_action);
        }
    };

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fargment_main_screen, container, false);
        busy = view.findViewById(R.id.loading_channel_token_fetch);

        IntentFilter new_post = new IntentFilter("update_from_room");
        activity.registerReceiver(broadCastNewMessage, new_post);

        IntentFilter service_ins = new IntentFilter("update_from_service");
        activity.registerReceiver(broadcastReceiverFromService, service_ins);

        all_active_rooms = new ArrayList<>();
        all_scheduled_rooms = new ArrayList<>();
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d("debug_activity", "OnView Created called MainScreenFragment");
        notification_btn = view.findViewById(R.id.notification);
        active_rooms_list = view.findViewById(R.id.active_rooms);
        active_rooms_adapter = new NewsFeedAdapter(activity, all_active_rooms, "ACTIVE");
        active_rooms_adapter.setClickListener(this);
        fragment_view = view;

        schedule_rooms_list = view.findViewById(R.id.schedule_rooms_list);
        scheduled_rooms_adapter = new NewsFeedAdapter(activity, all_scheduled_rooms, "SCHEDULED");
        scheduled_rooms_adapter.setClickListener(this);

        final User user = User.getLoggedInUser();
        user.UserSteps = "MAIN_SCREEN";
        user.CompletedOnboarding = true;
        user.save();

        UserSettings us = UserSettings.getSettings();
        if (us == null) {
            us = new UserSettings();
            us.UserID = User.getLoggedInUserID();
            us.save();
        }

        if (RoomHelper.isServiceRunningInForeground(activity, ActiveRoomService.class)) {
            // if service is running it means user needs to go to room
            loadRoomFragment();
        } else {
            Log.d("debug_data", "Reset the settings");
            UserSettings.deleteAll();
            us = new UserSettings();
            us.selected_event_id = -1;
            us.save();
        }

        ImageView start_a_room_cta = view.findViewById(R.id.start_a_room_cta);

        start_a_room_cta.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UserSettings us = UserSettings.getSettings();
                User u = User.getLoggedInUser();
                if (u.IsStartRoomEnabled == false) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                    builder.setTitle("Friends Invite needed");
                    builder.setMessage("Invite " + String.valueOf(min_no_of_followers_needed) + " friends in your network to unlock the ability to create a room. Invite now?");
                    builder.setPositiveButton("Let's do it", new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {

                            // Do nothing, but close the dialog
                            Helper.loadFragment(new SendInviteFragment(),activity);
                            dialog.dismiss();
                        }
                    });

                    builder.show();
                    return;
                }

                if (us.is_current_user_admin && !us.selected_event_id.equals(-1)) {
                    showDialogForCurrentRoomExit("Action Moderator", "You are an active moderator of a room. To create the room you have to leave the conversation. Do you want to leave the room before creating new room?");
                } else if (!us.selected_event_id.equals(-1)) {
                    showDialogForCurrentRoomExit("Action Conversation", "Active conversation in progress. Do you want to leave the room before creating new room?");
                } else {
                    RoomHelper.showDialogRoomCreate(activity);
                }
            }
        });


        schedule_rooms_list.setAdapter(scheduled_rooms_adapter);
        PreCachingLayoutManager layoutManager = new PreCachingLayoutManager(activity);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        layoutManager.setExtraLayoutSpace(DeviceUtils.getScreenHeight(activity));
        schedule_rooms_list.setLayoutManager(layoutManager);

        PreCachingLayoutManager activelayoutManager = new PreCachingLayoutManager(activity);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        layoutManager.setExtraLayoutSpace(DeviceUtils.getScreenHeight(activity));


        active_rooms_list.setAdapter(active_rooms_adapter);
        active_rooms_list.setLayoutManager(activelayoutManager);


        ShapeableImageView img = view.findViewById(R.id.user_profile_image);
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
        } else {
            img.setBackground(activity.getDrawable(R.drawable.empty_user_profile_image));
        }

        img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OtherProfile otherprof = new OtherProfile();
                Bundle args = new Bundle();
                args.putString("user_id",String.valueOf(user.UserID));
                otherprof.setArguments(args);
//                loadFragmentWithoutupdate(otherprof);
                Helper.loadFragment(otherprof,activity);
            }
        });

        ImageView search_icon = view.findViewById(R.id.search_icon);
        search_icon.setVisibility(View.VISIBLE);
        search_icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                loadFragmentWithoutupdate(new ExploreFragment());
                Helper.loadFragment(new ExploreFragment(),activity);
            }
        });

        fetch_all_events(true);

        invite = view.findViewById(R.id.invite);
        invite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideInvitesView(view);
//                loadFragmentWithoutupdate(new SendInviteFragment());
                Helper.loadFragment(new SendInviteFragment(),activity);
            }
        });

        View invited_view = view.findViewById(R.id.invite_card);
        invited_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });

        invites_count_display = view.findViewById(R.id.invites_count_display);
        fetch_all_invites_count();


        if (user.ShowWelcomeScreen) {
            changeViewToShow(view);
            user.ShowWelcomeScreen = Boolean.FALSE;
            user.save();
            show_inviter_card = false;
        } else {
            hideInvitesView(view);
        }


        notification_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                loadFragmentWithoutupdate(new NotificationList());
                notification_btn.setImageResource(R.drawable.notification_bell_icon);
                Helper.loadFragment(new NotificationList(),activity);
            }
        });

        ImageView sch_rooms = view.findViewById(R.id.schedule_rooms);
        sch_rooms.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                loadFragmentWithoutupdate(new ScheduleRoomList());
                Helper.loadFragment(new ScheduleRoomList(),activity);
            }
        });

        uc = view.findViewById(R.id.uc);
        handler.postDelayed(runnable, 10000);
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


    @Override
    public void onResume() {
        super.onResume();
        fetch_all_events(false);
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
        activity.unregisterReceiver(broadcastReceiverFromService);

        // send message to service for shutdown
        super.onDestroy();
    }






    private Integer tappedRoomId = -9;



     @Override
     public void onRoomClick(int index, String list_type) {
         ArrayList<EventElement> all_feeds = list_type.equals("ACTIVE") ? all_active_rooms : all_scheduled_rooms;

        if (list_type.equals("SCHEDULED")) {
            // show the bottom sheet
            BottomScheduleRoom bottomSheet =
                    BottomScheduleRoom.newInstance();

            Bundle bundle = new Bundle();
            bundle.putString("room_slug", all_feeds.get(index).roomSlug );
            bottomSheet.setArguments(bundle);
            bottomSheet.show(getFragmentManager(),
                    BottomScheduleRoom.TAG);
            return;
        }

         start_join_room(all_feeds.get(index).eventID, all_feeds.get(index).eventChannelName, all_feeds.get(index).eventTitle);
    }

    public void start_join_room(Integer newRoomId, String channelName, String roomTitle) {
        tappedRoomId = newRoomId;
        UserSettings us = UserSettings.getSettings();
        if (tappedRoomId != -1 && tappedRoomId == us.selected_event_id) {
            loadRoomFragment();
            return;
        }

        us.selected_channel_name  =  channelName;
        us.selected_channel_display_name = roomTitle;
        us.save();

        show_busy_indicator();

        if (checkPermissionInitial(activity)) {
            if (tappedRoomId != us.selected_event_id && us.selected_event_id != -1) {
                // user switching channel...
                // don't save room id as the service will use to leave the channel and set in service
                RoomHelper.send_channel_switch(activity, us.selected_channel_name, us.selected_channel_display_name, tappedRoomId);
            } else {
                us.selected_event_id = tappedRoomId;
                us.save();
                RoomHelper.sendRoomServieStartRequest(activity, us.selected_channel_name, us.selected_channel_display_name, us.selected_event_id);
            }
        }
    }


    private final Handler handler = new Handler();

    private Runnable runnable = new Runnable() {
        public void run() {
            Log.d("debug_c", "Hiding the display");
            hideInvitesView(fragment_view);
        }
    };


    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d("debug_audio", "on save instance called");
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



    void fetch_all_events(boolean with_indicator) {
        final User user = User.getLoggedInUser();
        Log.d("debug_audio", "fetch all events");
        if (with_indicator) {
            show_busy_indicator();
        }
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        params.add("version",getVersion());
        JsonHttpResponseHandler jrep = new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    JSONObject current_user_info = response.getJSONObject("current_user_info");
                    User user = User.getLoggedInUser();
                    user.IsSuperUser = current_user_info.getBoolean("is_super_user");
                    user.FirstName = current_user_info.getString("first_name");
                    user.LastName = current_user_info.getString("last_name");
                    user.ProfilePicURL = current_user_info.getString("profile_pic_url");
                    user.IsStartRoomEnabled = current_user_info.getBoolean("IsStartRoomEnabled");
                    min_no_of_followers_needed = current_user_info.getInt("min_no_of_invites_needed");
                    user.InvitedUsersCount = current_user_info.getInt("invited_users_count");
                    user.save();
                    Boolean should_show_notification_dot = current_user_info.getBoolean("should_show_notification_dot");
                    if(should_show_notification_dot){
                        notification_btn.setImageResource(R.drawable.notification_dot);
                    }

                    JSONArray all_active_rooms_t = response.getJSONArray("all_active_rooms");
                    JSONArray all_scheduled_rooms_t = response.getJSONArray("all_scheduled_rooms");

                    ArrayList<EventElement> active_room_data = new ArrayList<>();
                    ArrayList<EventElement> scheduled_room_data = new ArrayList<>();
                    active_room_data = get_rooms_data_from_json(all_active_rooms_t);
                    scheduled_room_data = get_rooms_data_from_json(all_scheduled_rooms_t);

                    all_active_rooms.clear();
                    all_scheduled_rooms.clear();
                    if (scheduled_room_data.size() == 0) {
                        uc.setVisibility(View.INVISIBLE);
                    } else {
                        uc.setVisibility(View.VISIBLE);
                    }
                    all_active_rooms.addAll(active_room_data);
                    all_scheduled_rooms.addAll(scheduled_room_data);
                    active_rooms_adapter.notifyDataSetChanged();
                    scheduled_rooms_adapter.notifyDataSetChanged();
                    if (with_indicator) {
                        hide_busy_indicator();
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
        client.post(App.getBaseURL() + "page/all_events_with_scheduled", params, jrep);
    }


    ArrayList<EventElement>  get_rooms_data_from_json(JSONArray all_info) throws JSONException {
        ArrayList<EventElement> all_event_data = new ArrayList<>();
        for(int i = 0; i < all_info.length(); i++) {
            JSONObject event = all_info.getJSONObject(i);
            String event_title = event.getString("event_title");
            String event_channel_name = event.getString("event_channel_name");
            Integer event_id = event.getInt("event_id");
            ArrayList<String> photo_urls = new ArrayList<>();
            ArrayList<EventCardUserElement> all_users_in_card = new ArrayList<>();
            JSONArray event_photo_urls = event.getJSONArray("event_photo_urls");
            JSONArray event_card_users = event.getJSONArray("event_card_users");
            for (int j = 0; j < event_photo_urls.length(); j++) {
                photo_urls.add(event_photo_urls.getString(j));
            }

            for (int j = 0; j < event_card_users.length(); j++) {
                EventCardUserElement u = new EventCardUserElement();
                JSONObject obj = event_card_users.getJSONObject(j);
                u.IsSpeaker = obj.getBoolean("is_speaker");
                u.Name = obj.getString("name");
                all_users_in_card.add(u);
            }

            Boolean hasStarted = event.getBoolean("has_started");
            Boolean isScheduled = event.getBoolean("is_scheduled");
            Boolean isAdmin = event.getBoolean("is_room_admin");

            EventElement e = new EventElement();
            e.eventChannelName = event_channel_name;
            e.eventTitle = event_title;
            e.eventID = event_id;
            e.eventPhotoUrls = photo_urls;
            e.userElements = all_users_in_card;
            if (isScheduled) {
                String event_room_slug = event.getString("event_room_slug");
                long timestamp = event.getLong("timestamp");
                e.roomSlug = event_room_slug;
                e.scheduleTimestamp = timestamp;
            }

            e.isRoomAdmin = isAdmin;
            e.hasStarted = hasStarted;
            e.isScheduled = isScheduled;


            all_event_data.add(e);
        }

        return all_event_data;
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


    public boolean checkPermissionInitial(final Context context) {
        int currentAPIVersion = Build.VERSION.SDK_INT;
        if (currentAPIVersion >= android.os.Build.VERSION_CODES.M) {
            if (
                    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
            ) {
                if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, android.Manifest.permission.RECORD_AUDIO)) {
                    requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, PERMISSION_REQ_ID_RECORD_AUDIO_INIT);
                } else {
                    requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, PERMISSION_REQ_ID_RECORD_AUDIO_INIT);
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
    public void onStop() {
        super.onStop();
    }



    void showDialogExit(String title, String description, ServerCallBack cs) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(description);
        builder.setTitle(title);
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
                cs.onSuccess();
                dialog.dismiss();
            }
        });
        builder.show();
    }


    @Override
    public boolean onBackPressed() {
        String txt = "You are part of the room with active conversation";
        String desc = "Active Conversation";

        //save the state of the client
        showDialogExit(txt, desc, new ServerCallBack() {
            @Override
            public void onSuccess() {
                activity.finish();
            }
        });
        return true;
    }

    private void send_create_room_request(String title, String room_type,long timestamp) {
        final User user = User.getLoggedInUser();
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        params.add("event_title", title);
        params.add("room_type", room_type);
        if(timestamp!=0) {
            params.add("schedule_timestamp", String.valueOf(timestamp));
        }
        show_busy_indicator();

        JsonHttpResponseHandler jrep = new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                /* processExit(); */
                Integer event_id = null;
                try {
                    Log.d("response",response.toString());
                    fetch_all_events(false);
                    event_id = response.getInt("event_id");
                    String event_channel_name = response.getString("event_channel_name");
                    // send request to service for starting the room
                    /// if service is running then call switch channel
                    // if service off then start the service
                    if (RoomHelper.isServiceRunningInForeground(activity, ActiveRoomService.class)) {
                        RoomHelper.send_channel_switch(activity, event_channel_name, title,  event_id);
                    } else {
                        UserSettings us = UserSettings.getSettings();
                        us.selected_event_id = event_id;
                        us.selected_channel_display_name = title;
                        us.selected_channel_name = event_channel_name;
                        us.save();
                        RoomHelper.sendRoomServieStartRequest(activity, event_channel_name, title,  event_id);
                    }
                    /*  join_a_room_after_start(event_id, title, event_channel_name); */
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
        client.post(App.getBaseURL() + "page/create_a_room", params, jrep);
    }


    @Override
    public void onRequestPermissionsResult(int RC, String per[], int[] PResult) {
        super.onRequestPermissionsResult(RC, per, PResult);
        if (PResult.length > 0 && PResult[0] == PackageManager.PERMISSION_GRANTED) {
            if (RC ==PERMISSION_REQ_ID_RECORD_AUDIO) {
                // user tapped during the join of the call
                process_mute_unmute_with_reconnect();
            } else if (RC == PERMISSION_REQ_ID_RECORD_AUDIO_INIT) {
                UserSettings us  = UserSettings.getSettings();
                if (RoomHelper.isServiceRunningInForeground(activity, ActiveRoomService.class)) {
                    // service is running so Let's switch the channel
                    // user switching channel...
                    // don't save room id as the service will use to leave the channel and set in service
                    RoomHelper.send_channel_switch(activity, us.selected_channel_name, us.selected_channel_display_name, tappedRoomId);
                } else {
                    us.selected_event_id = tappedRoomId;
                    us.save();
                    RoomHelper.sendRoomServieStartRequest(activity, us.selected_channel_name, us.selected_channel_display_name, us.selected_event_id);
                }
            }
        } else {

            // user has denied permission so connect as usual
            if (RC == PERMISSION_REQ_ID_RECORD_AUDIO_INIT) {
                // user tapped before join the call
                UserSettings us  = UserSettings.getSettings();
                if (RoomHelper.isServiceRunningInForeground(activity, ActiveRoomService.class)) {
                    // service is running so Let's switch the channel
                    // user switching channel...
                    // don't save room id as the service will use to leave the channel and set in service
                    RoomHelper.send_channel_switch(activity, us.selected_channel_name, us.selected_channel_display_name, tappedRoomId);
                } else {
                    us.selected_event_id = tappedRoomId;
                    us.save();
                    RoomHelper.sendRoomServieStartRequest(activity, us.selected_channel_name, us.selected_channel_display_name, us.selected_event_id);
                }
            } else {
                Toast.makeText(activity, "Permission is needed to start Talking", Toast.LENGTH_LONG).show();
                UserSettings us = UserSettings.getSettings();
                us.is_muted = true;
                us.save();

                // unmute set if the user is in speaker state
                ArrayList<UsersInRoom> all_room_users = UsersInRoom.getAllSpeakers();
                for (int i = 0; i < all_room_users.size(); i++) {
                    if (all_room_users.get(i).UserId.equals(User.getLoggedInUserID())) {
                        all_room_users.get(i).IsMuted = true;
                        all_room_users.get(i).save();
                    }
                }

                // broadcast to update it's own settings
                Bundle data_bundle = new Bundle();
                data_bundle.putString("update_type", "MUTE_UNMUTE");
                data_bundle.putBoolean("is_muted", true);
                data_bundle.putInt("user_id", User.getLoggedInUserID());
                Intent intent = new Intent("update_from_main");
                intent.putExtras(data_bundle);
                activity.sendBroadcast(intent);


                data_bundle.putString("update_type", "REFRESH_SETTINGS");
                intent.putExtras(data_bundle);
                activity.sendBroadcast(intent);
            }
        }
    }



    private void showDialogForCurrentRoomExit(String title, String description) {
        showDialogExit(title, description, new ServerCallBack() {
            @Override
            public void onSuccess() {
                process_leave_channel();
                unloadFragmentBottom();
                fetch_all_events(false);
                RoomHelper.showDialogRoomCreate(activity);
            }
        });
    }

    private void loadFragmentWithoutupdate(Fragment fragment_to_start) {
        FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
        ft.add(R.id.fragment_holder, fragment_to_start);
        ft.commitAllowingStateLoss();
    }

    private void loadFragmentInBottom() {
        UserSettings us = UserSettings.getSettings();
        Bundle args = new Bundle();
        args.putString("event_title", us.selected_channel_display_name);
        args.putInt("event_id", us.selected_event_id);
        Fragment frg = new BottomFragment();
        FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
        frg.setArguments(args);
        ft.replace(R.id.fragment_bottom, frg, "bottom_sheet");
        ft.commitAllowingStateLoss();
    }

    private void unloadFragmentBottom() {
        if (activity != null) {
            Fragment fragment = activity.getSupportFragmentManager().findFragmentByTag("bottom_sheet");
            if(fragment != null) {
                activity.getSupportFragmentManager().beginTransaction().remove(fragment).commitAllowingStateLoss();
            }
        }
    }

    private void loadRoomFragment() {
        unloadFragmentBottom();
        if (!is_room_fragment_loaded) {
            UserSettings us = UserSettings.getSettings();
            Bundle args = new Bundle();
            args.putString("update_type", "LIST_CHANGE");
            args.putString("event_title", us.selected_channel_display_name);
            args.putInt("event_id", us.selected_event_id);
            Fragment fr = new RoomDisplayFragment();
            FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
            ft.setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_top);
            fr.setArguments(args);
            ft.add(R.id.fragment_holder, fr);
            is_room_fragment_loaded = true;
            ft.commitAllowingStateLoss();
        }
    }

    // send to service the mute/unmute request
    void process_mute_unmute() {
        RoomHelper.sendServiceMuteUnmute(activity);
    }

    // send the mute/unmute when user denied permission before the call
    void process_mute_unmute_with_reconnect() {
        RoomHelper.sendServiceMuteUnmuteReconnect(activity);
    }

    void process_leave_channel() {
        is_room_fragment_loaded = false;
        RoomHelper.sendServiceLeaveChannel(activity);
    }

    void show_accept_reject_dialog(String sender_speaker_id) {
        RoomsSpeakerAcceptRejectRequestDialogFragment speaker_accept_reject  =
                new RoomsSpeakerAcceptRejectRequestDialogFragment(sender_speaker_id);
        speaker_accept_reject.setCancelable(false);
        speaker_accept_reject.show(activity.getSupportFragmentManager(),
                "add_photo_dialog_fragment");
    }

    String getVersion(){
        String version = "";
        try {
            PackageInfo pInfo = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
            version = pInfo.versionName;
            int verCode = pInfo.versionCode;
            Log.d("version",version);
            Log.d("verCode",String.valueOf(verCode));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return version;
    }

}