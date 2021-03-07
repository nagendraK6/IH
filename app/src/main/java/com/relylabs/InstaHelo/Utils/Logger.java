package com.relylabs.InstaHelo.Utils;

import android.view.View;

import com.amplitude.api.Amplitude;
import com.amplitude.api.AmplitudeClient;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.relylabs.InstaHelo.App;
import com.relylabs.InstaHelo.models.User;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.WeakHashMap;

import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;


/**
 * Created by nagendra on 9/20/17.
 */

public class Logger {

    // params
    public static final String STATUS = "sttaus";
    public static final String RES = "res";
    public static final String THROWABLE = "throwable";
    public static final String JSON = "json";


    public static final String NEWS_FEED_FETCH_START = "NEWS_FEED_FETCH_START";
    public static final String NEWS_FEED_FETCH_SUCCESS = "NEWS_FEED_FETCH_SUCCESS";
    public static final String NEWS_FEED_FETCH_FAILED = "NEWS_FEED_FETCH_FAILED";

    public static final String NEWS_FEED_REFETCH_START = "NEWS_FEED_REFETCH_START";
    public static final String NEWS_FEED_REFETCH_SUCCESS = "NEWS_FEED_REFETCH_SUCCESS";
    public static final String NEWS_FEED_REFETCH_FAILED = "NEWS_FEED_REFETCH_FAILED";

    public static final String POST_CREATE_START = "POST_CREATE_START";
    public static final String POST_CREATE_SUCCESS = "POST_CREATE_SUCCESS";
    public static final String POST_CREATE_FAILED = "POST_CREATE_FAILED";

    public static final String STORY_CREATE_START = "STORY_CREATE_START";
    public static final String STORY_CREATE_SUCCESS = "STORY_CREATE_SUCCESS";
    public static final String STORY_CREATE_FAILED = "STORY_CREATE_FAILED";

    // phone no
    public static final String PHONE_ADD_REQUEST_START = "PHONE_ADD_REQUEST_START";
    public static final String PHONE_ADD_REQUEST_SUCCESS = "PHONE_ADD_REQUEST_SUCCESS";
    public static final String PHONE_ADD_REQUEST_FAILED = "PHONE_ADD_REQUEST_FAILED";

    public static final String USER_NAME_SEND_REQUEST_START = "USER_NAME_SEND_REQUEST_START";
    public static final String USER_NAME_SEND_REQUEST_SUCCESS = "USER_NAME_SEND_REQUEST_SUCCESS";
    public static final String USER_NAME_SEND_REQUEST_FAILED = "USER_NAME_SEND_REQUEST_FAILED";

    public static final String USER_LOCATION_SEND_REQUEST_START = "USER_LOCATION_SEND_REQUEST_START";
    public static final String USER_LOCATION_SEND_REQUEST_SUCCESS = "USER_LOCATION_SEND_REQUEST_SUCCESS";
    public static final String USER_LOCATION_SEND_REQUEST_FAILED = "USER_LOCATION_SEND_REQUEST_FAILED";


    public static final String USER_PREFERENCE_SEND_REQUEST_START = "USER_PREFERENCE_SEND_REQUEST_START";
    public static final String USER_PREFERENCE_SEND_REQUEST_SUCCESS = "USER_PREFERENCE_SEND_REQUEST_SUCCESS";
    public static final String USER_PREFERENCE_SEND_REQUEST_FAILED = "USER_PREFERENCE_SEND_REQUEST_FAILED";


    public static final String OTP_VERIFY_REQUEST_START = "OTP_VERIFY_REQUEST_START";
    public static final String OTP_VERIFY_REQUEST_SUCCESS = "OTP_VERIFY_REQUEST_START";
    public static final String OTP_VERIFY_REQUEST_FAILED = "OTP_VERIFY_REQUEST_FAILED";
    public static final String AUTO_OTP_VERIFY_REQUEST_START = "AUTO_OTP_VERIFY_REQUEST_START";
    public static final String AUTO_OTP_VERIFY_REQUEST_SUCCESS = "AUTO_OTP_VERIFY_REQUEST_START";
    public static final String AUTO_OTP_VERIFY_REQUEST_FAILED = "AUTO_OTP_VERIFY_REQUEST_START";

    public static final String GET_STARTED_CLICKED = "GET_STARTED_CLICKED";


    public static final String OTP_TYPING = "OTP_TYPING";
    public static final String OTP_RESEND = "OTP_RESEND";

    public static final String RECOMMENDED_LIST_FETCH_START = "RECOMMENDED_LIST_FETCH_START";
    public static final String RECOMMENDED_LIST_FETCH_SUCCESS = "RECOMMENDED_LIST_FETCH_SUCCESS";
    public static final String RECOMMENDED_LIST_FETCH_FAILED = "RECOMMENDED_LIST_FETCH_FAILED";


    public static void log(String eventName) {
        getInstance().logEvent(eventName);
    }

    public static void log(String eventName, WeakHashMap<String, String> arguments) {
        getInstance().logEvent(eventName, new JSONObject(arguments));
    }

    private static AmplitudeClient getInstance() {
        AmplitudeClient client = Amplitude.getInstance();
        User user = User.getLoggedInUser();
        if (user != null) {
            client.setUserId(Integer.toString(user.UserID));
        }

        return client;
    }

    public static void sendServerException(String stack_trac_string) {
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        params.add("stack_trace", stack_trac_string);
        User user = User.getLoggedInUser();

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

        client.addHeader("Accept", "application/json");
        client.addHeader("Authorization", "Token " + user.AccessToken);
        client.post(App.getBaseURL() + "registration/send_stack_trace", params, jrep);
    }
}