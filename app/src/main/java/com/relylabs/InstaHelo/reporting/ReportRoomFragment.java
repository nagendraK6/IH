package com.relylabs.InstaHelo.reporting;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

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

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.relylabs.InstaHelo.App;
import com.relylabs.InstaHelo.MainScreenFragment;
import com.relylabs.InstaHelo.OtherProfile;
import com.relylabs.InstaHelo.R;
import com.relylabs.InstaHelo.Utils.Helper;
import com.relylabs.InstaHelo.models.User;

import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;


public class ReportRoomFragment extends Fragment {

    TextView cancel;
    EditText editText;
    ImageView submit;
    TextView report_name;
    FragmentActivity activity;
    String room_name;
    String room_id_to_report;
    String report_msg = "";
    View fragment_view;
    String source = "";
    Boolean has_reported = false;
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
        return inflater.inflate(R.layout.fragment_report_room, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        fragment_view = view;
        cancel = view.findViewById(R.id.cancel);
        editText = view.findViewById(R.id.report_text);
        submit = view.findViewById(R.id.submit);
        report_name = view.findViewById(R.id.report_name);
        source = getArguments().getString("source");
        room_name = getArguments().getString("room_name");
        report_name.setText(room_name);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Helper.removefragment(activity);
                Helper.hideKeyboard(activity);
            }
        });

        editText.addTextChangedListener(new TextWatcher() {

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
                report_msg = s.toString();
            }
        });

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (has_reported) {
                    Helper.removefragment(activity);
                    return;
                } else if(!report_msg.equals("")){
                    sendReportToServer();
                }
            }
        });
    }

    void sendReportToServer(){
        final User user = User.getLoggedInUser();
        AsyncHttpClient client = new AsyncHttpClient();
        ProgressBar busy = fragment_view.findViewById(R.id.busy);
        busy.setVisibility(View.VISIBLE);
        RequestParams params = new RequestParams();
        submit.setVisibility(View.INVISIBLE);
        params.add("room_name",room_name);
        params.add("report_msg",report_msg);
        JsonHttpResponseHandler jrep = new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                busy.setVisibility(View.INVISIBLE);
                Log.d("response",response.toString());
                has_reported = true;
                submit.setVisibility(View.VISIBLE);
                submit.setImageDrawable(activity.getDrawable(R.drawable.ok));
                editText.setVisibility(View.INVISIBLE);
                Helper.hideKeyboard(activity);
                String toast_text = "Thanks for reporting the room. We will review the report as soon as possible and will take the appropriate action";
                Helper.showToast(activity,fragment_view,getLayoutInflater(),toast_text,R.drawable.toast_red_background);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable t, JSONObject obj) {
            }
        };

        client.addHeader("Accept", "application/json");
        client.addHeader("Authorization", "Token " + user.AccessToken);
        client.post(App.getBaseURL() + "registration/report_room", params, jrep);
    }

    private void sendResult() {
        Helper.hideKeyboard(activity);
        String toast_text = "Thanks for reporting the room. We will review the report as soon as possible and will take the appropriate action";
        Helper.showToast(activity,fragment_view,getLayoutInflater(),toast_text,R.drawable.toast_red_background);
    }
}