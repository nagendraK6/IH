package com.relylabs.InstaHelo;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Lifecycle;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.ybq.android.spinkit.SpinKitView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.shape.CornerFamily;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.relylabs.InstaHelo.Utils.Helper;
import com.relylabs.InstaHelo.Utils.Logger;
import com.relylabs.InstaHelo.Utils.RoomHelper;
import com.relylabs.InstaHelo.editprofile.EditBioFragment;
import com.relylabs.InstaHelo.editprofile.EditPhotoFragment;
import com.relylabs.InstaHelo.editprofile.NameEditFragment;
import com.relylabs.InstaHelo.followerList.FollowerList;
import com.relylabs.InstaHelo.followerList.FollowingList;
import com.relylabs.InstaHelo.models.User;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.WeakHashMap;

import cz.msebera.android.httpclient.Header;



public class OtherProfile extends Fragment {


    // room fragment and bottom fragment sends to main fragment to change ui if needed
    BroadcastReceiver broadCastNewMessage = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (current_user_id.equals(String.valueOf(User.getLoggedInUserID()))) {
                getProfileInfo(fragment_view);
            }
        }
    };

    public String inviterUserId = "";
    public String follow_text = "";
    ProgressBar busy;
    String current_user_id = "";
    View fragment_view;
    FragmentActivity activity;
    ImageView edit_btn_bio, edit_btn_name, edit_btn_photo;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Activity){
            activity=(FragmentActivity) context;
        }
    }

    @Override
    public void onDestroy() {
        activity.unregisterReceiver(broadCastNewMessage);
        super.onDestroy();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_other_profile, container, false);
    }



    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        fragment_view = view;
        IntentFilter new_post = new IntentFilter("update_from_follow");
        activity.registerReceiver(broadCastNewMessage, new_post);

        busy = view.findViewById(R.id.loading_channel_token_fetch);
        final User user = User.getLoggedInUser();
        current_user_id = getArguments().getString("user_id");
        edit_btn_bio = view.findViewById(R.id.edit_btn_bio);
        edit_btn_name = view.findViewById(R.id.edit_btn_name);
        edit_btn_photo = view.findViewById(R.id.edit_btn_photo);

        if(!current_user_id.equals(String.valueOf(user.UserID))){
            TextView follow_btn = view.findViewById(R.id.follow_btn);
            follow_btn.setVisibility(View.VISIBLE);
        }

        ImageView back = view.findViewById(R.id.prev_button2);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Helper.removefragment(activity);
            }
        });
        TextView followerBtn = view.findViewById(R.id.textView12);
        followerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                FollowerList follower_list = new FollowerList();
                Bundle args = new Bundle();
                args.putString("user_id",current_user_id);
                follower_list.setArguments(args);
                Helper.loadFragment(follower_list,activity);
            }
        });

        TextView followingBtn = view.findViewById(R.id.textView15);
        followingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FollowingList following_list = new FollowingList();
                Bundle args = new Bundle();
                args.putString("user_id",current_user_id);
                following_list.setArguments(args);
                Helper.loadFragment(following_list,activity);
            }
        });
        ShapeableImageView inviter_img = view.findViewById(R.id.profile_img_noti);
        inviter_img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!inviterUserId.equals("")) {
                    OtherProfile otherprof = new OtherProfile();
                    Bundle args = new Bundle();
                    args.putString("user_id",inviterUserId);
                    otherprof.setArguments(args);
                    Helper.loadFragment(otherprof,activity);
                }
            }
        });
        TextView follow_btn = view.findViewById(R.id.follow_btn);
        follow_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(follow_text.equals("Following")){
                    follow_btn.setBackground(view.getContext().getDrawable(R.drawable.follow_cta_action));
                    final User user = User.getLoggedInUser();
                    AsyncHttpClient client = new AsyncHttpClient();
                    boolean running = false;
                    RequestParams params = new RequestParams();
                    params.add("uid",current_user_id);
                    JsonHttpResponseHandler jrep = new JsonHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                            broadcastforupdate();
                            follow_text = "Follow";
                            Log.d("response_follow",response.toString());
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, Throwable t, JSONObject obj) {
                        }
                    };

                    client.addHeader("Accept", "application/json");
                    client.addHeader("Authorization", "Token " + user.AccessToken);
                    client.post(App.getBaseURL() + "registration/unfollow_user", params, jrep);
                }
                else if(follow_text.equals("Follow") ){
                    follow_btn.setBackground(view.getContext().getDrawable(R.drawable.following_state));
                    final User user = User.getLoggedInUser();
                    AsyncHttpClient client = new AsyncHttpClient();
                    boolean running = false;
                    RequestParams params = new RequestParams();
                    params.add("uid",current_user_id);
                    JsonHttpResponseHandler jrep = new JsonHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                            broadcastforupdate();
                            follow_text = "Following";
                            Log.d("response_follow",response.toString());
                        }



                        @Override
                        public void onFailure(int statusCode, Header[] headers, Throwable t, JSONObject obj) {
                        }
                    };

                    client.addHeader("Accept", "application/json");
                    client.addHeader("Authorization", "Token " + user.AccessToken);
                    client.post(App.getBaseURL() + "registration/follow_user", params, jrep);
                }

            }
        });
        String user_id = getArguments().getString("user_id");
        ShapeableImageView prof = view.findViewById(R.id.profile_img);
        TextView name = view.findViewById(R.id.name_user);
        TextView bio = view.findViewById(R.id.user_bio);

        if (String.valueOf(user.UserID).equals(user_id)) {
            edit_btn_bio.setVisibility(View.VISIBLE);
            edit_btn_photo.setVisibility(View.VISIBLE);
            edit_btn_name.setVisibility(View.VISIBLE);


            prof.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(String.valueOf(user.UserID).equals(user_id)){
                        Helper.loadFragment(new EditPhotoFragment(),activity);
                    }
                }
            });
            edit_btn_photo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(String.valueOf(user.UserID).equals(user_id)){
                        Helper.loadFragment(new EditPhotoFragment(),activity);
                    }
                }
            });


            name.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(String.valueOf(user.UserID).equals(user_id)){
                        Helper.loadFragment(new NameEditFragment(),activity);
                    }
                }
            });

            edit_btn_name.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(String.valueOf(user.UserID).equals(user_id)){
                        Helper.loadFragment(new NameEditFragment(),activity);
                    }
                }
            });


            bio.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(String.valueOf(user.UserID).equals(user_id)){
                        Helper.loadFragment(new EditBioFragment(),activity);
                    }
                }
            });

            edit_btn_bio.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(String.valueOf(user.UserID).equals(user_id)){
                        Helper.loadFragment(new EditBioFragment(),activity);
                    }
                }
            });
        }

        getProfileInfo(view);
    }

    public void getProfileInfo(View view){
        show_busy_indicator();
        String user_id = getArguments().getString("user_id");
        Log.d("user_id",user_id);
        final User user = User.getLoggedInUser();
        AsyncHttpClient client = new AsyncHttpClient();
        boolean running = false;
        RequestParams params = new RequestParams();
        params.add("user_id",user_id);
        JsonHttpResponseHandler jrep = new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                hide_busy_indicator();
                try {
                    String error_message = response.getString("error_message");
                    if(error_message.equals("SUCCESS")) {
                        int follower = response.getInt("follower_count");
                        int following = response.getInt("following_count");
                        String inviter_name = response.getString("inviter_name");
                        String inviter_image = response.getString("inviter_img");
                        String joined_at = response.getString("joined_at");
                        String bio = response.getString("bio");
                        String username_display = response.getString("username");
                        follow_text = response.getString("follow_text");
                        inviterUserId = response.getString("inviter_id");
                        TextView follow_btn = view.findViewById(R.id.follow_btn);


                        if(follow_text.equals("Following")) {
                            follow_btn.setBackground(view.getContext().getDrawable(R.drawable.following_state));
                        } else {
                            follow_btn.setBackground(view.getContext().getDrawable(R.drawable.follow_cta_action));
                        }

                        String user_name = response.getString("name");
//                        String user_username = response.getString("username");
                        String prof_url = response.getString("prof_url");
                        ShapeableImageView prof = view.findViewById(R.id.profile_img);
                        if(!prof_url.equals("")){
                            float radius = activity.getResources().getDimension(R.dimen.default_corner_radius_profile_page);
                            prof.setShapeAppearanceModel(prof.getShapeAppearanceModel()
                                    .toBuilder()
                                    .setTopRightCorner(CornerFamily.ROUNDED,radius)
                                    .setTopLeftCorner(CornerFamily.ROUNDED,radius)
                                    .setBottomLeftCorner(CornerFamily.ROUNDED,radius)
                                    .setBottomRightCorner(CornerFamily.ROUNDED,radius)
                                    .build());
                            Picasso.get().load(prof_url).into(prof);
                        }
                        ShapeableImageView inviter_img = view.findViewById(R.id.profile_img_noti);
                        if (!inviter_image.equals("")) {
                            float radius = activity.getResources().getDimension(R.dimen.default_corner_radius_profile_inviter);
                            inviter_img.setShapeAppearanceModel(inviter_img.getShapeAppearanceModel()
                                    .toBuilder()
                                    .setTopRightCorner(CornerFamily.ROUNDED, radius)
                                    .setTopLeftCorner(CornerFamily.ROUNDED, radius)
                                    .setBottomLeftCorner(CornerFamily.ROUNDED, radius)
                                    .setBottomRightCorner(CornerFamily.ROUNDED, radius)
                                    .build());
                            Picasso.get().load(inviter_image).into(inviter_img);
                        }
                        TextView follow = view.findViewById(R.id.follower);
                        TextView following_text = view.findViewById(R.id.following);
                        TextView name = view.findViewById(R.id.name_user);
                        name.setText(user_name);
                        TextView username = view.findViewById(R.id.username);
                        username.setText(username_display);
                        follow.setText(String.valueOf(follower));
                        following_text.setText(String.valueOf(following));
                        TextView joined = view.findViewById(R.id.joined);
                        joined.setText("Joined "+joined_at);
                        TextView nominated = view.findViewById(R.id.nominated2);
                        nominated.setText(inviter_name);
                        TextView user_bio = view.findViewById(R.id.user_bio);
                        user_bio.setText(bio);
                    }

                    Log.d("profile_res",response.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable t, JSONObject obj) {
            }
        };

        client.addHeader("Accept", "application/json");
        client.addHeader("Authorization", "Token " + user.AccessToken);
        client.post(App.getBaseURL() + "registration/profile_page_info", params, jrep);
    }


    private void broadcastforupdate() {
        Intent intent = new Intent("update_from_follow");
        activity.sendBroadcast(intent);
    }
    void show_busy_indicator() {
        busy.setVisibility(View.VISIBLE);
    }

    void hide_busy_indicator() {
        busy.setVisibility(View.INVISIBLE);
    }
}