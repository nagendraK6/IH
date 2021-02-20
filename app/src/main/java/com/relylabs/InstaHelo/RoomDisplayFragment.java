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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.shape.CornerFamily;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.relylabs.InstaHelo.models.User;
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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import cz.msebera.android.httpclient.Header;
import de.hdodenhof.circleimageview.CircleImageView;

public class RoomDisplayFragment extends Fragment implements RoomsUsersDisplayListAdapter.ItemClickListener, IOnBackPressed  {

    ArrayList<UsersInRoom> speakers;
    ArrayList<UsersInRoom> audiences;

    RecyclerView recyclerView_s, recyclerView_a;
    public  boolean is_current_role_speaker, is_muted, is_current_user_admin;
    RoomsUsersDisplayListAdapter speaker_adapter, audience_adapter;
    TextView raise_hand;
    ImageView mute_unmute_button_bottom;
    Integer event_id = -1;
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
         //   ArrayList<UsersInRoom> t = intent
              //      .getParcelableArrayListExtra("speakers_list");

            if (update_type.equals("LIST_CHANGE")) {
                is_current_role_speaker =
                        intent.getBooleanExtra("is_current_role_speaker", false);
                is_current_user_admin = intent.getBooleanExtra("is_current_user_admin", false);
                is_muted = intent.getBooleanExtra("is_muted", false);
                Log.d("debug_voice", "Mute is " + String.valueOf(is_muted));
                if (is_current_role_speaker) {
                    mute_unmute_button_bottom.setVisibility(View.VISIBLE);
                    raise_hand.setVisibility(View.INVISIBLE);
                } else {
                    mute_unmute_button_bottom.setVisibility(View.INVISIBLE);
                    raise_hand.setVisibility(View.VISIBLE);
                }

             //   ArrayList<UsersInRoom> t = UsersInRoom.getAllSpeakers();
                incrementalAdd();
          //      speakers.clear();
           //     speakers.addAll(t);
           //     speaker_adapter.notifyDataSetChanged();
                processMuteUnmuteSettings();
                fetchListenersData();
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
            }

            if (update_type.equals("CONNECTION_CHANGE")) {
                fetchListenersData();
            }
        }
    };


    private  void updateItem(Integer index, Boolean muted) {
            Bundle diff = new Bundle();
                diff.putBoolean("IsMuted", muted);
                speaker_adapter.notifyItemChanged(index, diff);
    }


    private void incrementalAdd() {
        ArrayList<UsersInRoom> new_list = UsersInRoom.getAllSpeakers();
        //if (new_list.size() == speakers.size()) {
            final RoomsUsersDisplayListDiffsCallback diffCallback = new RoomsUsersDisplayListDiffsCallback(speakers, new_list);
            final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);
            speakers.clear();
            speakers.addAll(new_list);
            diffResult.dispatchUpdatesTo(speaker_adapter);
        /*} else {
         speakers.clear();
         speakers.addAll(new_list);
         speaker_adapter.notifyDataSetChanged();
        }*/
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
                removefragment();
            }
        });

        TextView exit_room = view.findViewById(R.id.leave_quitely);
        mute_unmute_button_bottom = view.findViewById(R.id.mute_unmute_button_bottom);

        exit_room.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                broadcastLocalUpdate("LEAVE_CHANNEL");
                removefragment();
            }
        });

        IntentFilter new_post = new IntentFilter("update_from_main");
        if (activity != null) {
            activity.registerReceiver(broadCastNewMessage, new_post);
        }


        audiences = new ArrayList<>();
        speakers = new ArrayList<>();
