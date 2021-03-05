package com.relylabs.InstaHelo.onboarding;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.os.Bundle;
import android.widget.Toast;

import com.github.ybq.android.spinkit.style.FadingCircle;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.relylabs.InstaHelo.App;
import com.relylabs.InstaHelo.MainScreenFragment;
import com.relylabs.InstaHelo.R;
import com.relylabs.InstaHelo.Utils.Logger;
import com.relylabs.InstaHelo.models.User;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.WeakHashMap;

import cz.msebera.android.httpclient.Header;

import com.relylabs.InstaHelo.Utils.Helper;

public class FragmentNonInvitedThankYouScreen extends Fragment {
    public FragmentActivity activity_ref;
    Boolean running = false;
    ProgressBar busy;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_thank_you_waiting, container, false);
        running = true;
        busy = view.findViewById(R.id.busy_send_server_request);
        FadingCircle cr = new FadingCircle();
        cr.setColor(R.color.neartagtextcolor);
        busy.setIndeterminateDrawable(cr);
        checkInvited();
        return view;
    }


    public void checkInvited(){
        busy.setVisibility(View.VISIBLE);
        final User user = User.getLoggedInUser();
        AsyncHttpClient client = new AsyncHttpClient();
        boolean running = false;
        RequestParams params = new RequestParams();
        SharedPreferences cached = activity_ref.getSharedPreferences("app_shared_pref", Context.MODE_PRIVATE);
        String fcm_token = cached.getString("fcm_token", null);
        params.add("fcm_token", fcm_token);

        JsonHttpResponseHandler jrep = new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    busy.setVisibility(View.INVISIBLE);
                    user.IsOTPVerified = true;
                    user.InviterImageURL = response.getString("inviter_image_url");
                    user.IsInvited = response.getBoolean("is_invited");
                    user.InviterName = response.getString("inviter_name");
                    user.InvitesCount = response.getInt("total_invites_count");
                    user.UserID = response.getInt("user_id");
                    user.UserSteps = "THANK_YOU_SCREEN";
                    user.save();


                    if (user.CompletedOnboarding) {
                        Helper.replaceFragment(new MainScreenFragment(),activity_ref);
                        return;
                    } else {
                        Helper.nextScreen(activity_ref);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable t, JSONObject obj) {
            }
        };

        client.addHeader("Accept", "application/json");
        client.addHeader("Authorization", "Token " + user.AccessToken);
        client.post(App.getBaseURL() + "registration/isInvited", params, jrep);
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
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}