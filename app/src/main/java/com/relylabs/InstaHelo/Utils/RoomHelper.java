package com.relylabs.InstaHelo.Utils;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
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
import com.relylabs.InstaHelo.services.ActiveRoomService;

import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

public class RoomHelper {

    public static void showDialogRoomCreate(FragmentActivity activity) {
        Helper.addFragmentWithTag(new RoomCreateBottomSheetDialogFragment(), activity, Constants.FRAGMENT_CREATE_ROOM_A);
    }


    public static boolean IsWAInstalled(FragmentActivity activity) {
        PackageManager pm = activity.getPackageManager();
        boolean app_installed;
        try {
            pm.getPackageInfo("com.whatsapp", PackageManager.GET_ACTIVITIES);
            app_installed = true;
        }
        catch (PackageManager.NameNotFoundException e) {
            app_installed = false;
        }
        return app_installed;
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


    public  static void broadcastToMainThread(Context activity, String action) {
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
                Log.d("debug_data", "server update success");
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

    public static void sendRoomServieStartRequest(FragmentActivity activity, String channel_name, String channel_display_name, Integer room_id) {
        Intent serviceIntent = new Intent(activity, ActiveRoomService.class);
        serviceIntent.putExtra("channel_name", channel_name);
        serviceIntent.putExtra("channel_display_name", channel_display_name);
        serviceIntent.putExtra("room_id", room_id);
        ContextCompat.startForegroundService(activity, serviceIntent);
    }

    public static void send_channel_switch(FragmentActivity activity, String channel_name, String channel_display_name, Integer room_id) {
        Bundle data_bundle = new Bundle();
        data_bundle.putString("user_action", "SWITCH_CHANNEL");
        data_bundle.putString("channel_name", channel_name);
        data_bundle.putString("channel_display_name", channel_display_name);
        data_bundle.putInt("room_id", room_id);
        Intent intent = new Intent("data_from_fragment");
        intent.putExtras(data_bundle);
        if (activity != null) {
            activity.sendBroadcast(intent);
        }
    }


    public static void sendServiceMuteUnmute(FragmentActivity activity) {
        Bundle data_bundle = new Bundle();
        data_bundle.putString("user_action", "MUTE_UNMUTE_CLICK");
        Intent intent = new Intent("data_from_fragment");
        intent.putExtras(data_bundle);
        if (activity != null) {
            activity.sendBroadcast(intent);
        }
    }

    public static void sendServiceMuteUnmuteReconnect(FragmentActivity activity) {
        Bundle data_bundle = new Bundle();
        data_bundle.putString("user_action", "MUTE_UNMUTE_CLICK_RECONNECT");
        Intent intent = new Intent("data_from_fragment");
        intent.putExtras(data_bundle);
        if (activity != null) {
            activity.sendBroadcast(intent);
        }
    }


    public static void sendServiceLeaveChannel (FragmentActivity activity) {
        Bundle data_bundle = new Bundle();
        data_bundle.putString("user_action", "LEAVE_CHANNEL_EXIT");
        Intent intent = new Intent("data_from_fragment");
        intent.putExtras(data_bundle);
        if (activity != null) {
            activity.sendBroadcast(intent);
        }
    }

    public static void send_hand_raise_request_service (FragmentActivity activity, Boolean is_clear) {
        String u_action  = "";
        if (is_clear) {
            u_action = "HAND_RAISE_CLEAR";
        } else {
            u_action = "MAKE_SPEAKER_REQUEST";
        }
        Bundle data_bundle = new Bundle();
        data_bundle.putString("user_action", u_action);
        Intent intent = new Intent("data_from_fragment");
        intent.putExtras(data_bundle);
        if (activity != null) {
            activity.sendBroadcast(intent);
        }
    }

    public static void send_make_audience_request_admin (FragmentActivity activity, Integer uid) {
        Bundle data_bundle = new Bundle();
        data_bundle.putString("user_action", "MAKE_AUDIENCE");
        data_bundle.putInt("uid", uid);
        Intent intent = new Intent("data_from_fragment");
        intent.putExtras(data_bundle);
        if (activity != null) {
            activity.sendBroadcast(intent);
        }
    }

    public static void send_make_speaker_request_admin (FragmentActivity activity, Integer uid) {
        Bundle data_bundle = new Bundle();
        data_bundle.putString("user_action", "MAKE_SPEAKER");
        data_bundle.putInt("uid", uid);
        Intent intent = new Intent("data_from_fragment");
        intent.putExtras(data_bundle);
        if (activity != null) {
            activity.sendBroadcast(intent);
        }
    }


    public static void send_reject_speaker_request_admin (FragmentActivity activity, Integer uid) {
        Bundle data_bundle = new Bundle();
        data_bundle.putString("user_action", "REJECT_SPEAKER");
        data_bundle.putInt("uid", uid);
        Intent intent = new Intent("data_from_fragment");
        intent.putExtras(data_bundle);
        if (activity != null) {
            activity.sendBroadcast(intent);
        }
    }

    public static void  send_accepted_speaker_request(FragmentActivity activity, Integer uid) {
        Bundle data_bundle = new Bundle();
        data_bundle.putInt("uid", uid);
        data_bundle.putString("user_action", "ACCEPTED_SPEAKER");
        Intent intent = new Intent("data_from_fragment");
        intent.putExtras(data_bundle);
        if (activity != null) {
            activity.sendBroadcast(intent);
        }
    }

    // audience denied
    public static void  send_denied_speaker_request(FragmentActivity activity, Integer uid) {
        Bundle data_bundle = new Bundle();
        data_bundle.putInt("uid", uid);
        data_bundle.putString("user_action", "DENIED_SPEAKER");
        Intent intent = new Intent("data_from_fragment");
        intent.putExtras(data_bundle);
        if (activity != null) {
            activity.sendBroadcast(intent);
        }
    }

    public static void send_request_to_exit_everyone(FragmentActivity activity) {
        Bundle data_bundle = new Bundle();
        data_bundle.putString("user_action", "CLOSE_ROOM");
        Intent intent = new Intent("data_from_fragment");
        intent.putExtras(data_bundle);
        if (activity != null) {
            activity.sendBroadcast(intent);
        }
    }

    public static boolean isServiceRunningInForeground(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                if (service.foreground) {
                    return true;
                }

            }
        }
        return false;
    }

    public static void ask_main_for_refresh_content(FragmentActivity activity) {
        Bundle data_bundle = new Bundle();
        data_bundle.putString("update_type", "REFRESH_FEED");
        Intent intent = new Intent("update_from_service");
        intent.putExtras(data_bundle);
        activity.sendBroadcast(intent);
    }
}