//        speakers = getArguments().getParcelableArrayList("speakers_list");
        speakers = UsersInRoom.getAllSpeakers();

        is_current_role_speaker = getArguments().getBoolean("is_current_role_speaker");
        is_current_user_admin = getArguments().getBoolean("is_current_user_admin");
        String event_title = getArguments().getString("event_title");
        TextView title_of_room = view.findViewById(R.id.title_of_room);
        title_of_room.setText(event_title);
        mute_unmute_button_bottom.setVisibility(View.VISIBLE);

            mute_unmute_button_bottom.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (is_muted) {
                        is_muted = false;
                    } else {
                        is_muted = true;
                    }

                    User lg_user = User.getLoggedInUser();
                    int index_impacted = -1;
                    for (int i = 0; i < speakers.size(); i++) {
                        if (speakers.get(i).UserId.equals(lg_user.UserID)) {
                            speakers.get(i).IsMuted = is_muted;
                            index_impacted = i;
                        }
                    }

                    updateItem(index_impacted, is_muted);
                   // speaker_adapter.notifyDataSetChanged();
                    processMuteUnmuteSettings();
                    broadcastLocalUpdate("MUTE_UNMUTE_CLICK");
                }
            });
        raise_hand = view.findViewById(R.id.raise_hand);

        is_muted = getArguments().getBoolean("is_muted");
        event_id = getArguments().getInt("event_id", -1);
        processMuteUnmuteSettings();
       // speakers = UsersInRoom.getAllSpeakers();
        setupSpeakers(view);
        setupAudiences(view);
       // updatePostingDetails(t);
        fetchListenersData();
    }

    void processMuteUnmuteSettings() {
        Log.d("debug_voice", "Current settings bottom is " + String.valueOf(is_muted));
        if (is_current_role_speaker) {
            mute_unmute_button_bottom.setVisibility(View.VISIBLE);
            if (is_muted) {
                if (activity != null) {
                    mute_unmute_button_bottom.setBackground(activity.getDrawable(R.drawable.mic_off_self_user));
                }
            } else {
                if (activity != null) {
                    mute_unmute_button_bottom.setBackground(activity.getDrawable(R.drawable.mic_on_room_view));
                }
            }

            raise_hand.setVisibility(View.INVISIBLE);
            raise_hand.setVisibility(View.INVISIBLE);
        } else {
            raise_hand.setVisibility(View.VISIBLE);
            mute_unmute_button_bottom.setVisibility(View.INVISIBLE);
        }
    }

    private  void setupAudiences(View view) {
        recyclerView_a = view.findViewById(R.id.listener_grid_list);
        recyclerView_a.setLayoutManager(new GridLayoutManager(getContext(), 3));
        audience_adapter = new RoomsUsersDisplayListAdapter(getContext(), audiences, is_current_user_admin);
        audience_adapter.setClickListener(this);
        recyclerView_a.setAdapter(audience_adapter);
    }


    private  void setupSpeakers(View view) {
        recyclerView_s = view.findViewById(R.id.speaker_grid_list);
        recyclerView_s.setLayoutManager(new GridLayoutManager(getContext(), 3));
        speaker_adapter = new RoomsUsersDisplayListAdapter(getContext(), speakers, is_current_user_admin);
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

    private void removefragment() {
        if (activity != null) {
            Fragment f = activity.getSupportFragmentManager().findFragmentById(R.id.fragment_holder);
            FragmentManager manager = activity.getSupportFragmentManager();
            FragmentTransaction trans = manager.beginTransaction();
            trans.setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_top);
            trans.remove(f);
            trans.commit();
            manager.popBackStack();
        }
    }


    @Override
    public void onDestroy() {
        activity.unregisterReceiver(broadCastNewMessage);
        super.onDestroy();
    }

    @Override
    public void onItemClick(Integer uid, String action) {
        Log.d("debug_audio", action);
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
        params.add("event_id", String.valueOf(event_id));


        JsonHttpResponseHandler jrep= new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    String error_message = response.getString("error_message");
                    if (error_message.equals("SUCCESS")) {
                        JSONArray all_data = response.getJSONArray("all_audiences");
                        ArrayList<UsersInRoom> all_audiences = new ArrayList<>();
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


    private void sendPing() {
        checkConnection();
        User user = User.getLoggedInUser();
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        params.add("event_id", String.valueOf(event_id));


        JsonHttpResponseHandler jrep= new JsonHttpResponseHandler() {
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
        client.post( App.getBaseURL() + "page/send_ping", params, jrep);
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

    @Override
    public boolean onBackPressed() {
        broadcastLocalUpdate("MINIMISED");
        removefragment();
        return true;
    }
}