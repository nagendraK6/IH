package com.relylabs.InstaHelo;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.os.Bundle;

import com.relylabs.InstaHelo.R;
import com.relylabs.InstaHelo.models.User;

public class BottomFragment extends Fragment {
    ImageView mute_unmute_button_bottom;
    Boolean is_current_role_speaker, is_muted;
    String event_title;

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

        processMuteUnmuteSettings();
    }

    void processMuteUnmuteSettings() {
        if (is_current_role_speaker) {
            mute_unmute_button_bottom.setVisibility(View.VISIBLE);
            if (is_muted) {
                mute_unmute_button_bottom.setBackground(getActivity().getDrawable(R.drawable.mic_off));
            } else {
                mute_unmute_button_bottom.setBackground(getActivity().getDrawable(R.drawable.mic_on_room_view));
            }

        } else {
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


}