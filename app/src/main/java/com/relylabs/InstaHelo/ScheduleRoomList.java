package com.relylabs.InstaHelo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.relylabs.InstaHelo.Utils.Constants;
import com.relylabs.InstaHelo.Utils.RoomHelper;
import com.relylabs.InstaHelo.bottomsheet.BottomScheduleRoom;
import com.relylabs.InstaHelo.followerList.FollowerListAdapter;
import com.relylabs.InstaHelo.models.EventCardUserElement;
import com.relylabs.InstaHelo.models.EventElement;
import com.relylabs.InstaHelo.models.User;
import com.relylabs.InstaHelo.models.UserSettings;
import com.relylabs.InstaHelo.rooms.ScheduleRoom;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;

import static com.relylabs.InstaHelo.Utils.Helper.loadFragment;
import static com.relylabs.InstaHelo.Utils.Helper.loadFragmentAdapter;
import static com.relylabs.InstaHelo.Utils.Helper.removefragment;

public class ScheduleRoomList extends Fragment implements NewsFeedAdapter.ItemClickListener,IOnBackPressed {


    FragmentActivity activity_ref;
    View fragment_view;
    RecyclerView recyclerView;
    NewsFeedAdapter adapter;
    ArrayList<EventElement> all_feeds;
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
        all_feeds = new ArrayList<>();
        IntentFilter new_post = new IntentFilter("update_from_room");
        activity_ref.registerReceiver(broadCastNewMessage, new_post);
        return inflater.inflate(R.layout.fragment_schedule_room_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        fragment_view = view;

        ImageView back = view.findViewById(R.id.back_btn);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removefragment(activity_ref);
            }
        });
        fetch_all_events(true);
        prepareRecyclerView();
    }

    void prepareRecyclerView() {
        recyclerView = fragment_view.findViewById(R.id.list_rooms);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 1));
        adapter = new NewsFeedAdapter(activity_ref,all_feeds, "SCHEDULED_ROOM");
        recyclerView.setAdapter(adapter);
        adapter.setClickListener(this);

    }

    void fetch_all_events(boolean with_indicator) {
        final User user = User.getLoggedInUser();
        Log.d("debug_audio", "fetch all events");
        ProgressBar busy = fragment_view.findViewById(R.id.loading_channel_token_fetch7);
        if (with_indicator) {
            busy.setVisibility(View.VISIBLE);
        }
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        JsonHttpResponseHandler jrep = new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    ArrayList<EventElement> all_event_data = new ArrayList<>();

                    JSONArray all_info = response.getJSONArray("all_events");
                    for(int i = 0; i < all_info.length(); i++) {
                        JSONObject event = all_info.getJSONObject(i);
                        String event_title = event.getString("event_title");
                        String event_channel_name = event.getString("event_channel_name");
                        String event_room_slug = event.getString("event_room_slug");
                        long timestamp = event.getLong("timestamp");
                        Boolean hasStarted = event.getBoolean("has_started");
                        Boolean isScheduled = event.getBoolean("is_scheduled");
                        Boolean isAdmin = event.getBoolean("is_room_admin");
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

                        EventElement e = new EventElement();
                        e.eventChannelName = event_channel_name;
                        e.eventTitle = event_title;
                        e.eventID = event_id;
                        e.eventPhotoUrls = photo_urls;
                        e.userElements = all_users_in_card;
                        e.roomSlug = event_room_slug;
                        e.scheduleTimestamp = timestamp;
                        e.isRoomAdmin = isAdmin;
                        e.hasStarted = hasStarted;
                        e.isScheduled = isScheduled;
                        all_event_data.add(e);
                    }


                    all_feeds.clear();
                    all_feeds.addAll(all_event_data);
                    adapter.notifyDataSetChanged();

                    if (with_indicator) {
                        busy.setVisibility(View.INVISIBLE);
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
        client.post(App.getBaseURL() + "page/all_events_scheduled", params, jrep);
    }

//    @Override
    public void onTagClick(int index) {
        BottomScheduleRoom bottomSheet =
                BottomScheduleRoom.newInstance();

        Bundle bundle = new Bundle();
        bundle.putString("room_slug", all_feeds.get(index).roomSlug );
        bottomSheet.setArguments(bundle);
        bottomSheet.show(getFragmentManager(),
                BottomScheduleRoom.TAG);
    }

    @Override
    public boolean onBackPressed() {
        return true;
    }

    private void showDialogForCurrentRoomExit(String title, String description) {
        showDialogExit(title, description, new ServerCallBack() {
            @Override
            public void onSuccess() {
                RoomHelper.showDialogRoomCreate(activity_ref);
            }
        });
    }

    void showDialogExit(String title, String description, ServerCallBack cs) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity_ref);
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

    BroadcastReceiver broadCastNewMessage = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String user_action = intent
                    .getStringExtra("user_action");
            Integer uid;
            switch (user_action) {
                case "ROOM_CREATE":
                    fetch_all_events(true);
                    break;
            }
        }
    };
    @Override
    public void onDestroy() {
        activity_ref.unregisterReceiver(broadCastNewMessage);
        super.onDestroy();
    }

    @Override
    public void onRoomClick(int index, String list_type) {

    }
}