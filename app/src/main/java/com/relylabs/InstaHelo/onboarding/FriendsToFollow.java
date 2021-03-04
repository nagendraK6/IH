package com.relylabs.InstaHelo.onboarding;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.os.Bundle;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.relylabs.InstaHelo.App;
import com.relylabs.InstaHelo.MainScreenFragment;
import com.relylabs.InstaHelo.R;
import com.relylabs.InstaHelo.models.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;


public class FriendsToFollow extends Fragment  implements FriendToFollowListAdapter.ItemClickListener  {
    public FragmentActivity activity_ref;
    ArrayList<String> contact_names, contact_numbers;
    ArrayList<String> suggested_names, suggested_profile_image_urls;
    View fragment_view;

    RecyclerView recyclerView;
    FriendToFollowListAdapter adapter;
    ArrayList<Integer> user_ids_to_follow;
    ArrayList<Boolean> user_ids_to_send_status;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        fragment_view =  inflater.inflate(R.layout.friends_suggestion, container, false);
        final User user = User.getLoggedInUser();
        user.UserSteps = "FRIENDS_TO_FOLLOW";
        user.save();
        return fragment_view;
    }
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Activity){
            activity_ref=(FragmentActivity) context;
        }
    }
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        processContacts();
    }


    private void setupFriendSuggestion() {

        // set up the RecyclerView
        recyclerView = fragment_view.findViewById(R.id.friend_list_display);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        adapter = new FriendToFollowListAdapter(getContext(), suggested_names, suggested_profile_image_urls);
        adapter.setClickListener(this);
        recyclerView.setAdapter(adapter);
        ImageView looks_good_to_me = fragment_view.findViewById(R.id.looks_good_to_me);
        looks_good_to_me.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                send_follow_to_server();
                // loadFragment(new SuggestedProfileToFollowFragment());
            }
        });
    }

    private void processContacts() {
        suggested_names = new ArrayList<>();
        suggested_profile_image_urls = new ArrayList<>();
        user_ids_to_follow = new ArrayList<>();
        user_ids_to_send_status = new ArrayList<>();
        fetch_contacts();
    }

    private void fetch_contacts() {
        final User user = User.getLoggedInUser();
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        JsonHttpResponseHandler jrep = new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    String error_message = response.getString("error_message");
                    JSONArray all_contacts_to_follow = response.getJSONArray("all_contacts");
                    if (all_contacts_to_follow.length() == 0) {
                        loadFragment(new MainScreenFragment());
                        return;
                       // nextScreen(activity_ref);
                    }


                    for (int i  = 0; i < all_contacts_to_follow.length(); i++) {
                        JSONObject contact_info = all_contacts_to_follow.getJSONObject(i);
                        suggested_names.add(contact_info.getString("name"));
                        suggested_profile_image_urls.add(contact_info.getString("profile_image_url"));
                        user_ids_to_follow.add(contact_info.getInt("user_id"));
                        user_ids_to_send_status.add(true);
                    }

                    setupFriendSuggestion();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String res, Throwable t) {
                loadFragment(new MainScreenFragment());
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject obj) {
                loadFragment(new MainScreenFragment());
            }
        };

        client.addHeader("Accept", "application/json");
        client.addHeader("Authorization", "Token " + user.AccessToken);
        client.post(App.getBaseURL() + "registration/get_suggested_friends", params, jrep);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void send_follow_to_server() {
        final User user = User.getLoggedInUser();
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();

        ArrayList<Integer> users_to_follow = new ArrayList<>();
        for (int i  =0 ; i < user_ids_to_send_status.size(); i++) {
            if (user_ids_to_send_status.get(i).equals(Boolean.TRUE)) {
                users_to_follow.add(user_ids_to_follow.get(i));
            }
        }

        JSONArray users_to_follow_json = new JSONArray(users_to_follow);

        params.add("users_to_follow_json", users_to_follow_json.toString());

        JsonHttpResponseHandler jrep = new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                loadFragment(new MainScreenFragment());
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String res, Throwable t) {
                loadFragment(new MainScreenFragment());
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject obj) {
                loadFragment(new MainScreenFragment());
            }
        };

        client.addHeader("Accept", "application/json");
        client.addHeader("Authorization", "Token " + user.AccessToken);
        client.post(App.getBaseURL() + "registration/add_users_to_follow", params, jrep);
    }


    private void loadFragment(Fragment fragment_to_start) {
        FragmentTransaction ft = activity_ref.getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragment_holder, fragment_to_start);
        ft.commitAllowingStateLoss();
    }

    @Override
    public void onItemClick(View view, int position, Boolean selected) {
        user_ids_to_send_status.set(position, selected);
    }
}