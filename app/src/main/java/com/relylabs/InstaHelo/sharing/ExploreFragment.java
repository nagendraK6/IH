package com.relylabs.InstaHelo.sharing;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.shape.CornerFamily;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.relylabs.InstaHelo.App;
import com.relylabs.InstaHelo.OtherProfile;
import com.relylabs.InstaHelo.R;
import com.relylabs.InstaHelo.Utils.Logger;
import com.relylabs.InstaHelo.models.Contact;
import com.relylabs.InstaHelo.models.User;
import com.relylabs.InstaHelo.models.UserWithImage;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.WeakHashMap;

import cz.msebera.android.httpclient.Header;

import com.relylabs.InstaHelo.Utils.Helper;

public class ExploreFragment  extends Fragment implements ExploreListAdapter.ItemClickListener  {

    View fragment_view;
    RecyclerView recyclerView;
    ProgressBar show_busy_indicator;
    private FragmentActivity activity;
    EndlessRecyclerViewScrollListener scrollListener;
    ArrayList<UserWithImage> all_user_profiles;
    String query_txt = "";
    int offset  = 0;
    Boolean has_ended = false;


    ExploreListAdapter adapter;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_explore, container, false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Activity) {
            activity = (FragmentActivity) context;
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        fragment_view = view;
        final ImageView left_move = view.findViewById(R.id.move_back);
        left_move.post( new Runnable() {
            // Post in the parent's message queue to make sure the parent
            // lays out its children before we call getHitRect()
            public void run() {
                final Rect r = new Rect();
                left_move.getHitRect(r);
                r.top += 24;
                r.bottom += 24;
                r.left += 24;
                r.right += 24;
                left_move.setTouchDelegate( new TouchDelegate( r , left_move));
            }
        });


        show_busy_indicator = view.findViewById(R.id.show_busy_indicator);


        left_move.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("debug_f", "Remove started");
                Helper.hideKeyboard(activity);
                Helper.removefragment(activity);
            }
        });

        SearchView search = (SearchView) view.findViewById(R.id.search_people);
        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                search.setIconified(false);
            }
        });
        search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                query_txt = newText;
                offset = 0;
                loadNextDataFromApi(10, true);
                return false;
            }
        });

        prepareRecyclerView(10);
    }



    void prepareRecyclerView(int limit) {
        all_user_profiles = new ArrayList<>();
        has_ended = false;
        recyclerView = fragment_view.findViewById(R.id.users_in_profile_list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(activity);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new ExploreListAdapter(activity, all_user_profiles);
        adapter.setClickListener(this);
        recyclerView.setAdapter(adapter);
        show_busy_indicator.setVisibility(View.INVISIBLE);
        scrollListener = new EndlessRecyclerViewScrollListener(layoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                // Triggered only when new data needs to be appended to the list
                // Add whatever code is needed to append new items to the bottom of the list
                loadNextDataFromApi(limit, false);
            }
        };

        recyclerView.addOnScrollListener(scrollListener);
        loadNextDataFromApi(limit, true);
    }


    void show_busy_indicator() {
        show_busy_indicator.setVisibility(View.VISIBLE);
    }

    void hide_busy_indicator() {
        show_busy_indicator.setVisibility(View.INVISIBLE);
    }


    public void loadNextDataFromApi(int limit, Boolean reset) {
        show_busy_indicator();
        final User user = User.getLoggedInUser();
        AsyncHttpClient client = new AsyncHttpClient();
        boolean running = false;
        RequestParams params = new RequestParams();
        params.add("txt_pattern", query_txt);
        params.add("offset", String.valueOf(offset));
        params.add("limit", String.valueOf(offset+limit));
        JsonHttpResponseHandler jrep = new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                hide_busy_indicator();
                try {
                    JSONArray all_data = response.getJSONArray("all_users");
                    ArrayList<UserWithImage> display_users = new ArrayList<>();
                    for (int i = 0; i < all_data.length(); i++) {
                        Integer uid = all_data.getJSONObject(i).getInt("id");
                        String name = all_data.getJSONObject(i).getString("name");
                        String image_url = all_data.getJSONObject(i).getString("image_url");
                        String bio = all_data.getJSONObject(i).getString("bio");
                        String display_user_name = all_data.getJSONObject(i).getString("display_user_name");
                        Boolean has_followed = all_data.getJSONObject(i).getBoolean("has_followed");

                        UserWithImage u = new UserWithImage();
                        u.UserId = uid;
                        u.profileImageURL = image_url;
                        u.FirstName = name;
                        u.display_user_name = display_user_name;
                        u.bio = bio;
                        u.hasFollowed = has_followed;
                        display_users.add(u);
                    }

                    if (reset) {
                        Log.d("debug_c", "clear");
                        all_user_profiles.clear();
                        all_user_profiles.addAll(display_users);
                        adapter.notifyDataSetChanged();
                       // scrollListener.resetState();
                    } else {
                        // append at the bottom
                        Log.d("debug_c", "append");
                        all_user_profiles.addAll(display_users);
                        adapter.notifyItemRangeInserted(offset, display_users.size());
                    }

                    offset = offset + display_users.size();
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                hide_busy_indicator();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable t, JSONObject obj) {
            }
        };

        client.addHeader("Accept", "application/json");
        client.addHeader("Authorization", "Token " + user.AccessToken);
        client.post(App.getBaseURL() + "page/fetch_users", params, jrep);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }




    @Override
    public void onItemClick(int position) {
        OtherProfile otherprof = new OtherProfile();
        Bundle args = new Bundle();
        args.putString("user_id",String.valueOf(all_user_profiles.get(position).UserId));
        otherprof.setArguments(args);
        Helper.loadFragment(otherprof,activity);
    }
}
