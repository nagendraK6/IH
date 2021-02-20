package com.relylabs.InstaHelo;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.media.Image;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.os.Bundle;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.shape.CornerFamily;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.relylabs.InstaHelo.R;
import com.relylabs.InstaHelo.models.User;
import com.relylabs.InstaHelo.rooms.HandRaiseUsersListDialogFragment;

import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

public class BottomFragment extends Fragment implements IOnBackPressed {
    TextView mute_unmute_button_bottom;
    Boolean is_current_role_speaker, is_muted;
    String event_title;
    Integer event_id;

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
        is_current_role_speaker = getArguments().getBoolean("is_current_role_speaker");
        is_muted =  getArguments().getBoolean("is_muted");
        event_title = getArguments().getString("event_title");
        event_id = getArguments().getInt("event_id");
        TextView title = view.findViewById(R.id.title);
        title.setText(event_title);
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

                    processMuteUnmuteSettings();
                    broadcastLocalUpdate("MUTE_UNMUTE_CLICK");
                }
            });
        }


        TextView raise_hand = view.findViewById(R.id.raise_hand);
        if (is_current_role_speaker) {
            raise_hand.setVisibility(View.INVISIBLE);
        } else {
            raise_hand.setVisibility(View.VISIBLE);
        }

        ShapeableImageView i1 = view.findViewById(R.id.author_image_in_shortcut_1);
        if (!User.getLoggedInUser().ProfilePicURL.equals("")) {
            float radius = getActivity().getResources().getDimension(R.dimen.default_corner_news_feed_image_bottom);
            i1.setShapeAppearanceModel(i1.getShapeAppearanceModel()
                    .toBuilder()
                    .setTopRightCorner(CornerFamily.ROUNDED,radius)
                    .setTopLeftCorner(CornerFamily.ROUNDED,radius)
                    .setBottomLeftCorner(CornerFamily.ROUNDED,radius)
                    .setBottomRightCorner(CornerFamily.ROUNDED,radius)
                    .build());

            Glide.with(getContext()).load(User.getLoggedInUser().ProfilePicURL).into(i1);
        }

        ShapeableImageView i2 = view.findViewById(R.id.author_image_in_shortcut_2);
        if (!User.getLoggedInUser().ProfilePicURL.equals("")) {
            float radius = getActivity().getResources().getDimension(R.dimen.default_corner_news_feed_image_bottom);
            i2.setShapeAppearanceModel(i2.getShapeAppearanceModel()
                    .toBuilder()
                    .setTopRightCorner(CornerFamily.ROUNDED,radius)
                    .setTopLeftCorner(CornerFamily.ROUNDED,radius)
                    .setBottomLeftCorner(CornerFamily.ROUNDED,radius)
                    .setBottomRightCorner(CornerFamily.ROUNDED,radius)
                    .build());
            Glide.with(getContext()).load(User.getLoggedInUser().ProfilePicURL).into(i2);
        }
        processMuteUnmuteSettings();

        TextView leave_quitely = view.findViewById(R.id.leave_quitely);
        leave_quitely.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                broadcastLocalUpdate("LEAVE_CHANNEL");
            }
        });

        TextView hand_raise_admin = view.findViewById(R.id.hand_raise_admin);
        hand_raise_admin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                broadcastLocalUpdate("LEAVE_CHANNEL");
                HandRaiseUsersListDialogFragment bottomSheetDialog = new HandRaiseUsersListDialogFragment();
              //  View parentView = getLayoutInflater().inflate(R.layout.activity_main, null);
              //  bottomSheetDialog.setContentView(parentView);
                bottomSheetDialog.show(getFragmentManager(), "TAG");
            }
        });

        raise_hand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                send_raise_hand_request();
            }
        });
    }


    private void send_raise_hand_request() {
        broadcastLocalUpdate("HAND_RAISE");
    }

    void processMuteUnmuteSettings() {
        if (is_current_role_speaker) {
            mute_unmute_button_bottom.setVisibility(View.VISIBLE);
            if (is_muted) {
                mute_unmute_button_bottom.setBackground(getActivity().getDrawable(R.drawable.mic_off_self_user));
            } else {
                mute_unmute_button_bottom.setBackground(getActivity().getDrawable(R.drawable.mic_on_room_view));
            }

        } else {
            mute_unmute_button_bottom.setVisibility(View.INVISIBLE);
        }
    }



    private  void broadcastLocalUpdate(String action) {
        Bundle data_bundle = new Bundle();
        data_bundle.putString("user_action", action);
        Intent intent = new Intent("update_from_room");
        intent.putExtras(data_bundle);
        getActivity().sendBroadcast(intent);
    }



    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    @Override
    public boolean onBackPressed() {
        broadcastLocalUpdate("LEAVE_CHANNEL");
        return false;
    }
}