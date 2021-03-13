package com.relylabs.InstaHelo.bottomsheet;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.relylabs.InstaHelo.App;
import com.relylabs.InstaHelo.R;


import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.relylabs.InstaHelo.RoomDisplayFragment;
import com.relylabs.InstaHelo.Utils.Helper;
import com.relylabs.InstaHelo.Utils.RoomHelper;
import com.relylabs.InstaHelo.models.User;
import com.relylabs.InstaHelo.models.UserSettings;
import com.relylabs.InstaHelo.rooms.ScheduleRoomSpeakerAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import cz.msebera.android.httpclient.Header;

import static com.relylabs.InstaHelo.Utils.Helper.removefragment;

public class BottomScheduleRoom extends BottomSheetDialogFragment {


    TextView title;
    TextView date_schedule;
    TextView speaker_list;
    ImageView action_btn;
    TextView room_not_started_text, dismiss_btn;
    ImageView copy_btn;


    public static final String TAG = "ActionBottomDialog";
    SpeakerAdapter adapter;
    RecyclerView recyclerView;
    final Calendar myCalendar = Calendar.getInstance();
    View fragment_view;
    public Boolean isRoomAdmin = false;
    public Boolean hasStarted = false;
    private ArrayList<String> names = new ArrayList<String>();
    private  ArrayList<String> usernames = new ArrayList<String>();
    private  ArrayList<String> img = new ArrayList<String>();
    private  ArrayList<String> user_ids = new ArrayList<>();
    
