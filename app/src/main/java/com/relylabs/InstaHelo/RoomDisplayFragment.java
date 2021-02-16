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

import com.relylabs.InstaHelo.models.User;
import com.relylabs.InstaHelo.models.UsersInRoom;
import com.relylabs.InstaHelo.rooms.RoomsUsersDisplayListAdapter;
import com.relylabs.InstaHelo.sharing.SharingContactListAdapter;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import de.hdodenhof.circleimageview.CircleImageView;

public class RoomDisplayFragment extends Fragment implements RoomsUsersDisplayListAdapter.ItemClickListener  {

    ArrayList<UsersInRoom> speakers;
    ArrayList<UsersInRoom> audiences;

    RecyclerView recyclerView_s, recyclerView_a;
    public  boolean is_current_role_speaker, is_muted, is_current_user_admin;
    RoomsUsersDisplayListAdapter speaker_adapter, audience_adapter;
    TextView raise_hand;
    ImageView mute_unmute_button_bottom;

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
            ArrayList<UsersInRoom> all_users = intent.getParcelableArrayListExtra("all_users_list");
            is_current_role_speaker = getArguments().getBoolean("is_current_role_speaker");
            is_current_user_admin = getArguments().getBoolean("is_current_user_admin");
            speakers.clear();
            speakers.addAll(t);
            speaker_adapter.notifyDataSetChanged();
            processMuteUnmuteSettings();

            ArrayList<UsersInRoom> t_a = getAudiencesFromSpeakersAndAllUsers(all_users);
            audiences.clear();
            audiences.addAll(t_a);
            audience_adapter.notifyDataSetChanged();
            Log.d("debug_audio", "Main refreshed. Audience size " + String.valueOf(t_a.size()));
        }
    };

    ArrayList<UsersInRoom> getAudiencesFromSpeakersAndAllUsers(ArrayList<UsersInRoom> all_users) {
        if (all_users == null) {
            return new ArrayList<>();
        }
        ArrayList<Integer> speaker_ids = new ArrayList<>();
        for(int i = 0; i < speakers.size(); i++) {
            speaker_ids.add(speakers.get(i).UserId);
        }

        ArrayList<UsersInRoom> listeners = new ArrayList<>();
        for (int i  = 0 ; i < all_users.size(); i++) {
            if (!speaker_ids.contains(all_users.get(i).UserId)) {
                listeners.add(all_users.get(i));
            }
        }

        return  listeners;
    }

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
        audiences = getAudiencesFromSpeakersAndAllUsers(all_users);
        is_current_role_speaker = getArguments().getBoolean("is_current_role_speaker");
        is_current_user_admin = getArguments().getBoolean("is_current_user_admin");
        if (is_current_role_speaker) {
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
        }
        raise_hand = view.findViewById(R.id.raise_hand);

        is_muted = getArguments().getBoolean("is_muted");
        processMuteUnmuteSettings();

        setupSpeakers(view);
        setupAudiences(view);
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
        }
    }

    private  void setupSpeakers(View view) {
        recyclerView_a = view.findViewById(R.id.listener_grid_list);
        recyclerView_a.setLayoutManager(new GridLayoutManager(getContext(), 3));
        audience_adapter = new RoomsUsersDisplayListAdapter(getContext(), audiences, is_current_user_admin);
        audience_adapter.setClickListener(new RoomsUsersDisplayListAdapter.ItemClickListener() {
            @Override
            public void onItemClick(Integer uid, String action) {
                Bundle data_bundle = new Bundle();
                data_bundle.putString("user_action", "MAKE_SPEAKER");
                data_bundle.putInt("uid", uid);
                Intent intent = new Intent("update_from_room");
                intent.putExtras(data_bundle);
                if (activity != null) {
                    activity.sendBroadcast(intent);
                }
            }
        });
        recyclerView_a.setAdapter(audience_adapter);
    }


    private  void setupAudiences(View view) {
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

    }
}