package com.relylabs.InstaHelo;

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
import com.relylabs.InstaHelo.Utils.Helper;
import com.relylabs.InstaHelo.models.User;

import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;


public class ReportProfile extends Fragment {

    TextView cancel;
    EditText editText;
    ImageView submit;
    TextView report_name;
    FragmentActivity activity;
    String name;
    String id_to_report;
    String report_msg = "";
    View fragment_view;
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
        return inflater.inflate(R.layout.fragment_report_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        fragment_view = view;
        cancel = view.findViewById(R.id.cancel);
        editText = view.findViewById(R.id.report_text);
        submit = view.findViewById(R.id.submit);
        report_name = view.findViewById(R.id.report_name);
        name = getArguments().getString("name");
        id_to_report = getArguments().getString("user_id");
        report_name.setText("Report " + name);
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
                if(!report_msg.equals("")){
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
        params.add("id_to_report",id_to_report);
        params.add("reporter_id",user.UserID.toString());
        params.add("report_msg",report_msg);
        JsonHttpResponseHandler jrep = new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                busy.setVisibility(View.INVISIBLE);
                Log.d("response",response.toString());
                sendResult();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable t, JSONObject obj) {
            }
        };

        client.addHeader("Accept", "application/json");
        client.addHeader("Authorization", "Token " + user.AccessToken);
        client.post(App.getBaseURL() + "registration/report_user", params, jrep);
    }

    private void sendResult() {
        if( getTargetFragment() == null ) {
            return;
        }
        Helper.hideKeyboard(activity);
        Intent intent = OtherProfile.newIntent("show_toast");
        getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, intent);
        Helper.removefragment(activity);
    }
}