    public static BottomScheduleRoom newInstance() {
        return new BottomScheduleRoom();
    }
    FragmentActivity activity_ref;
    int event_id;
    String title_main;
    String channelName;
    String room_slug;
    String time_share;
    SimpleDateFormat sdf;
    String myFormat;
    ImageView whatsapp, facebook, twitter;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Activity){
            activity_ref=(FragmentActivity) context;
        }
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bottom_schedule_room, container, false);
    }

    @Override
    public int getTheme() {
        return R.style.AppBottomSheetDialogTheme;
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        myFormat = "E, dd MMM yyyy hh:mm a z"; //In which you need put here
        sdf = new SimpleDateFormat(myFormat);
        fragment_view = view;
        room_slug = this.getArguments().getString("room_slug");
        Log.d("room_slug",room_slug);
        getData(room_slug);
        copy_btn = view.findViewById(R.id.copy_btn);
        copy_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                LayoutInflater inflater = getLayoutInflater();
                Helper.showToast(activity_ref,view,inflater,"Link copied to clipboard!");
                String shareBody =
                        "Hey! checkout this audio room " + title_main  + " on @instahelo app. Join me at " + time_share + " \n" +
                                "Download from https://play.google.com/store/apps/details?id=com.relylabs.InstaHelo . Click here for more details : " + App.getBaseURL() + room_slug ;
                ClipboardManager clipboard = (ClipboardManager) activity_ref.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("InstaHelo", shareBody);
                clipboard.setPrimaryClip(clip);
            }
        });
        TextView addToCalendar = view.findViewById(R.id.add_to_calendar);
        addToCalendar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Helper.addToCalendar(activity_ref,myCalendar,title_main);
            }
        });


        action_btn = view.findViewById(R.id.imageView5);
        title = fragment_view.findViewById(R.id.shcedule_topic);
        date_schedule = fragment_view.findViewById(R.id.shcedule_time);
        speaker_list = fragment_view.findViewById(R.id.hosts);

        room_not_started_text = fragment_view.findViewById(R.id.room_not_started_text);
        dismiss_btn = fragment_view.findViewById(R.id.dismiss_btn);
        dismiss_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        action_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isRoomAdmin && !hasStarted){
                    // updates to server that room start and join the room.
                    sendStartNotiServer();
                }
                else if(isRoomAdmin && hasStarted){
                    joinTheRoom();
                    dismiss();
                }
                else if (!isRoomAdmin && !hasStarted){
                    Log.d("room_not_started","log");
                }
                else if(!isRoomAdmin && hasStarted){
                    joinTheRoom();
                    dismiss();
                }
            }
        });

        whatsapp = view.findViewById(R.id.wa);
        twitter = view.findViewById(R.id.twitter);
        facebook = view.findViewById(R.id.fb);
        whatsapp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareEvent("com.whatsapp");
            }
        });
        twitter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareEvent("com.twitter.android");
            }
        });
        facebook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareEvent("com.facebook.katana");
            }
        });
    }

    public void shareEvent(String package_name){
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        if (Helper.isAppInstalled(activity_ref,package_name)) {
            sharingIntent.setPackage(package_name);
        }
        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Instahelo Room");
        String shareBody =
                "Hey! checkout this audio room " + title_main  + " on @instahelo app. Join me at " + time_share + " \n" +
                        "Download from https://play.google.com/store/apps/details?id=com.relylabs.InstaHelo . Click here for more details : " + App.getBaseURL() + room_slug ;
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(sharingIntent, "Share via"));
    }

    public void sendStartNotiServer(){
        final User user = User.getLoggedInUser();
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        params.add("event_id",String.valueOf(event_id));
        ProgressBar busy = fragment_view.findViewById(R.id.loading_channel_token_fetch9);
        busy.setVisibility(View.VISIBLE);
        JsonHttpResponseHandler jrep = new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    busy.setVisibility(View.INVISIBLE);
                    Log.d("response", response.toString());
                    String error_message = response.getString("error_message");
                    joinTheRoom();
                    dismiss();

                }
                catch (JSONException e) {
                    e.printStackTrace();
                   dismiss();
                }

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable t, JSONObject obj) {
            }
        };

        client.addHeader("Accept", "application/json");
        client.addHeader("Authorization", "Token " + user.AccessToken);
        client.post(App.getBaseURL() + "page/start_schedule_room", params, jrep);
    }

    public void joinTheRoom(){
        // send the notification to main screen fragment and it will process as a normal room click
        Bundle data_bundle = new Bundle();
        data_bundle.putString("user_action", "JOIN_ROOM");
        data_bundle.putInt("room_id", event_id);
        data_bundle.putString("channel_name", channelName);
        data_bundle.putString("room_title", title_main);

        Intent intent = new Intent("update_from_room");
        intent.putExtras(data_bundle);
        if (activity_ref != null) {
            activity_ref.sendBroadcast(intent);
        }
    }

    void getData(String room_slug){
        final User user = User.getLoggedInUser();
        AsyncHttpClient client = new AsyncHttpClient();
        boolean running = false;
        RequestParams params = new RequestParams();
        params.add("room_slug",room_slug);
        ProgressBar busy = fragment_view.findViewById(R.id.loading_channel_token_fetch9);
        LinearLayout sharing = fragment_view.findViewById(R.id.sharing);
        busy.setVisibility(View.VISIBLE);
        JsonHttpResponseHandler jrep = new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {

                    busy.setVisibility(View.INVISIBLE);
                    sharing.setVisibility(View.VISIBLE);
                    Log.d("response_follow", response.toString());


                    String title_json = response.getString("title");
                    title_main = title_json;
                    int id = response.getInt("event_id");
                    event_id = id;
                    String chanelName = response.getString("event_channel_name");
                    channelName = chanelName;
                    isRoomAdmin = response.getBoolean("is_room_admin");
                    hasStarted = response.getBoolean("has_started");
                    long timestamp = response.getLong("schedule_time");


                    if (isRoomAdmin && hasStarted == false) {
                        action_btn.setBackground(activity_ref.getDrawable(R.drawable.start_scheduled_room));
                    } else if (hasStarted) {
                        action_btn.setBackground(activity_ref.getDrawable(R.drawable.join_room_in_progress));
                    } else {
                        action_btn.setVisibility(View.INVISIBLE);
                        room_not_started_text.setVisibility(View.VISIBLE);
                        dismiss_btn.setVisibility(View.VISIBLE);
                    }

                    myCalendar.setTimeInMillis(timestamp);
                    time_share = sdf.format(myCalendar.getTime()).toUpperCase();
                    String myFormat = "E, dd MMM yyyy hh:mm a"; //In which you need put here
                    SimpleDateFormat sdf = new SimpleDateFormat(myFormat);
                    date_schedule.setText(sdf.format(myCalendar.getTime()).toUpperCase());
                    JSONArray speaker_list_json = response.getJSONArray("speakers_list");


                    String sp_list = "With ";
                    for(int i=0;i<speaker_list_json.length();i++){
                        Log.d("array",speaker_list_json.get(i).toString());
                        JSONObject temp = (JSONObject) speaker_list_json.get(i);
                        names.add(temp.get("name").toString());
                        usernames.add(temp.get("display_username").toString());
                        img.add(temp.get("image").toString());
                        user_ids.add(temp.get("user_id").toString());
                        sp_list += temp.get("name").toString() + ", ";
                    }
                    speaker_list.setText(sp_list.substring(0,sp_list.length()-2));
                    title.setText(title_json);
                    prepareRecyclerView();
                }
                catch (JSONException e) {
                    e.printStackTrace();
                    removefragment(activity_ref);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable t, JSONObject obj) {
            }
        };

        client.addHeader("Accept", "application/json");
        client.addHeader("Authorization", "Token " + user.AccessToken);
        client.post(App.getBaseURL() + "page/get_schdule_room_info", params, jrep);
    }

    void prepareRecyclerView() {
        recyclerView = fragment_view.findViewById(R.id.speaker_grid_list);
        recyclerView.setLayoutManager( new GridLayoutManager(getContext(), 5));
        adapter = new SpeakerAdapter(getContext(), names, usernames, img,user_ids);
        recyclerView.setAdapter(adapter);
    }
    @Override
    public void onDetach() {
        super.onDetach();
    }
}