package com.relylabs.InstaHelo;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.os.Bundle;
import android.widget.Toast;

import com.github.ybq.android.spinkit.SpinKitView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.shape.CornerFamily;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.relylabs.InstaHelo.HandRaise.HandRaiseUsersListDialogFragment;
import com.relylabs.InstaHelo.Utils.RoomHelper;
import com.relylabs.InstaHelo.models.User;
import com.relylabs.InstaHelo.models.UserSettings;
import com.relylabs.InstaHelo.models.UsersInRoom;
import com.relylabs.InstaHelo.rooms.RoomsUsersDisplayListAdapter;
import com.relylabs.InstaHelo.rooms.RoomsUsersDisplayListDiffsCallback;
import com.relylabs.InstaHelo.sharing.SharingContactListAdapter;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import cz.msebera.android.httpclient.Header;
import de.hdodenhof.circleimageview.CircleImageView;

import com.relylabs.InstaHelo.Utils.Helper;


public class RoomDisplayFragment extends Fragment implements RoomsUsersDisplayListAdapter.ItemClickListener, IOnBackPressed  {

    ArrayList<UsersInRoom> speakers;
    ArrayList<UsersInRoom> audiences;

    RecyclerView recyclerView_s, recyclerView_a;

    ProgressBar busy;

    RoomsUsersDisplayListAdapter speaker_adapter, audience_adapter;

    TextView hand_raise_admin, hand_raise_audience;
    ImageView mute_unmute_button_bottom;
    ImageView wa_sharing;

