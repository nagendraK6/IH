package com.relylabs.InstaHelo.rooms;

import android.app.Activity;
import android.content.Context;
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
import android.widget.TextView;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.relylabs.InstaHelo.App;
import com.relylabs.InstaHelo.MainScreenFragment;
import com.relylabs.InstaHelo.R;
import com.relylabs.InstaHelo.followerList.FollowerListAdapter;
import com.relylabs.InstaHelo.models.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.ArrayList;
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
        String room_slug = getArguments().getString("room_slug");
        Log.d("room_slug",room_slug);
        fragment_view = view;
        ImageView back = view.findViewById(R.id.back_button_schedule);
        TextView all_rooms = view.findViewById(R.id.all_room);
        ImageView go_to_all_rooms = view.findViewById(R.id.go_to_all_rooms);
        getData(room_slug);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Helper.removefragment(activity_ref);
            }
        });
        all_rooms.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Helper.removefragment(activity_ref);
            }
        });
        go_to_all_rooms.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Helper.removefragment(activity_ref);
            }
        });

    }
    void getData(String room_slug){
        final User user = User.getLoggedInUser();
        AsyncHttpClient client = new AsyncHttpClient();
        boolean running = false;
        RequestParams params = new RequestParams();
        params.add("room_slug",room_slug);
        JsonHttpResponseHandler jrep = new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    Log.d("response_follow", response.toString());
                    TextView title = fragment_view.findViewById(R.id.schdeule_title);
                    TextView date_schedule = fragment_view.findViewById(R.id.date_schedule);
                    TextView time_schedule = fragment_view.findViewById(R.id.time_schedule);
                    TextView speaker_list = fragment_view.findViewById(R.id.speaker_list);
                    String title_json = response.getString("title");
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
                    title.setText(title_json);
                    Date d = new Date((long)timestamp);
                    String dateToStr = DateFormat.getDateInstance().format(d);
                    String timeToStr = DateFormat.getTimeInstance(DateFormat.SHORT).format(d);
                   // Log.d("date",dateToStr);
                  //  Log.d("time",timeToStr);
                  //  String[] arr = dateToStr.split("-");
                  //  dateToStr = arr[0]+ " "+ arr[1] + ", " + arr[2];
                   // String[] arr2 = DateFormat.getTimeInstance(DateFormat.LONG).format(d).split(" ");
                   // timeToStr += " (" + arr2[2] + ")";
                    date_schedule.setText(dateToStr);
                    time_schedule.setText(timeToStr);
                    prepareRecyclerView();

//                    adapter.notifyDataSetChanged();

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

}