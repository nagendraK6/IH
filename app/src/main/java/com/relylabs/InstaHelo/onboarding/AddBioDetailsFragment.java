package com.relylabs.InstaHelo.onboarding;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import android.app.Activity;
import android.content.Context;
import android.text.Editable;
import android.text.Selection;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.os.Bundle;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.relylabs.InstaHelo.App;
import com.relylabs.InstaHelo.R;
import com.relylabs.InstaHelo.models.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

public class AddBioDetailsFragment extends Fragment {
    public FragmentActivity activity_ref;
    EditText edit_display_bio;
    String edit_display_bio_text;
    ImageView next_btn;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bio_ask, container, false);
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
        edit_display_bio = view.findViewById(R.id.edit_display_bio);
        next_btn = view.findViewById(R.id.next_btn);
        next_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (edit_display_bio.length() > 0) {
                    sendBioToServer();
                }
            }
        });
        edit_display_bio.addTextChangedListener(new TextWatcher() {

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
                if (edit_display_bio_text.length() > 0) {
                    next_btn.setBackground(activity_ref.getDrawable(R.drawable.next_enabled));
                } else {
                    next_btn.setBackground(activity_ref.getDrawable(R.drawable.next_disabled));
                }
            }
        });
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    private void loadFragment(Fragment fragment_to_start) {
        FragmentTransaction ft = activity_ref.getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragment_holder, fragment_to_start);
        ft.commit();
    }

    private void sendBioToServer() {
        final User user = User.getLoggedInUser();
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();


        params.add("bio_description", edit_display_bio_text);

        JsonHttpResponseHandler jrep = new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    String error_message = response.getString("error_message");
                    loadFragment(new ContactRequestFragment());
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
        client.post(App.getBaseURL() + "registration/update_bio", params, jrep);
    }


    @Override
    public void onResume() {
        super.onResume();
        //  running = true;
        edit_display_bio.post(new Runnable() {
            @Override
            public void run() {
                if (activity_ref!= null) {
                    // user_name.requestFocus();
                    // user_name.setSelection(user_name.getText().length());
                    InputMethodManager imgr = (InputMethodManager) activity_ref.getSystemService(Context.INPUT_METHOD_SERVICE);
                    //imgr.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
                    imgr.showSoftInput(edit_display_bio, InputMethodManager.SHOW_IMPLICIT);
                }
            }
        });
    }
}