    private FragmentActivity activity;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Activity){
            activity=(FragmentActivity) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.activity = null;
    }

    BroadcastReceiver broadCastNewMessage = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String update_type = intent.getStringExtra("update_type");
            Log.d("debug_audio", "Received update from the main: " + update_type);

            if (update_type.equals("LIST_CHANGE")) {
                incrementalAdd();
                processMuteUnmuteSettings();
                fetchListenersData();
                setCurrentImageHandRaise();
            }


            if (update_type.equals("MUTE_UNMUTE")) {
                Integer uid = intent
                        .getIntExtra("user_id", -1);
                Boolean muted = intent.getBooleanExtra("is_muted", true);
                Integer index  = -1;
                for (int i = 0; i < speakers.size(); i++) {
                    if (speakers.get(i).UserId.equals(uid)) {
                        index = i;
                    }
                }

                if (index > -1) {
                    speakers.get(index).IsMuted = muted;
                    updateItem(index, muted);
                }

                setCurrentImageHandRaise();
            }

            if (update_type.equals("CONNECTION_CHANGE")) {
                fetchListenersData();
            }

            if (update_type.equals("REFRESH_SETTINGS")) {
                processMuteUnmuteSettings();
                fetchListenersData();
                setCurrentImageHandRaise();
            }

            if (update_type.equals("EXIT_ROOM")) {
                broadcastLocalUpdate("LEAVE_CHANNEL");
                Helper.removefragment(activity);
            }
        }
    };


    BroadcastReceiver broadcastReceiverFromService = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String update_type = intent.getStringExtra("update_type");
            Log.d("debug_audio", "Received update from the main: " + update_type);

            if (update_type.equals("LIST_CHANGE")) {
                incrementalAdd();
                processMuteUnmuteSettings();
                fetchListenersData();
                setCurrentImageHandRaise();
            }

            if (update_type.equals("VOLUME_INDICATOR")) {
                ArrayList<Integer> uids = intent.getIntegerArrayListExtra("uids");
                ArrayList<Integer> speaking_on_off = intent.getIntegerArrayListExtra("speaking_on_off");
                updateItems(uids, speaking_on_off);
            }


            if (update_type.equals("MUTE_UNMUTE")) {
                Integer uid = intent
                        .getIntExtra("user_id", -1);
                Boolean muted = intent.getBooleanExtra("is_muted", true);
                Integer index  = -1;
                for (int i = 0; i < speakers.size(); i++) {
                    if (speakers.get(i).UserId.equals(uid)) {
                        index = i;
                    }
                }

                if (index > -1) {
                    speakers.get(index).IsMuted = muted;
                    updateItem(index, muted);
                }

                setCurrentImageHandRaise();
            }

            if (update_type.equals("CONNECTION_CHANGE")) {
                fetchListenersData();
            }

            if (update_type.equals("REFRESH_SETTINGS")) {
                processMuteUnmuteSettings();
                fetchListenersData();
                setCurrentImageHandRaise();
            }

            if (update_type.equals("EXIT_ROOM")) {
                broadcastLocalUpdate("LEAVE_CHANNEL");
                Helper.removefragment(activity);
            }
        }
    };


    private  void updateItem(Integer index, Boolean muted) {
        Bundle diff = new Bundle();
        diff.putBoolean("IsMuted", muted);
        speaker_adapter.notifyItemChanged(index, diff);
    }

    private void update_speaking_indicator(Integer index, Boolean is_speaking) {
        Bundle diff = new Bundle();
        diff.putBoolean("IsSpeaking", is_speaking);
        speaker_adapter.notifyItemChanged(index, diff);
    }

    private void updateItems(ArrayList<Integer> uids, ArrayList<Integer> speaking_on_off) {
        for (int i  = 0; i < uids.size(); i++) {
            Integer index = get_speaker_index(uids.get(i));
            if (index > -1) {
                if (speaking_on_off.get(i) == 1) {
                    update_speaking_indicator(index, true);
                } else {
                    update_speaking_indicator(index, false);
                }
            }
        }
    }

    private  Integer get_speaker_index(Integer uid) {
        for (int i = 0; i < speakers.size(); i++) {
            if (speakers.get(i).UserId.equals(uid)) {
                return i;
            }
        }

        return -1;
    }



    private void incrementalAdd() {
        ArrayList<UsersInRoom> new_list = UsersInRoom.getAllSpeakers();
        final RoomsUsersDisplayListDiffsCallback diffCallback = new RoomsUsersDisplayListDiffsCallback(speakers, new_list);
        final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
        speakers.clear();
        speakers.addAll(new_list);
        diffResult.dispatchUpdatesTo(speaker_adapter);
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_room_display, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d("debug_activity", "OnView Created called RoomDisplayFragment");

        ImageView invite = view.findViewById(R.id.invite);
        invite.setVisibility(View.INVISIBLE);


        ImageView notif = view.findViewById(R.id.notification);
        notif.setVisibility(View.INVISIBLE);

        ShapeableImageView img = view.findViewById(R.id.user_profile_image);
        float radius = this.activity.getResources().getDimension(R.dimen.default_corner_radius_profile);
        img.setShapeAppearanceModel(img.getShapeAppearanceModel()
                .toBuilder()
                .setTopRightCorner(CornerFamily.ROUNDED,radius)
                .setTopLeftCorner(CornerFamily.ROUNDED,radius)
                .setBottomLeftCorner(CornerFamily.ROUNDED,radius)
                .setBottomRightCorner(CornerFamily.ROUNDED,radius)
                .build());


        User user = User.getLoggedInUser();
        if (!user.ProfilePicURL.equals("")) {
            Picasso.get().load(user.ProfilePicURL).into(img);
        }

        View v = view.findViewById(R.id.minimize_room);
        v.setVisibility(View.VISIBLE);
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                broadcastLocalUpdate("MINIMISED");
                Helper.removefragment(activity);
            }
        });

        View exit_room = view.findViewById(R.id.leave_quitely);
        mute_unmute_button_bottom = view.findViewById(R.id.mute_unmute_button_bottom);

        exit_room.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                show_busy_indicator();
                broadcastLocalUpdate("LEAVE_CHANNEL");
                Helper.removefragment(activity);
            }
        });

        IntentFilter new_post = new IntentFilter("update_from_main");
        if (activity != null) {
            activity.registerReceiver(broadCastNewMessage, new_post);
        }


        IntentFilter update_from_service = new IntentFilter("update_from_service");
        if (activity != null) {
            activity.registerReceiver(broadcastReceiverFromService, update_from_service);
        }




        audiences = new ArrayList<>();
        speakers = new ArrayList<>();
        speakers = UsersInRoom.getAllSpeakers();

        String event_title = UserSettings.getSettings().selected_channel_display_name;
        TextView title_of_room = view.findViewById(R.id.title_of_room);
        title_of_room.setText(event_title);
        mute_unmute_button_bottom.setVisibility(View.VISIBLE);

        mute_unmute_button_bottom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UserSettings us = UserSettings.getSettings();
                if (us.is_muted) {
                    us.is_muted = false;
                } else {
                    us.is_muted = true;
                }
                us.save();

                User lg_user = User.getLoggedInUser();
                int index_impacted = -1;
                for (int i = 0; i < speakers.size(); i++) {
                    if (speakers.get(i).UserId.equals(lg_user.UserID)) {
                        speakers.get(i).IsMuted =  us.is_muted;
                        index_impacted = i;
                    }
                }

                updateItem(index_impacted,  us.is_muted);
                // speaker_adapter.notifyDataSetChanged();
                processMuteUnmuteSettings();


                broadcastLocalUpdate("MUTE_UNMUTE_CLICK");
            }
        });

        UserSettings us = UserSettings.getSettings();

        hand_raise_admin = view.findViewById(R.id.hand_raise_admin);
        hand_raise_audience = view.findViewById(R.id.hand_raise_audience);

        hand_raise_admin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processHandRaiseClick();
            }
        });

        hand_raise_audience.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processHandRaiseClick();
            }
        });

        setCurrentImageHandRaise();


        processMuteUnmuteSettings();
        // speakers = UsersInRoom.getAllSpeakers();
        setupSpeakers(view);
        setupAudiences(view);
        // updatePostingDetails(t);
        fetchListenersData();
        busy = view.findViewById(R.id.loading_channel_token_fetch);

        wa_sharing = view.findViewById(R.id.wa_room_sharing);
        wa_sharing.setVisibility(View.VISIBLE);
        wa_sharing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                if (RoomHelper.IsWAInstalled(activity)) {
                    sharingIntent.setPackage("com.whatsapp");
                }

                String speakers_text = " with ";
                Boolean found = false;
                for (int i = 0; i < Math.min(speakers.size(), 5); i++) {
                    if (!user.UserID.equals(speakers.get(i).UserId)) {
                        speakers_text = speakers_text + speakers.get(i).Name + ", ";
                        found = true;
                    }
                }

                if (!found) {
                    speakers_text = "";
                } else {
                    speakers_text = speakers_text.replaceAll(", $", "");
                }

                    sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Instahelo Room");
                    String shareBody =

                            "I am in an audio room discussing " + event_title  +  speakers_text +  " on @instahelo app. Join me: \n" +
                            "https://play.google.com/store/apps/details?id=com.relylabs.InstaHelo";
                    sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
                    startActivity(Intent.createChooser(sharingIntent, "Share via"));
            }
        });

        super_user_controls(view);
    }



    void processMuteUnmuteSettings() {
        UserSettings us = UserSettings.getSettings();
        if (us.is_current_role_speaker) {
            mute_unmute_button_bottom.setVisibility(View.VISIBLE);
            if (us.is_muted) {
                if (activity != null) {
                    mute_unmute_button_bottom.setBackground(activity.getDrawable(R.drawable.mic_off_self_user));
                }
            } else {
                if (activity != null) {
                    mute_unmute_button_bottom.setBackground(activity.getDrawable(R.drawable.mic_on_room_view));
                }
            }
        } else {
            mute_unmute_button_bottom.setVisibility(View.INVISIBLE);
        }
    }

    private  void setupAudiences(View view) {
        UserSettings us = UserSettings.getSettings();
        recyclerView_a = view.findViewById(R.id.listener_grid_list);
        recyclerView_a.setLayoutManager(new GridLayoutManager(getContext(), 3));
        audience_adapter = new RoomsUsersDisplayListAdapter(getContext(), audiences, us.is_current_user_admin);
        audience_adapter.setClickListener(this);
        recyclerView_a.setAdapter(audience_adapter);
    }


    private  void setupSpeakers(View view) {
        UserSettings us = UserSettings.getSettings();
        recyclerView_s = view.findViewById(R.id.speaker_grid_list);
        recyclerView_s.setLayoutManager(new GridLayoutManager(getContext(), 3));
        speaker_adapter = new RoomsUsersDisplayListAdapter(getContext(), speakers, us.is_current_user_admin);
        speaker_adapter.setClickListener(this);
        recyclerView_s.setAdapter(speaker_adapter);
    }

    private  void broadcastLocalUpdate(String action) {
        Bundle data_bundle = new Bundle();
        data_bundle.putString("user_action", action);
        Intent intent = new Intent("update_from_room");
        intent.putExtras(data_bundle);
        if (activity != null) {
            activity.sendBroadcast(intent);
        }
    }



    @Override
    public void onDestroy() {
        activity.unregisterReceiver(broadCastNewMessage);
        activity.unregisterReceiver(broadcastReceiverFromService);
        super.onDestroy();
    }

    @Override
    public void onItemClick(Integer uid, String action) {
        Log.d("debug_audio", action);
        if (action.equals("SHOW_PROFILE")) {
            show_profile(uid);
            return;
        }

        Bundle data_bundle = new Bundle();
        data_bundle.putString("user_action", action);
        data_bundle.putInt("uid", uid);
        Intent intent = new Intent("update_from_room");
        intent.putExtras(data_bundle);
        if (activity != null) {
            activity.sendBroadcast(intent);
        }
    }

    private void fetchListenersData() {
        checkConnection();
        User user = User.getLoggedInUser();
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        UserSettings us = UserSettings.getSettings();
        params.add("event_id", String.valueOf(us.selected_event_id));


        JsonHttpResponseHandler jrep= new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    String error_message = response.getString("error_message");
                    if (error_message.equals("SUCCESS")) {
                        JSONArray all_data = response.getJSONArray("all_audiences");
                        ArrayList<UsersInRoom> all_audiences = new ArrayList<>();
                        Log.d("debug_data", "Audience Count " + String.valueOf(all_audiences.size()));
                        for (int i  = 0 ; i < all_data.length(); i++) {
                            JSONObject obj = all_data.getJSONObject(i);
                            all_audiences.add(new UsersInRoom(
                                    Boolean.FALSE,
                                    Boolean.TRUE,
                                    obj.getInt("id"),
                                    obj.getString("name"),
                                    obj.getString("profile_image_url")
                            ));
                        }

                        audiences.clear();
                        audiences.addAll(all_audiences);
                        audience_adapter.notifyDataSetChanged();
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
        client.post( App.getBaseURL() + "page/get_audiences", params, jrep);
    }



    private  void checkConnection() {
        if (activity != null) {
            ConnectivityManager cm = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            if (null != activeNetwork) {
                if(activeNetwork.getType() != ConnectivityManager.TYPE_WIFI &&
                        activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                    Toast.makeText(getContext(), "Internet has poor connectivity", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(getContext(), "Internet has poor connectivity", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void super_user_controls(View v) {
        ShapeableImageView user_profile_image = v.findViewById(R.id.user_profile_image);
        User user =User.getLoggedInUser();
        if (user.IsSuperUser) {
            user_profile_image.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final CharSequence[] items = { "+Add", "-Remove", "Close Room", "Cancel" };
                    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                    builder.setItems(items, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int item) {
                            if (activity == null) {
                                return;
                            }

                            if (items[item].equals("+Add")) {
                                process_additional_users(true);
                            }else if (items[item].equals("Close Room")) {
                                process_start_close_room_request();
                            } else if (items[item].equals("Cancel")) {
                                dialog.dismiss();
                            } else if (items[item].equals("-Remove")) {
                                process_additional_users(false);
                            }
                        }
                    });
                    builder.show();
                }
            });
        }
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


    private void setCurrentImageHandRaise() {
        UserSettings us  = UserSettings.getSettings();

        if (us.is_current_user_admin && us.audience_hand_raised) {
            hand_raise_admin.setVisibility(View.VISIBLE);
            hand_raise_admin.setBackground(activity.getDrawable(R.drawable.raise_hand_with_notification));
            hand_raise_audience.setVisibility(View.INVISIBLE);
        }

        if (us.is_current_user_admin && !us.audience_hand_raised) {
            hand_raise_admin.setVisibility(View.VISIBLE);
            hand_raise_admin.setBackground(activity.getDrawable(R.drawable.raise_hand_without_notification));
            hand_raise_audience.setVisibility(View.INVISIBLE);
        }

        if (!us.is_current_role_speaker && us.is_self_hand_raised) {
            hand_raise_admin.setVisibility(View.INVISIBLE);
            hand_raise_audience.setBackground(activity.getDrawable(R.drawable.hand_down_big));
            hand_raise_audience.setVisibility(View.VISIBLE);
        }

        if (!us.is_current_role_speaker && !us.is_self_hand_raised) {
            hand_raise_admin.setVisibility(View.INVISIBLE);
            hand_raise_audience.setBackground(activity.getDrawable(R.drawable.hand_raise_big));
            hand_raise_audience.setVisibility(View.VISIBLE);
        }

        if (!us.is_current_user_admin && us.is_current_role_speaker) {
            hand_raise_audience.setVisibility(View.INVISIBLE);
            hand_raise_admin.setVisibility(View.INVISIBLE);
        }
     }

    private void processHandRaiseClick() {
        UserSettings us = UserSettings.getSettings();
        if (us.is_current_user_admin) {
            HandRaiseUsersListDialogFragment bottomSheetDialog = new HandRaiseUsersListDialogFragment();
            Bundle bundle = new Bundle();
            bundle.putInt("event_id", us.selected_event_id);
            bottomSheetDialog.setArguments(bundle);

            bottomSheetDialog.show(getFragmentManager(), "TAG");
            us.audience_hand_raised = false;
            us.save();
        } else if (!us.is_current_role_speaker &&  !us.is_self_hand_raised) {
            send_raise_hand_request();
            us.is_self_hand_raised = true;
            us.save();
        } else if (!us.is_current_role_speaker &&  us.is_self_hand_raised) {
            send_raise_hand_request_clear();
            us.is_self_hand_raised = false;
            us.save();
        }

        setCurrentImageHandRaise();
    }



    private void send_raise_hand_request() {
        broadcastLocalUpdate("MAKE_SPEAKER_REQUEST");
    }


    private void send_raise_hand_request_clear() {
        broadcastLocalUpdate("HAND_RAISE_CLEAR");
    }


    @Override
    public boolean onBackPressed() {
        broadcastLocalUpdate("MINIMISED");
        Helper.removefragment(activity);
        return true;
    }

    public void show_profile(Integer uid) {
        OtherProfile otherprof = new OtherProfile();
        Bundle args = new Bundle();
        args.putString("user_id",String.valueOf(uid));
        otherprof.setArguments(args);
        FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
        ft.add(R.id.fragment_holder, otherprof);
        ft.commitAllowingStateLoss();
    }

    public void process_start_close_room_request() {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage("You are going to close the room. Are you sure you want to do this?");
        builder.setTitle("Warning");
        builder.setPositiveButton("No", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {

                // Do nothing, but close the dialog
                dialog.dismiss();
            }
        });

        builder.setNegativeButton("Yes", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                // Do nothing
                RoomHelper.send_request_to_exit_everyone(activity);
                dialog.dismiss();
            }
        });
        builder.show();
    }

    public void process_additional_users(Boolean should_add) {
        checkConnection();
        User user = User.getLoggedInUser();
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        UserSettings us = UserSettings.getSettings();
        params.add("event_id", String.valueOf(us.selected_event_id));
        String url_str = should_add ? "page/room_add_optional_user" : "page/room_remove_optional_user";

        JsonHttpResponseHandler jrep= new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                fetchListenersData();
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
        client.post( App.getBaseURL() + url_str, params, jrep);
    }
}