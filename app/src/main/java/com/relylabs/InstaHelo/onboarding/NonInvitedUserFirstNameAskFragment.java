package com.relylabs.InstaHelo.onboarding;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.ybq.android.spinkit.style.FadingCircle;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.relylabs.InstaHelo.App;
import com.relylabs.InstaHelo.R;
import com.relylabs.InstaHelo.Utils.Logger;
import com.relylabs.InstaHelo.models.User;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.WeakHashMap;

import cz.msebera.android.httpclient.Header;
import de.hdodenhof.circleimageview.CircleImageView;

import com.squareup.picasso.Picasso;

public class NonInvitedUserFirstNameAskFragment extends Fragment {
    public FragmentActivity activity_ref;

    EditText first_name, last_name;
    ProgressBar busy;
    Boolean running = true;

    String first_name_text = "";
    String last_name_text = "";
    TextView submit_first_name_last_name;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_non_invited_name_ask, container, false);
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Activity){
            activity_ref=(FragmentActivity) context;
        }
    }

    private void loadFragment(Fragment fragment_to_start) {
        if (running) {
            FragmentTransaction ft = activity_ref.getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.fragment_holder, fragment_to_start);
            ft.commitAllowingStateLoss();
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        User user = User.getLoggedInUser();


        busy = view.findViewById(R.id.busy_send_user_name);
        FadingCircle cr = new FadingCircle();
        cr.setColor(R.color.Black);
        busy.setIndeterminateDrawable(cr);

        first_name =  view.findViewById(R.id.first_name);
        last_name =  view.findViewById(R.id.last_name);
        submit_first_name_last_name = view.findViewById(R.id.submit_first_name_last_name);
        submit_first_name_last_name.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (first_name_text.length() > 0) {
                    sendNameToServer();
                }
            }
        });


        first_name.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                first_name_text = editable.toString();
                if (first_name_text.length() > 0) {
                    submit_first_name_last_name.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.next_enabled));
                } else {
                    submit_first_name_last_name.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.next_disabled));
                }
            }
        });

        last_name.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                last_name_text = editable.toString();
            }
        });

        user.UserSteps = "NON_INVITED_NAME_ASK";
        user.save();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        running = false;
        super.onDestroy();
        //App.getRefWatcher(activity_ref()).watch(this);
    }

    private void sendNameToServer() {
        User user = User.getLoggedInUser();
        busy.setVisibility(View.VISIBLE);
        submit_first_name_last_name.setVisibility(View.INVISIBLE);

        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        params.add("first_name", first_name_text);
        params.add("last_name", last_name_text);


        JsonHttpResponseHandler jrep= new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    String error_message = response.getString("error_message");
                    if (!error_message.equals("SUCCESS")) {
                        Toast.makeText(getContext(), error_message, Toast.LENGTH_LONG).show();

                        busy.setVisibility(View.INVISIBLE);
                        submit_first_name_last_name.setVisibility(View.VISIBLE);
                        return;
                    }



                    user.FirstName = first_name_text;
                    user.LastName = last_name_text;
                    user.save();
                    Logger.log(Logger.USER_NAME_SEND_REQUEST_SUCCESS);
                    loadFragment(new DisplayUserNameAskFragment());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
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
        client.post( App.getBaseURL() + "registration/user_name_send", params, jrep);
    }

    @Override
    public void onPause() {
        running = false;
        super.onPause();
    }
}