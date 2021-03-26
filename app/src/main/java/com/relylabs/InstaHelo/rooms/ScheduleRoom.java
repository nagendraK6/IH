package com.relylabs.InstaHelo.rooms;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.relylabs.InstaHelo.App;
import com.relylabs.InstaHelo.MainScreenFragment;
import com.relylabs.InstaHelo.R;
import com.relylabs.InstaHelo.Utils.Constants;
import com.relylabs.InstaHelo.Utils.RoomHelper;
import com.relylabs.InstaHelo.followerList.FollowerListAdapter;
import com.relylabs.InstaHelo.models.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;

import cz.msebera.android.httpclient.Header;

import com.relylabs.InstaHelo.Utils.Helper;

public class ScheduleRoom extends Fragment {

    ScheduleRoomSpeakerAdapter adapter;
    RecyclerView recyclerView;
    View fragment_view;
    private ArrayList<String> names = new ArrayList<String>();
    private  ArrayList<String> usernames = new ArrayList<String>();
    private  ArrayList<String> img = new ArrayList<String>();
    private  ArrayList<String> user_ids = new ArrayList<>();
    Boolean has_just_created = false;
    ImageView whatsapp, facebook, twitter;
    String room_title;
    final Calendar myCalendar = Calendar.getInstance();

    TextView title;
    TextView date_and_time_schedule;
    TextView speaker_list;
    String room_slug;
    String myFormat;
    SimpleDateFormat sdf;
    String time_share;
    ImageView copy_btn;
    
    FragmentActivity activity_ref;
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Activity){
            activity_ref=(FragmentActivity) context;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_schedule_room, container, false);
    }

    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        myFormat = "E, dd MMM yyyy hh:mm a z"; //In which you need put here
        sdf = new SimpleDateFormat(myFormat);
        room_slug = getArguments().getString("room_slug");
        if (getArguments().containsKey("has_just_created")) {
            has_just_created = Boolean.TRUE;
            TextView new_room_info = view.findViewById(R.id.new_room_info);
            new_room_info.setVisibility(View.VISIBLE);
        }

        fragment_view = view;
        title = fragment_view.findViewById(R.id.schdeule_title);
        date_and_time_schedule = fragment_view.findViewById(R.id.date_and_time_schedule);
        speaker_list = fragment_view.findViewById(R.id.speaker_list);
        room_title = "";

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
      //  ImageView back = view.findViewById(R.id.back_button_schedule);
        //TextView all_rooms = view.findViewById(R.id.all_room);
        ImageView go_to_all_rooms = view.findViewById(R.id.go_to_all_rooms);
        getData(room_slug);
        /*back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RoomHelper.ask_main_for_refresh_content(activity_ref);
                cleanup_composer_is_needed();
                Helper.removefragment(activity_ref);
            }
        });
        all_rooms.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RoomHelper.ask_main_for_refresh_content(activity_ref);
                cleanup_composer_is_needed();
                Helper.removefragment(activity_ref);
            }
        });*/
        go_to_all_rooms.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RoomHelper.ask_main_for_refresh_content(activity_ref);
                cleanup_composer_is_needed();
                Helper.removefragment(activity_ref);
            }
        });

        copy_btn = view.findViewById(R.id.copy_btn2);
        copy_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LayoutInflater inflater = getLayoutInflater();
                Helper.showToast(activity_ref,view,inflater,"Link copied to clipboard!", R.drawable.toast_background);
                String shareBody =
                        "Hey! checkout this audio room " + room_title  + " on @instahelo app. Join me at " + time_share + " \n" +
                                "Download from https://play.google.com/store/apps/details?id=com.relylabs.InstaHelo . Click here for more details : " + App.getBaseURL() + room_slug ;
                ClipboardManager clipboard = (ClipboardManager) activity_ref.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("InstaHelo", shareBody);
                clipboard.setPrimaryClip(clip);
            }
        });

        TextView addToCalendar = view.findViewById(R.id.add_to_calendar2);
        addToCalendar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Helper.addToCalendar(activity_ref,myCalendar,room_title, room_slug);
            }
        });
    }

    public void shareEvent(String package_name){
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        if (Helper.isAppInstalled(activity_ref,package_name)) {
            sharingIntent.setPackage(package_name);
        }

        if (package_name.equals("com.whatsapp")) {
            room_title = "*" + room_title + "*";
        } else {
            room_title = "'" + room_title + "'";
        }

        sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Instahelo Room");
        String shareBody =
                "Hey! checkout this audio room " + room_title  + " on @instahelo app. Join me at " + time_share + " \n" +
                        "Download from https://play.google.com/store/apps/details?id=com.relylabs.InstaHelo . Click here for more details : " + App.getBaseURL() + room_slug ;
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(sharingIntent, "Share via"));
    }

    void getData(String room_slug){
        ProgressBar busy = fragment_view.findViewById(R.id.loading_channel_token_fetch8);
        busy.setVisibility(View.VISIBLE);

        final User user = User.getLoggedInUser();
        AsyncHttpClient client = new AsyncHttpClient();
        boolean running = false;
        RequestParams params = new RequestParams();
        params.add("room_slug",room_slug);
        JsonHttpResponseHandler jrep = new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                busy.setVisibility(View.INVISIBLE);
                try {
                    Log.d("response_follow", response.toString());

                    room_title = response.getString("title");
                    long timestamp = response.getLong("schedule_time");
                    JSONArray speaker_list_json = response.getJSONArray("speakers_list");
                    String sp_list = "Speakers: ";
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
                    title.setText(room_title);
                    myCalendar.setTimeInMillis(timestamp);
                    time_share = sdf.format(myCalendar.getTime()).toUpperCase();
                    Date d = new Date((long)timestamp);
                    String dateToStr = DateFormat.getDateInstance().format(d);
                    String timeToStr = DateFormat.getTimeInstance(DateFormat.SHORT).format(d);
                    date_and_time_schedule.setText(dateToStr + ", " + timeToStr);
                    prepareRecyclerView();
                }
                catch (JSONException e) {
                    e.printStackTrace();
                    Helper.removefragment(activity_ref);
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
        int layout_size = usernames.size() <= 3 ? usernames.size() : 3;
        GridLayoutManager gm = new GridLayoutManager(fragment_view.getContext(), layout_size);
        gm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                Log.d("debug_c", "Span size check" +String.valueOf(position));
                return 1;
            }
        });
        recyclerView.setLayoutManager(gm);
        adapter = new ScheduleRoomSpeakerAdapter(getContext(), names, usernames, img,user_ids);
        recyclerView.setAdapter(adapter);
    }

    void cleanup_composer_is_needed() {
        if (has_just_created) {
            Helper.removeFragmentWithTag(activity_ref, Constants.FRAGMENT_CREATE_ROOM_B);
            Helper.removeFragmentWithTag(activity_ref, Constants.FRAGMENT_CREATE_ROOM_A);
        }
    }
}