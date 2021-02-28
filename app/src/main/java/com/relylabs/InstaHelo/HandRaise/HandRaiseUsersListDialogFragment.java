package com.relylabs.InstaHelo.HandRaise;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.recyclerview.widget.RecyclerView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.relylabs.InstaHelo.App;
import com.relylabs.InstaHelo.DeviceUtils;
import com.relylabs.InstaHelo.NewsFeedCardElementAdapter;
import com.relylabs.InstaHelo.PreCachingLayoutManager;
import com.relylabs.InstaHelo.R;
import com.relylabs.InstaHelo.models.User;
import com.relylabs.InstaHelo.models.UserWithImage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;

public class HandRaiseUsersListDialogFragment extends BottomSheetDialogFragment {

    RecyclerView pending_hand_raise_users;
    ArrayList<UserWithImage> all_pending_users;
    ModeratorHandRaiseAdapter adapter;
    Integer event_id = -1;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_all_hand_raise, container,
                false);

        // get the views and attach the listener

        return view;    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        pending_hand_raise_users =  view.findViewById(R.id.pending_hand_raise_users);
        all_pending_users = new ArrayList<>();
        event_id = getArguments().getInt("event_id");
        adapter = new ModeratorHandRaiseAdapter(view.getContext(), all_pending_users);
        pending_hand_raise_users.setAdapter(adapter);
        adapter.setClickListener(new ModeratorHandRaiseAdapter.ItemClickListener() {
            @Override
            public void onTagClick(int index, String action) {
                // remove the current pending user from the list and update the list
                broadcast(all_pending_users.get(index).UserId, action);
                ArrayList<UserWithImage> new_list = new ArrayList<>();
                new_list.addAll(all_pending_users);
                new_list.remove(index);
                all_pending_users.clear();
                all_pending_users.addAll(new_list);
                adapter.notifyDataSetChanged();
            }
        });
        PreCachingLayoutManager layoutManager = new PreCachingLayoutManager(view.getContext());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        layoutManager.setExtraLayoutSpace(DeviceUtils.getScreenHeight(view.getContext()));
        pending_hand_raise_users.setLayoutManager(layoutManager);
        fetch_pending_users(String.valueOf(event_id));
    }

    private void broadcast(Integer user_id, String action) {
        Bundle data_bundle = new Bundle();
        data_bundle.putString("user_action", action);
        data_bundle.putInt("uid", user_id);
        Intent intent = new Intent("update_from_room");
        intent.putExtras(data_bundle);
        if (getActivity() != null) {
            getActivity().sendBroadcast(intent);
        }
    }

    private void fetch_pending_users(String event_id) {
        final User user = User.getLoggedInUser();
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        params.add("event_id", event_id);
        JsonHttpResponseHandler jrep = new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    JSONArray all_users = response.getJSONArray("all_users");
                    ArrayList<UserWithImage> all_pending_invites = new ArrayList<>();
                    for (int i = 0; i < all_users.length(); i++) {
                        JSONObject obj = all_users.getJSONObject(i);
                        Integer uid = obj.getInt("id");
                        String name = obj.getString("name");
                        String image_url = obj.getString("image_url");
                        UserWithImage u = new UserWithImage();
                        u.UserId = uid;
                        u.profileImageURL = image_url;
                        u.FirstName = name;
                        all_pending_invites.add(u);

                        all_pending_users.clear();
                        all_pending_users.addAll(all_pending_invites);
                        adapter.notifyDataSetChanged();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String res, Throwable t) {
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable t, JSONObject obj) {
            }
        };

        client.addHeader("Accept", "application/json");
        client.addHeader("Authorization", "Token " + user.AccessToken);
        client.post(App.getBaseURL() + "page/fetch_pending_raise_hands", params, jrep);
    }
}
