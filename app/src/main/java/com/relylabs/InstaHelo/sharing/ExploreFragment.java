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
                removefragment();
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
                return false;
            }
        });

        prepareRecyclerView(100);
    }



    void prepareRecyclerView(int limit) {
        all_user_profiles = new ArrayList<>();
        has_ended = false;
        recyclerView = fragment_view.findViewById(R.id.users_in_profile_list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(activity);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new ExploreListAdapter(getContext(), all_user_profiles);
        adapter.setClickListener(this);
        recyclerView.setAdapter(adapter);
        show_busy_indicator.setVisibility(View.INVISIBLE);
        scrollListener = new EndlessRecyclerViewScrollListener(layoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                // Triggered only when new data needs to be appended to the list
                // Add whatever code is needed to append new items to the bottom of the list
                loadNextDataFromApi(page, limit);
            }
        };

        recyclerView.addOnScrollListener(scrollListener);
        loadNextDataFromApi(0, 25);
    }

    void loadNextDataFromApi(int page, int limit) {
        Log.d("debug_c", "Page called " + String.valueOf(page));
        /*int old_offset = contact_numbers.size();
        Log.d("debug_c", "Fetching at " + String.valueOf(offset));
        ArrayList<Contact> new_list = readContacts(limit);
        for (int i  = 0; i < new_list.size(); i++) {
            Log.d("debug_c", new_list.get(i).Name);
            contact_names.add(new_list.get(i).Name);
            contact_numbers.add(new_list.get(i).Phone);
        }

        if (new_list.size() > 0) {
            //   adapter.notifyDataSetChanged();
            Log.d("debug_c", "adapter called");
            adapter.notifyItemRangeInserted(old_offset, new_list.size());
        }*/
        server_fetch_profiles(page, limit);
    }

    void show_busy_indicator() {
        show_busy_indicator.setVisibility(View.VISIBLE);
    }

    void hide_busy_indicator() {
        show_busy_indicator.setVisibility(View.INVISIBLE);
    }


    public void server_fetch_profiles(int page, int limit) {
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
                    JSONArray all_data = response.getJSONArray("all_users");
                    ArrayList<UserWithImage> display_users = new ArrayList<>();
                    for (int i = 0; i < all_data.length(); i++) {
                        Integer uid = all_data.getJSONObject(i).getInt("id");
                        String name = all_data.getJSONObject(i).getString("name");
                        String image_url = all_data.getJSONObject(i).getString("image_url");
                        String bio = all_data.getJSONObject(i).getString("bio");
                        String display_user_name = all_data.getJSONObject(i).getString("display_user_name");

                        UserWithImage u = new UserWithImage();
                        u.UserId = uid;
                        u.profileImageURL = image_url;
                        u.FirstName = name;
                        u.display_user_name = display_user_name;
                        u.bio = bio;
                        display_users.add(u);
                    }

                    if (display_users.size() > 0) {
                        all_user_profiles.clear();
                        all_user_profiles.addAll(display_users);
                        adapter.notifyDataSetChanged();
                    }
                    Log.d("profile_res",response.toString());
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


    private void removefragment() {
        hideKeyboard(activity);
        Log.d("debug_f", "Remove s");
        Fragment f = activity.getSupportFragmentManager().findFragmentById(R.id.fragment_holder);
        FragmentManager manager = activity.getSupportFragmentManager();
        FragmentTransaction trans = manager.beginTransaction();
        trans.remove(f);
        trans.commit();
        Log.d("debug_f", "Remove e");
        manager.popBackStack();
    }

    @Override
    public void onItemClick(int position) {

    }

    public static void hideKeyboard(Context mContext) {
        InputMethodManager imm = (InputMethodManager) mContext
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(((Activity) mContext).getWindow()
                .getCurrentFocus().getWindowToken(), 0);
    }
}
