package com.relylabs.InstaHelo.Utils;

import com.amplitude.api.Amplitude;
import com.amplitude.api.AmplitudeClient;
import com.relylabs.InstaHelo.models.User;

import java.util.WeakHashMap;

import org.json.JSONObject;


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
}