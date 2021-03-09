package com.relylabs.InstaHelo.rooms;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.relylabs.InstaHelo.R;
import com.relylabs.InstaHelo.Utils.RoomHelper;
import com.relylabs.InstaHelo.models.User;
import com.relylabs.InstaHelo.models.UsersInRoom;

public class RoomsSpeakerAcceptRejectRequestDialogFragment extends BottomSheetDialogFragment {

    String sender_speaker_id = "";
    public  RoomsSpeakerAcceptRejectRequestDialogFragment(String sender_id) {
        super();
        sender_speaker_id = sender_id;
    }

    final Handler handler  = new Handler();
    final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (isVisible()) {
                RoomHelper.send_denied_speaker_request(activity, User.getLoggedInUserID());
                dismiss();
            }
        }
    };

    FragmentActivity activity;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.speaker_request_accept_reject, container,
                false);
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Activity){
            activity=(FragmentActivity) context;
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ImageView deny_speaker_request = view.findViewById(R.id.deny_speaker_request);
        deny_speaker_request.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RoomHelper.send_denied_speaker_request(activity, User.getLoggedInUserID());
                dismiss();
            }
        });

        ImageView accept_speaker_request = view.findViewById(R.id.accept_speaker_request);
        accept_speaker_request.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RoomHelper.send_accepted_speaker_request(activity, User.getLoggedInUserID());
                dismiss();
            }
        });

        try {
            Integer sender_id = Integer.valueOf(sender_speaker_id);
            UsersInRoom u = UsersInRoom.getRecords(sender_id);
            if (u != null) {
                String txt = u.Name + " invited you to speak";
                TextView accept_reject = view.findViewById(R.id.accept_reject);
                accept_reject.setText(txt);
            }
        } catch (Exception ex) {
            Log.d("debug_d", "Error in uid fetch");
        }

        handler.postDelayed(runnable, 15000);
    }
}
