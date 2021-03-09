package com.relylabs.InstaHelo.bottomsheet;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
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

public class BottomScheduleRoom extends BottomSheetDialogFragment implements View.OnClickListener {
    public static final String TAG = "ActionBottomDialog";
    private static final int PERMISSION_REQ_ID_RECORD_AUDIO = 0;
    private static final int PERMISSION_REQ_ID_RECORD_AUDIO_INIT = 1;
    SpeakerAdapter adapter;
    RecyclerView recyclerView;
    final Calendar myCalendar = Calendar.getInstance();
    private ItemClickListener mListener;
    View fragment_view;
    public Boolean isRoomAdmin = false;
    public Boolean hasStarted = false;
    private ArrayList<String> names = new ArrayList<String>();
    private  ArrayList<String> usernames = new ArrayList<String>();
    private  ArrayList<String> img = new ArrayList<String>();
    private  ArrayList<String> user_ids = new ArrayList<>();
    private Integer tappedRoomId = -9;
    Boolean is_room_fragment_loaded = false;
    public static BottomScheduleRoom newInstance() {
        return new BottomScheduleRoom();
    }
    FragmentActivity activity_ref;
    int event_id;
    String title_main;
    String channelName;
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
    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        fragment_view = view;
        String room_slug = this.getArguments().getString("room_slug");
        Log.d("room_slug",room_slug);
        getData(room_slug);

        ImageView action_btn = view.findViewById(R.id.imageView5);

        action_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("hasstarted", hasStarted.toString());
                Log.d("is_admin",isRoomAdmin.toString());
                if(isRoomAdmin && !hasStarted){
                    sendStartNotiServer();
                    dismiss();
                    startRoom();
                }
                else if(isRoomAdmin && hasStarted){
                    dismiss();
                    startRoom();
                }
                else if (!isRoomAdmin && !hasStarted){
                    Log.d("room_not_started","log");
                }
                else if(!isRoomAdmin && hasStarted){
                    dismiss();
                    startRoom();
                }
            }
        });
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
        client.post(App.getBaseURL() + "page/start_schedule_room", params, jrep);
    }
    public void startRoom(){
        UserSettings us = UserSettings.getSettings();
        tappedRoomId = event_id;
        if (tappedRoomId != -1 && tappedRoomId == us.selected_event_id) {
            loadRoomFragment();
            return;
        }

        us.selected_channel_name  =  channelName;
        us.selected_channel_display_name = title_main;
        us.save();

//                    show_busy_indicator();

        if (checkPermissionInitial(activity_ref)) {
            if (tappedRoomId != us.selected_event_id && us.selected_event_id != -1) {
                // user switching channel...
                // don't save room id as the service will use to leave the channel and set in service
                RoomHelper.send_channel_switch(activity_ref, us.selected_channel_name, us.selected_channel_display_name, tappedRoomId);
            } else {
                us.selected_event_id = tappedRoomId;
                us.save();
                RoomHelper.sendRoomServieStartRequest(activity_ref, us.selected_channel_name, us.selected_channel_display_name, us.selected_event_id);
            }
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

    private void loadRoomFragment() {
        unloadFragmentBottom();
        if (!is_room_fragment_loaded) {
            UserSettings us = UserSettings.getSettings();
            Bundle args = new Bundle();
            args.putString("update_type", "LIST_CHANGE");
            args.putString("event_title", us.selected_channel_display_name);
            args.putInt("event_id", us.selected_event_id);
            Fragment fr = new RoomDisplayFragment();
            FragmentTransaction ft = activity_ref.getSupportFragmentManager().beginTransaction();
            ft.setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_top);
            fr.setArguments(args);
            ft.add(R.id.fragment_holder, fr);
            is_room_fragment_loaded = true;
            ft.commitAllowingStateLoss();
        }
    }
    private void unloadFragmentBottom() {
        if (activity_ref != null) {
            Fragment fragment = activity_ref.getSupportFragmentManager().findFragmentByTag("bottom_sheet");
            if(fragment != null) {
                activity_ref.getSupportFragmentManager().beginTransaction().remove(fragment).commitAllowingStateLoss();
            }
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
        ImageView start = fragment_view.findViewById(R.id.imageView5);
        busy.setVisibility(View.VISIBLE);
        JsonHttpResponseHandler jrep = new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    busy.setVisibility(View.INVISIBLE);
                    sharing.setVisibility(View.VISIBLE);
                    start.setVisibility(View.VISIBLE);
                    Log.d("response_follow", response.toString());
                    TextView title = fragment_view.findViewById(R.id.shcedule_topic);
                    TextView date_schedule = fragment_view.findViewById(R.id.shcedule_time);
                    TextView speaker_list = fragment_view.findViewById(R.id.hosts);
                    String title_json = response.getString("title");
                    title_main = title_json;
                    int id = response.getInt("event_id");
                    event_id = id;
                    String chanelName = response.getString("event_channel_name");
                    channelName = chanelName;
                    isRoomAdmin = response.getBoolean("is_room_admin");
                    hasStarted = response.getBoolean("has_started");
                    long timestamp = response.getLong("schedule_time");
                    myCalendar.setTimeInMillis(timestamp);
                    String myFormat = "E, dd MMM yyyy hh:mm a z"; //In which you need put here
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
        int layout_size = usernames.size() <= 3 ? usernames.size() : 6;
        GridLayoutManager gm = new GridLayoutManager(fragment_view.getContext(), 6);
        gm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                Log.d("debug_c", "Span size check" +String.valueOf(position));
                return 1;
            }
        });
        recyclerView.setLayoutManager(gm);
        adapter = new SpeakerAdapter(getContext(), names, usernames, img,user_ids);
        recyclerView.setAdapter(adapter);
    }
    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
    @Override public void onClick(View view) {
        TextView tvSelected = (TextView) view;
        mListener.onItemClick(tvSelected.getText().toString());
        dismiss();
    }
    public interface ItemClickListener {
        void onItemClick(String item);
    }
}