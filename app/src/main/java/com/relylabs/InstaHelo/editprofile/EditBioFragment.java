package com.relylabs.InstaHelo.editprofile;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.relylabs.InstaHelo.App;
import com.relylabs.InstaHelo.R;
import com.relylabs.InstaHelo.models.User;

import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

import com.relylabs.InstaHelo.Utils.Helper;

public class EditBioFragment extends Fragment {
    String edit_display_bio_text = "";
    Boolean isChanged = false;
    public FragmentActivity activity;
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Activity){
            activity=(FragmentActivity) context;
        }
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_edit_bio, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        activity = getActivity();
        TextView cancel = view.findViewById(R.id.cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Helper.removefragment(activity);
            }
        });

        final User user = User.getLoggedInUser();
        edit_display_bio_text = user.BioDescription;
        EditText bio = view.findViewById(R.id.edit_display_bio_update);
        ImageView done = view.findViewById(R.id.done_bio);
        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (edit_display_bio_text.length() > 0 && isChanged) {
                    sendBioToServer(view);
                }
                else if (edit_display_bio_text.length() > 0 && !isChanged){
                    Helper.removefragment(activity);
                }
            }
        });
        bio.setText(user.BioDescription);
        bio.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // TODO Auto-generated method stub

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
                // TODO Auto-generated method stub

            }

            @Override
            public void afterTextChanged(Editable s) {
                edit_display_bio_text = s.toString();
                isChanged = true;
            }
        });
    }

    private void sendBioToServer(View view) {
        final User user = User.getLoggedInUser();
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        ImageView done = view.findViewById(R.id.done_bio);
        done.setVisibility(View.INVISIBLE);
        params.add("bio_description", edit_display_bio_text);
        ProgressBar progress = view.findViewById(R.id.loading_channel_token_fetch6);
        progress.setVisibility(View.VISIBLE);
        JsonHttpResponseHandler jrep = new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    String error_message = response.getString("error_message");
                    progress.setVisibility(View.INVISIBLE);
                    if (!error_message.equals("SUCCESS")) {
                        Toast.makeText(getContext(), error_message, Toast.LENGTH_LONG).show();
                        done.setVisibility(View.VISIBLE);
                        return;
                    }
                    user.BioDescription = edit_display_bio_text;
                    user.save();
                    Helper.removefragment(activity);;

                } catch (JSONException e) {
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
        client.post(App.getBaseURL() + "registration/update_bio", params, jrep);
    }
}