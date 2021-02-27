package com.relylabs.InstaHelo;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.os.Bundle;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.shape.CornerFamily;
import com.relylabs.InstaHelo.models.User;
import com.relylabs.InstaHelo.HandRaise.HandRaiseUsersListDialogFragment;
import com.relylabs.InstaHelo.models.UserSettings;

public class BottomFragment extends Fragment implements IOnBackPressed {


    BroadcastReceiver broadCastNewMessageBottomFragemnt = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String user_action =  intent
                    .getStringExtra("update_type");
            Integer uid;
            UserSettings us = UserSettings.getSettings();
            switch (user_action) {
                case "HAND_RAISE":
                case "LIST_CHANGE":
                case "REFRESH_SETTINGS":
                    processMuteUnmuteSettings();
                    processRaiseHandSettings();
                    break;
                case "EXIT_ROOM":
                    broadcastLocalUpdate("LEAVE_CHANNEL");
                    return;
            }
        }
    };

    BroadcastReceiver broadcastReceiverFromService = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String user_action =  intent
                    .getStringExtra("update_type");
            UserSettings us = UserSettings.getSettings();
            switch (user_action) {
                case "HAND_RAISE":
                case "LIST_CHANGE":
                case "REFRESH_SETTINGS":
                    processMuteUnmuteSettings();
                    processRaiseHandSettings();
                    break;
                case "EXIT_ROOM":
                    broadcastLocalUpdate("LEAVE_CHANNEL");
                    return;
            }
        }
    };


    TextView mute_unmute_button_bottom;
    String event_title;
    Integer event_id;
    TextView hand_raise_audience_admin;
    private FragmentActivity activity;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Activity){
            activity=(FragmentActivity) context;
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_minimzed_visible, container, false);
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                broadcastLocalUpdate("EXPAND_ROOM");
            }
        });

        mute_unmute_button_bottom = view.findViewById(R.id.mute_unmute_button_bottom);
        event_title = getArguments().getString("event_title");
        event_id = getArguments().getInt("event_id");
        TextView title = view.findViewById(R.id.title);
        title.setText(event_title);
        UserSettings us = UserSettings.getSettings();
        if (us.is_current_role_speaker) {
            mute_unmute_button_bottom.setVisibility(View.VISIBLE);
            mute_unmute_button_bottom.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (us.is_muted) {
                        us.is_muted = false;
                        us.save();
                    } else {
                        us.is_muted = true;
                        us.save();
                    }

                    processMuteUnmuteSettings();
                    broadcastLocalUpdate("MUTE_UNMUTE_CLICK");
                }
            });
        }



        // there are 4 secnarios for images
        // admin with notification
        // admin without notification
        // audience hand raised
        // audience hand down

        hand_raise_audience_admin = view.findViewById(R.id.hand_raise_audience_admin);
        if (!us.is_current_user_admin && us.is_current_role_speaker) {
            hand_raise_audience_admin.setVisibility(View.INVISIBLE);
        }

        hand_raise_audience_admin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processHandRaiseClick();
            }
        });

        setCurrentImageHandRaise();


        ShapeableImageView i1 = view.findViewById(R.id.author_image_in_shortcut_1);
        if (!User.getLoggedInUser().ProfilePicURL.equals("")) {
            float radius = activity.getResources().getDimension(R.dimen.default_corner_news_feed_image_bottom);
            i1.setShapeAppearanceModel(i1.getShapeAppearanceModel()
                    .toBuilder()
                    .setTopRightCorner(CornerFamily.ROUNDED,radius)
                    .setTopLeftCorner(CornerFamily.ROUNDED,radius)
                    .setBottomLeftCorner(CornerFamily.ROUNDED,radius)
                    .setBottomRightCorner(CornerFamily.ROUNDED,radius)
                    .build());

            Glide.with(activity).load(User.getLoggedInUser().ProfilePicURL).into(i1);
        }

        ShapeableImageView i2 = view.findViewById(R.id.author_image_in_shortcut_2);
        if (!User.getLoggedInUser().ProfilePicURL.equals("")) {
            float radius = activity.getResources().getDimension(R.dimen.default_corner_news_feed_image_bottom);
            i2.setShapeAppearanceModel(i2.getShapeAppearanceModel()
                    .toBuilder()
                    .setTopRightCorner(CornerFamily.ROUNDED,radius)
                    .setTopLeftCorner(CornerFamily.ROUNDED,radius)
                    .setBottomLeftCorner(CornerFamily.ROUNDED,radius)
                    .setBottomRightCorner(CornerFamily.ROUNDED,radius)
                    .build());
            Glide.with(activity).load(User.getLoggedInUser().ProfilePicURL).into(i2);
        }
        processMuteUnmuteSettings();

        TextView leave_quitely = view.findViewById(R.id.leave_quitely);
        leave_quitely.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                broadcastLocalUpdate("LEAVE_CHANNEL");
            }
        });

        IntentFilter new_post = new IntentFilter("update_from_main");
        if (activity != null) {
            activity.registerReceiver(broadCastNewMessageBottomFragemnt, new_post);
        }

        IntentFilter update_from_service = new IntentFilter("update_from_service");
        if (activity != null) {
            activity.registerReceiver(broadcastReceiverFromService, update_from_service);
        }
    }


    private void send_raise_hand_request() {
        broadcastLocalUpdate("MAKE_SPEAKER_REQUEST");
    }


    private void send_raise_hand_request_clear() {
        broadcastLocalUpdate("HAND_RAISE_CLEAR");
    }



    void processMuteUnmuteSettings() {
        UserSettings us  = UserSettings.getSettings();
        if (us.is_current_role_speaker) {
            mute_unmute_button_bottom.setVisibility(View.VISIBLE);
            if (us.is_muted) {
                mute_unmute_button_bottom.setBackground(activity.getDrawable(R.drawable.mic_off_self_user));
            } else {
                mute_unmute_button_bottom.setBackground(activity.getDrawable(R.drawable.mic_on_room_view));
            }
        } else {
            mute_unmute_button_bottom.setVisibility(View.INVISIBLE);
        }
    }


    void processRaiseHandSettings() {
        setCurrentImageHandRaise();
    }


    private  void broadcastLocalUpdate(String action) {
        Bundle data_bundle = new Bundle();
        data_bundle.putString("user_action", action);
        Intent intent = new Intent("update_from_room");
        intent.putExtras(data_bundle);
        activity.sendBroadcast(intent);
    }



    @Override
    public void onDestroy() {
        activity.unregisterReceiver(broadCastNewMessageBottomFragemnt);
        activity.unregisterReceiver(broadcastReceiverFromService);
        super.onDestroy();
    }


    @Override
    public boolean onBackPressed() {
        broadcastLocalUpdate("LEAVE_CHANNEL");
        return false;
    }

    private void setCurrentImageHandRaise() {
        UserSettings us  = UserSettings.getSettings();
        if (us.is_current_user_admin && us.audience_hand_raised) {
            hand_raise_audience_admin.setBackground(activity.getDrawable(R.drawable.raise_hand_with_notification));
            Log.d("debug_audio", "Set image hand with notification");
        }

        if (us.is_current_user_admin && !us.audience_hand_raised) {
            hand_raise_audience_admin.setBackground(activity.getDrawable(R.drawable.raise_hand_without_notification));
            Log.d("debug_audio", "Set image hand without notification");
        }

        if (!us.is_current_role_speaker && us.is_self_hand_raised) {
            hand_raise_audience_admin.setBackground(activity.getDrawable(R.drawable.hand_raise_dark));
            Log.d("debug_audio", "Audience tapped hand");
        }

        if (!us.is_current_role_speaker && !us.is_self_hand_raised) {
            hand_raise_audience_admin.setBackground(activity.getDrawable(R.drawable.raise_hand_without_notification));
            Log.d("debug_audio", "Audience tapped hand raise down");
        }


        if (!us.is_current_user_admin && us.is_current_role_speaker) {
            hand_raise_audience_admin.setVisibility(View.INVISIBLE);
        } else {
            hand_raise_audience_admin.setVisibility(View.VISIBLE);
        }
    }

    private void processHandRaiseClick() {
        UserSettings us = UserSettings.getSettings();

        if (us.is_current_user_admin) {
            HandRaiseUsersListDialogFragment bottomSheetDialog = new HandRaiseUsersListDialogFragment();
            Bundle bundle = new Bundle();
            bundle.putInt("event_id", event_id);
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
}