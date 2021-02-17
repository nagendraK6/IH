package com.relylabs.InstaHelo;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.os.Bundle;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.relylabs.InstaHelo.models.User;
import com.relylabs.InstaHelo.models.UsersInRoom;
import com.relylabs.InstaHelo.rooms.RoomsUsersDisplayListAdapter;
import com.relylabs.InstaHelo.sharing.SharingContactListAdapter;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import cz.msebera.android.httpclient.Header;
import de.hdodenhof.circleimageview.CircleImageView;

public class RoomDisplayFragment extends Fragment implements RoomsUsersDisplayListAdapter.ItemClickListener  {

    ArrayList<UsersInRoom> speakers;
    ArrayList<UsersInRoom> audiences;

    RecyclerView recyclerView_s, recyclerView_a;
    public  boolean is_current_role_speaker, is_muted, is_current_user_admin;
    RoomsUsersDisplayListAdapter speaker_adapter, audience_adapter;
    TextView raise_hand;
    ImageView mute_unmute_button_bottom;
    Integer event_id = -1;
    private Activity activity;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof Activity) {
            this.activity = (Activity) context;
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
            Log.d("debug_audio", "Received update from the main");
            ArrayList<UsersInRoom> t = intent
                    .getParcelableArrayListExtra("speakers_list");
            is_current_role_speaker =
                    intent.getBooleanExtra("is_current_role_speaker", false);
            is_current_user_admin = intent.getBooleanExtra("is_current_user_admin", false);

            if (is_current_role_speaker) {
                mute_unmute_button_bottom.setVisibility(View.VISIBLE);
                raise_hand.setVisibility(View.INVISIBLE);
            } else {
                mute_unmute_button_bottom.setVisibility(View.INVISIBLE);
                raise_hand.setVisibility(View.VISIBLE);
            }
            speakers.clear();
            speakers.addAll(t);
            speaker_adapter.notifyDataSetChanged();
            processMuteUnmuteSettings();
            fetchListenersData();
        }
    };

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_room_display, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ImageView invite = view.findViewById(R.id.invite);
        invite.setVisibility(View.INVISIBLE);


        ImageView notif = view.findViewById(R.id.notification);
        notif.setVisibility(View.INVISIBLE);

        CircleImageView img = view.findViewById(R.id.user_profile_image);
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

        speakers = getArguments().getParcelableArrayList("speakers_list");
        ArrayList<UsersInRoom> all_users = getArguments().getParcelableArrayList("all_users_list");

        audiences = new ArrayList<>();

        is_current_role_speaker = getArguments().getBoolean("is_current_role_speaker");
        is_current_user_admin = getArguments().getBoolean("is_current_user_admin");
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
                    for (int i = 0; i < speakers.size(); i++) {
                        if (speakers.get(i).UserId.equals(lg_user.UserID)) {
                            speakers.get(i).IsMuted = is_muted;
                        }
                    }

                    speaker_adapter.notifyDataSetChanged();
                    processMuteUnmuteSettings();
                    broadcastLocalUpdate("MUTE_UNMUTE_CLICK");
                }
            });
        raise_hand = view.findViewById(R.id.raise_hand);

        is_muted = getArguments().getBoolean("is_muted");
        event_id = getArguments().getInt("event_id", -1);
        processMuteUnmuteSettings();

        setupSpeakers(view);
        setupAudiences(view);
        fetchListenersData();
    }

    void processMuteUnmuteSettings() {
        if (is_current_role_speaker) {
            mute_unmute_button_bottom.setVisibility(View.VISIBLE);
            if (is_muted) {
                if (activity != null) {
                    mute_unmute_button_bottom.setBackground(activity.getDrawable(R.drawable.mic_off));
                }
            } else {
                if (activity != null) {
                    mute_unmute_button_bottom.setBackground(activity.getDrawable(R.drawable.mic_on_room_view));
                }
            }

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
        if (getActivity() != null) {
            Fragment f = getActivity().getSupportFragmentManager().findFragmentById(R.id.fragment_holder);
            FragmentManager manager = getActivity().getSupportFragmentManager();
            FragmentTransaction trans = manager.beginTransaction();
            trans.setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_top);
            trans.remove(f);
            trans.commit();
            manager.popBackStack();
        }
    }
    @Override
    public void onDestroy() {
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
                        Log.d("debug_audio", " All data " + String.valueOf(all_data.length()));
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
}