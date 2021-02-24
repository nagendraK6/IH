package com.relylabs.InstaHelo.Utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.SyncHttpClient;
import com.relylabs.InstaHelo.App;
import com.relylabs.InstaHelo.R;
import com.relylabs.InstaHelo.ServerCallBack;
import com.relylabs.InstaHelo.models.User;
import com.relylabs.InstaHelo.rooms.RoomCreateBottomSheetDialogFragment;

import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

public class RoomHelper {

    public static void showDialogRoomCreate(FragmentActivity activity) {
        Fragment fr = new RoomCreateBottomSheetDialogFragment();
        FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
        ft.setCustomAnimations(R.anim.slide_in_bottom, R.anim.slide_out_top);
        ft.add(R.id.fragment_holder, fr);
        ft.commit();
    }

    public static  boolean IsInternetConnect(FragmentActivity activity) {
        if (activity != null) {
            ConnectivityManager cm = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            if (null != activeNetwork) {
                if(activeNetwork.getType() != ConnectivityManager.TYPE_WIFI &&
                        activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                    return false;

                }
            } else {
                return  false;
            }
        }

        return true;
    }


    public  static void broadcastToMainThread(Activity activity, String action) {
        Bundle data_bundle = new Bundle();
        data_bundle.putString("user_action", action);
        Intent intent = new Intent("update_from_room");
        intent.putExtras(data_bundle);
        if (activity != null) {
            activity.sendBroadcast(intent);
        }
    }

    public static  void send_hand_raise_request(Integer event_id, Boolean clear) {
        final User user = User.getLoggedInUser();
        AsyncHttpClient clienta = null;
            clienta = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        params.add("event_id", String.valueOf(event_id));
        params.add("should_clear", String.valueOf(clear == Boolean.TRUE ? 1 : 0));

        JsonHttpResponseHandler jrep = new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String res, Throwable t) {
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable t, JSONObject obj) {
            }
        };

            clienta.addHeader("Accept", "application/json");
            clienta.addHeader("Authorization", "Token " + user.AccessToken);
            clienta.post(App.getBaseURL() + "page/raise_hand", params, jrep);
    }

    public static void server_update(Integer user_id, String event_id, String user_action, ServerCallBack callback) {
        final User user = User.getLoggedInUser();
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        params.add("user_id", String.valueOf(user_id));
        params.add("event_id", event_id);
        params.add("user_action", user_action);

        JsonHttpResponseHandler jrep = new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                callback.onSuccess();
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
        client.post(App.getBaseURL() + "page/update_action", params, jrep);
    }

    public static  void sendPing(Integer event_id) {
        User user = User.getLoggedInUser();
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        params.add("event_id", String.valueOf(event_id));


        JsonHttpResponseHandler jrep= new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                Log.d("debug_data", "sent ping success");
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
        client.post( App.getBaseURL() + "page/send_a_ping", params, jrep);
    }
}
