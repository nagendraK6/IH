package com.relylabs.InstaHelo.followerList;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.github.ybq.android.spinkit.SpinKitView;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.relylabs.InstaHelo.App;
import com.relylabs.InstaHelo.Profile_Screen_Fragment;
import com.relylabs.InstaHelo.R;
import com.relylabs.InstaHelo.models.Contact;
import com.relylabs.InstaHelo.models.User;
import com.relylabs.InstaHelo.sharing.SharingContactListAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;


public class FollowingList extends Fragment {


    private  ArrayList<String> names = new ArrayList<String>();
    private  ArrayList<String> usernames = new ArrayList<String>();
    private  ArrayList<String> bio = new ArrayList<String>();
    private  ArrayList<String> img = new ArrayList<String>();
    private  ArrayList<String> currStatus = new ArrayList<>();
    SpinKitView busy;
    View fragment_view;
    RecyclerView recyclerView;
    FollowingListAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_following_list, container, false);
    }
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        busy = view.findViewById(R.id.loading_channel_token_fetch2);
        fragment_view = view;
        View back = view.findViewById(R.id.prev_button_follow);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removefragment();
//                loadFragment(new Profile_Screen_Fragment());
            }
        });
        getFollowers();
        prepareRecyclerView();

    }
    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    public void getFollowers(){
        show_busy_indicator();
        final User user = User.getLoggedInUser();
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();

        JsonHttpResponseHandler jrep = new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    hide_busy_indicator();
                    String error_message = response.getString("error_message");
                    Log.d("/followers",response.toString());
                    JSONArray name = response.getJSONArray("names");
                    JSONArray username = response.getJSONArray("usernames");
                    JSONArray bio_temp = response.getJSONArray("bio");
                    JSONArray img_temp = response.getJSONArray(("img"));
                    if(name!=null){
                        for (int i=0;i<name.length();i++){
                            names.add(name.getString(i));
                            currStatus.add("Following");
                        }
                    }
                    if(username!=null){
                        for (int i=0;i<username.length();i++){
                            usernames.add(username.getString(i));
                        }
                    }
                    if(bio_temp!=null){
                        for(int i=0;i<bio_temp.length();i++){
                            bio.add(bio_temp.getString(i));
                        }
                    }
                    if(img_temp!=null){
                        for(int i=0;i<img_temp.length();i++){
                            img.add(img_temp.getString(i));
                        }
                    }
                    adapter.notifyDataSetChanged();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String res, Throwable t) {
                Log.d("debug_data", "" + res);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject obj) {
            }
        };

        client.addHeader("Accept", "application/json");
        client.addHeader("Authorization", "Token " + user.AccessToken);
        client.post(App.getBaseURL() + "registration/following_list", params, jrep);
    }


    private void removefragment() {
        Fragment f = getActivity().getSupportFragmentManager().findFragmentById(R.id.fragment_holder);
        FragmentManager manager = getActivity().getSupportFragmentManager();
        FragmentTransaction trans = manager.beginTransaction();
        trans.remove(f);
        trans.commitAllowingStateLoss();
        manager.popBackStack();
    }
    private void loadFragment(Fragment fragment_to_start) {
        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragment_holder, fragment_to_start);
        ft.commitAllowingStateLoss();
    }
    void prepareRecyclerView() {
        recyclerView = fragment_view.findViewById(R.id.list_follower);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 1));
        adapter = new FollowingListAdapter(getContext(), names, usernames,bio, img,currStatus);
//        adapter.setClickListener(this);
        recyclerView.setAdapter(adapter);

    }
    void show_busy_indicator() {
        busy.setVisibility(View.VISIBLE);
    }

    void hide_busy_indicator() {
        busy.setVisibility(View.INVISIBLE);
    }
}