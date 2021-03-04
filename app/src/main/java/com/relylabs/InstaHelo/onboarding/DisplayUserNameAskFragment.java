package com.relylabs.InstaHelo.onboarding;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Editable;
import android.text.Selection;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
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

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.WeakHashMap;

import cz.msebera.android.httpclient.Header;

import static com.relylabs.InstaHelo.Utils.Helper.nextScreen;


public class DisplayUserNameAskFragment extends Fragment {

    public FragmentActivity activity_ref;
    EditText user_name;
    ProgressBar busy;
    Boolean running = false;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_display_username, container, false);

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Activity){
            activity_ref=(FragmentActivity) context;
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final User user = User.getLoggedInUser();
        user_name = view.findViewById(R.id.edit_display_user_name);
        user.UserSteps = "RESERVE_DISPLAY_USER_NAME";
        user.save();

        user_name.setText("@");
        if (!user.Username.equals("")) {
            user_name.setText(user.Username, TextView.BufferType.EDITABLE);
        }



        Selection.setSelection(user_name.getText(), user_name.getText().length());


        user_name.addTextChangedListener(new TextWatcher() {

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
                if(!s.toString().startsWith("@")){
                    user_name.setText("@");
                    Selection.setSelection(user_name.getText(), user_name.getText().length());
                }

            }
        });

        final TextView send_button = view.findViewById(R.id.send_user_name);

        busy = view.findViewById(R.id.busy_send_user_name);
        FadingCircle cr = new FadingCircle();
        cr.setColor(R.color.Black);
        busy.setIndeterminateDrawable(cr);

        running = true;
        if(user_name.getText().toString().length() > 1) {
            send_button.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.next_enabled));
        } else {
            send_button.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.next_disabled));
        }
        send_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View buttonView) {
                if (StringUtils.isEmpty(user_name.getText().toString()) || user_name.getText().toString().length() < 2) {
                    return;
                }

                busy.setVisibility(View.VISIBLE);
                send_button.setVisibility(View.INVISIBLE);
                AsyncHttpClient client = new AsyncHttpClient();
                RequestParams params = new RequestParams();
                params.add("display_user_name", user_name.getText().toString());

                SharedPreferences cached = activity_ref.getSharedPreferences("app_shared_pref", Context.MODE_PRIVATE);
                String fcm_token = cached.getString("fcm_token", null);
                params.add("fcm_token", fcm_token);

                JsonHttpResponseHandler jrep= new JsonHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                        try {
                            String error_message = response.getString("error_message");
                            if (!error_message.equals("SUCCESS")) {
                                Toast.makeText(getContext(), error_message, Toast.LENGTH_LONG).show();
                                ProgressBar busy = view.findViewById(R.id.busy_send_user_name);
                                user_name.setText("@");
                                busy.setVisibility(View.INVISIBLE);
                                send_button.setVisibility(View.VISIBLE);
                                return;
                            }



                            user.Username = user_name.getText().toString();
                            user.save();
                            Log.d("Here",user.IsInvited.toString());
                            Logger.log(Logger.USER_NAME_SEND_REQUEST_SUCCESS);
                            nextScreen(activity_ref);
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
                client.post( App.getBaseURL() + "registration/display_user_name_send", params, jrep);
            }
        });

        user_name.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if(editable.toString().length() > 1) {
                    send_button.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.next_enabled));
                } else {
                    send_button.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.next_disabled));
                }
            }
        });
    }
    @Override
    public void onResume() {
        super.onResume();
        running = true;
        user_name.post(new Runnable() {
            @Override
            public void run() {
                if (activity_ref!= null) {
                    user_name.requestFocus();
                    user_name.setSelection(user_name.getText().length());
                    InputMethodManager imgr = (InputMethodManager) activity_ref.getSystemService(Context.INPUT_METHOD_SERVICE);
                    imgr.showSoftInput(user_name, InputMethodManager.SHOW_IMPLICIT);
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        running = false;
        super.onDestroy();
        //App.getRefWatcher(activity_ref.watch(this);
    }

    @Override
    public void onPause() {
        running = false;
        super.onPause();
    }


    private void update_bio_to_server() {

    }
}