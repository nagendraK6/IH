package com.relylabs.InstaHelo;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.ybq.android.spinkit.SpinKitView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.shape.CornerFamily;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.relylabs.InstaHelo.Utils.Logger;
import com.relylabs.InstaHelo.followerList.FollowerList;
import com.relylabs.InstaHelo.followerList.FollowingList;
import com.relylabs.InstaHelo.models.User;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.WeakHashMap;

import cz.msebera.android.httpclient.Header;


public class Profile_Screen_Fragment extends Fragment {




    SpinKitView busy;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.profile_screen_fragment, container, false);
    }
    private void removefragment() {
        Fragment f = getActivity().getSupportFragmentManager().findFragmentById(R.id.fragment_holder);
        FragmentManager manager = getActivity().getSupportFragmentManager();
        FragmentTransaction trans = manager.beginTransaction();
        trans.remove(f);
        trans.commit();
        manager.popBackStack();
    }
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        busy = view.findViewById(R.id.loading_channel_token_fetch_profile);
        final User user = User.getLoggedInUser();
        ImageView back = view.findViewById(R.id.prev_button2);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removefragment();
            }
        });
        TextView followerBtn = view.findViewById(R.id.textView12);
        followerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadFragment(new FollowerList());
            }
        });

        TextView followingBtn = view.findViewById(R.id.textView15);
        followingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadFragment(new FollowingList());
            }
        });
        ShapeableImageView prof = view.findViewById(R.id.profile_img);
        float radius = getResources().getDimension(R.dimen.default_corner_radius_profile_page);
        prof.setShapeAppearanceModel(prof.getShapeAppearanceModel()
                .toBuilder()
                .setTopRightCorner(CornerFamily.ROUNDED,radius)
                .setTopLeftCorner(CornerFamily.ROUNDED,radius)
                .setBottomLeftCorner(CornerFamily.ROUNDED,radius)
                .setBottomRightCorner(CornerFamily.ROUNDED,radius)
                .build());
        TextView name = view.findViewById(R.id.name_user);
        TextView username = view.findViewById(R.id.username);
        TextView user_bio_old = view.findViewById(R.id.user_bio);


        name.setText(user.FirstName + " " + user.LastName);
        user_bio_old.setText(user.BioDescription);
        username.setText(user.Username);
        if (!user.ProfilePicURL.equals("")) {
            Picasso.get().load(user.ProfilePicURL).into(prof);
        }
        getProfileInfo(view);

    }

    public void getProfileInfo(View view){
        show_busy_indicator();
        final User user = User.getLoggedInUser();
        AsyncHttpClient client = new AsyncHttpClient();
        boolean running = false;
        RequestParams params = new RequestParams();
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
                        ShapeableImageView inviter_img = view.findViewById(R.id.profile_img_follow);
                        if (!inviter_image.equals("")) {
                            float radius = getResources().getDimension(R.dimen.default_corner_radius_profile_inviter);
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

                        follow.setText(String.valueOf(follower));
                        following_text.setText(String.valueOf(following));
                        TextView joined = view.findViewById(R.id.joined);
                        joined.setText("Joined "+joined_at);
                        TextView nominated = view.findViewById(R.id.nominated2);
                        nominated.setText(inviter_name);
                        TextView user_bio = view.findViewById(R.id.user_bio);
                        user_bio.setText(bio);
                        user.BioDescription = bio;
                        user.save();
                    }

                    Log.d("profile_res",response.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }



            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable t, JSONObject obj) {
                WeakHashMap<String, String> log_data = new WeakHashMap<>();
                log_data.put(Logger.STATUS, Integer.toString(statusCode));
                log_data.put(Logger.JSON, obj.toString());
                log_data.put(Logger.THROWABLE, t.toString());

            }
        };

        client.addHeader("Accept", "application/json");
        client.addHeader("Authorization", "Token " + user.AccessToken);
        client.post(App.getBaseURL() + "registration/profile_info", params, jrep);
    }
    private void loadFragment(Fragment fragment_to_start) {
        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
        ft.add(R.id.fragment_holder, fragment_to_start);
        ft.commit();
    }
    void show_busy_indicator() {
        busy.setVisibility(View.VISIBLE);
    }

    void hide_busy_indicator() {
        busy.setVisibility(View.INVISIBLE);
    }
}
