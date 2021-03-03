package com.relylabs.InstaHelo.notification;

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


public class NotificationList extends Fragment {


    private  ArrayList<String> text_arr = new ArrayList<String>();
    private  ArrayList<String> username = new ArrayList<String>();
    private  ArrayList<String> img_arr = new ArrayList<String>();
    private  ArrayList<String> time_arr = new ArrayList<String>();
    private  ArrayList<String> currStatus = new ArrayList<>();
    View fragment_view;
    RecyclerView recyclerView;
    NotificationListAdapter adapter;
    SpinKitView busy;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notification_list, container, false);
    }
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        busy = view.findViewById(R.id.loading_channel_token_fetch2);
        fragment_view = view;
        ImageView back = view.findViewById(R.id.prev_button_notification);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removefragment();
            }
        });
        getNotifications();
        prepareRecyclerView();

    }
    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    public void getNotifications(){
        show_busy_indicator();
        final User user = User.getLoggedInUser();
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();

        JsonHttpResponseHandler jrep = new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                hide_busy_indicator();
                try {
                    Log.d("response_noti", response.toString());
                    JSONArray text_json = response.getJSONArray("text_arr");
                    JSONArray user_name = response.getJSONArray("username");
                    JSONArray img_arr_json = response.getJSONArray("img_arr");
                    JSONArray time_arr_json = response.getJSONArray("time_arr");
                    if (text_json != null) {
                        for (int i = 0; i < text_json.length(); i++) {
                            text_arr.add(text_json.getString(i));
                            username.add(user_name.getString(i));
                            img_arr.add(img_arr_json.getString(i));
                            time_arr.add(time_arr_json.getString(i));
                        }
                        adapter.notifyDataSetChanged();
                    }
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }
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
        client.post(App.getBaseURL() + "registration/get_notifications", params, jrep);
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
        ft.add(R.id.fragment_holder, fragment_to_start);
        ft.commitAllowingStateLoss();
    }
    void prepareRecyclerView() {
        recyclerView = fragment_view.findViewById(R.id.list_notification);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 1));
        adapter = new NotificationListAdapter(getContext(), text_arr, username,img_arr,time_arr);
        recyclerView.setAdapter(adapter);

    }
    void show_busy_indicator() {
        busy.setVisibility(View.VISIBLE);
    }

    void hide_busy_indicator() {
        busy.setVisibility(View.INVISIBLE);
    }
}