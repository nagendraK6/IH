package com.relylabs.InstaHelo.onboarding;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.os.Bundle;

import com.relylabs.InstaHelo.MainScreenFragment;
import com.relylabs.InstaHelo.R;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.os.Bundle;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.relylabs.InstaHelo.App;
import com.relylabs.InstaHelo.R;
import com.relylabs.InstaHelo.Utils.Helper;
import com.relylabs.InstaHelo.models.Contact;
import com.relylabs.InstaHelo.models.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;

import com.relylabs.InstaHelo.Utils.Helper;

public class SuggestedProfileToFollowFragment extends Fragment  implements SuggestedProfileToFollowAdapter.ItemClickListener  {
    public FragmentActivity activity_ref;
    ArrayList<String> suggested_names, suggested_bios, suggested_profile_image_urls;
    View fragment_view;

    RecyclerView recyclerView;
    SuggestedProfileToFollowAdapter adapter;

    ArrayList<Integer> user_ids_to_follow;
    ArrayList<Boolean> user_ids_to_send_status;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        fragment_view =  inflater.inflate(R.layout.fragment_suggested_profiles, container, false);
        final User user = User.getLoggedInUser();
        user.UserSteps = "SUGGESTED_PROFILE";
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
        fetch_data();
    }


    private void setupFriendSuggestion() {
        recyclerView = fragment_view.findViewById(R.id.suggested_profiles_list);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 1));
        adapter = new SuggestedProfileToFollowAdapter(getContext(), suggested_names, suggested_bios, suggested_profile_image_urls);
        adapter.setClickListener(this);
        recyclerView.setAdapter(adapter);
        ImageView looks_good_to_me = fragment_view.findViewById(R.id.looks_good_to_me);
        looks_good_to_me.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                send_follow_to_server();
                final User user = User.getLoggedInUser();
                user.UserSteps = "MAIN_SCREEN";
                user.save();
            }
        });
    }

    private void fetch_data() {
        final User user = User.getLoggedInUser();
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        JsonHttpResponseHandler jrep = new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    String error_message = response.getString("error_message");
                    JSONArray all_contacts_to_follow = response.getJSONArray("all_contacts");
                    suggested_names = new ArrayList<>();
                    suggested_profile_image_urls = new ArrayList<>();
                    suggested_bios = new ArrayList<>();
                    user_ids_to_follow = new ArrayList<>();
                    user_ids_to_send_status = new ArrayList<>();
                    for (int i  = 0; i < all_contacts_to_follow.length(); i++) {
                        JSONObject contact_info = all_contacts_to_follow.getJSONObject(i);
                        suggested_names.add(contact_info.getString("name"));
                        suggested_profile_image_urls.add(contact_info.getString("profile_image_url"));
                        suggested_bios.add(contact_info.getString("bio_description"));
                        user_ids_to_follow.add(contact_info.getInt("user_id"));
                        user_ids_to_send_status.add(true);
                    }

                    if (all_contacts_to_follow.length() == 0) {
                        Helper.replaceFragment(new DarkPatternInviteFragment(),activity_ref);
                    } else {
                        setupFriendSuggestion();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String res, Throwable t) {
                Log.d("debug_data", "" + res);
                Helper.replaceFragment(new DarkPatternInviteFragment(),activity_ref);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject obj) {
                Helper.replaceFragment(new DarkPatternInviteFragment(),activity_ref);
            }
        };

        client.addHeader("Accept", "application/json");
        client.addHeader("Authorization", "Token " + user.AccessToken);
        client.post(App.getBaseURL() + "registration/sugested_profiles_to_follow", params, jrep);
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
                Helper.replaceFragment(new DarkPatternInviteFragment(), activity_ref);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String res, Throwable t) {
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject obj) {
            }
        };

        client.addHeader("Accept", "application/json");
        client.addHeader("Authorization", "Token " + user.AccessToken);
        client.post(App.getBaseURL() + "registration/add_users_to_follow", params, jrep);
    }


    @Override
    public void onItemClick(View view, int position, Boolean selected) {
        user_ids_to_send_status.set(position, selected);
    }
}