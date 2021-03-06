package com.relylabs.InstaHelo.rooms;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
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
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.shape.CornerFamily;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.relylabs.InstaHelo.R;
import com.relylabs.InstaHelo.Utils.Logger;
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
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import cz.msebera.android.httpclient.Header;
import de.hdodenhof.circleimageview.CircleImageView;

import static com.relylabs.InstaHelo.Utils.Helper.hideKeyboard;
import static com.relylabs.InstaHelo.Utils.Helper.loadFragment;

public class RoomCreateBottomSheetDialogFragment extends Fragment  {

    private FragmentActivity activity;
    TextView edit_room_title;
    TextView next_button;
    String room_title = "";
    TextView  back_button;
    RadioGroup room_selection;
    String room_type = "social";

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

    @Override
    public void onDestroy() {
        super.onDestroy();
        activity.unregisterReceiver(broadCastNewMessage);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        IntentFilter new_post = new IntentFilter("update_from_schedule");
        activity.registerReceiver(broadCastNewMessage, new_post);
        return inflater.inflate(R.layout.fragment_room_create_view, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        edit_room_title = view.findViewById(R.id.edit_room_title);
        next_button = view.findViewById(R.id.submit_room_create);
        back_button = view.findViewById(R.id.cancel);
        room_selection = view.findViewById(R.id.room_choice);
        TextView helper = view.findViewById(R.id.helperText);
        room_selection.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch(checkedId) {
                    case R.id.social_room:
                        room_type = "social";
                        helper.setText("Only the followers of speakers can join");
                            break;
                    case R.id.public_room:
                        room_type = "public";
                        helper.setText("Anyone can join");
                        break;
                }
            }
        });
        back_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removefragment();
            }
        });

        edit_room_title.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                Logger.log(Logger.OTP_TYPING);
                room_title = edit_room_title.getText().toString();
                if (room_title.length()  > 0 ) {
                    next_button.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.create_room_enabled));
                } else {
                    next_button.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.start_a_room_disabled));
                }
            }

        });

        ImageView imageView = view.findViewById(R.id.imageView);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!room_title.equals("")) {
                    hideKeyboard(getContext());
                    ScheduleForLater sch = new ScheduleForLater();
                    Bundle args = new Bundle();
                    args.putString("title", room_title);
                    args.putString("type",room_type);
                    sch.setArguments(args);
                    loadFragment(sch, activity);
                }
            }
        });
        next_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (room_title.length()  > 0 ) {
                    broadcastLocalUpdate("ROOM_CREATE", room_title, room_type);
                    removefragment();
                }
            }
        });


    }


    @Override
    public void onResume() {
        super.onResume();
        edit_room_title.post(new Runnable() {
            @Override
            public void run() {
                if (activity!= null) {
                    edit_room_title.requestFocus();
                    InputMethodManager imgr = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                    imgr.showSoftInput(edit_room_title, InputMethodManager.SHOW_IMPLICIT);
                }
            }
        });
    }

    private  void broadcastLocalUpdate(String action, String title, String room_type) {
        Bundle data_bundle = new Bundle();
        data_bundle.putString("user_action", action);
        data_bundle.putString("room_title", title);
        data_bundle.putString("room_type", room_type);
        Intent intent = new Intent("update_from_room");
        intent.putExtras(data_bundle);
        if (activity != null) {
            activity.sendBroadcast(intent);
        }
    }

    private void removefragment() {
        View view = edit_room_title;
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

        if (activity != null) {
            Fragment f = activity.getSupportFragmentManager().findFragmentById(R.id.fragment_holder);
            FragmentManager manager = activity.getSupportFragmentManager();
            FragmentTransaction trans = manager.beginTransaction();
            trans.setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_top);
            trans.remove(f);
            trans.commitAllowingStateLoss();
            manager.popBackStack();
        }
    }


    BroadcastReceiver broadCastNewMessage = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String user_action = intent
                    .getStringExtra("user_action");
            Integer uid;
            switch (user_action) {
                case "REMOVE_FRAGMENT":
                    hideKeyboard(getContext());
                    removefragment();
                    break;
            }
        }
    };


}