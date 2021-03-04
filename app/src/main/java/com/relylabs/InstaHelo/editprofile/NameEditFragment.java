package com.relylabs.InstaHelo.editprofile;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.text.Editable;
import android.text.InputType;
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
import com.relylabs.InstaHelo.Utils.Logger;
import com.relylabs.InstaHelo.models.User;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.WeakHashMap;

import cz.msebera.android.httpclient.Header;

import static com.relylabs.InstaHelo.Utils.Helper.nextScreen;


public class NameEditFragment extends Fragment {

    String first_name_text = "";
    String last_name_text = "";
    Boolean isChanged = false;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_name_edit, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TextView cancel = view.findViewById(R.id.cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removefragment();
            }
        });
        final User user = User.getLoggedInUser();
        first_name_text = user.FirstName;
        last_name_text = user.LastName;
        EditText first_name = view.findViewById(R.id.name_first);
        first_name.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        EditText last_name = view.findViewById(R.id.name_last);
        last_name.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        ImageView done = view.findViewById(R.id.done_name);
        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (first_name_text.length() > 0 && isChanged) {
                    sendNameToServer(view);
                }
                else if (first_name_text.length() > 0 && !isChanged){
                    removefragment();
                }
            }
        });
        first_name.setText(user.FirstName);
        last_name.setText(user.LastName);
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
                isChanged = true;
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
                isChanged = true;

            }
        });
    }
    private void removefragment() {
        Fragment f = getActivity().getSupportFragmentManager().findFragmentById(R.id.fragment_holder);
        FragmentManager manager = getActivity().getSupportFragmentManager();
        FragmentTransaction trans = manager.beginTransaction();
        trans.remove(f);
        trans.commit();
        manager.popBackStack();
    }
    private void sendNameToServer(View view) {
        User user = User.getLoggedInUser();
        ImageView done = view.findViewById(R.id.done_name);
        done.setVisibility(View.INVISIBLE);
        ProgressBar progress = view.findViewById(R.id.loading_channel_token_fetch4);
        progress.setVisibility(View.VISIBLE);
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        params.add("first_name", first_name_text);
        params.add("last_name", last_name_text);


        JsonHttpResponseHandler jrep= new JsonHttpResponseHandler() {
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
                    user.FirstName = first_name_text;
                    user.LastName = last_name_text;
                    user.save();
                    Logger.log(Logger.USER_NAME_SEND_REQUEST_SUCCESS);
                    Log.d("response",response.toString());
                    removefragment();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String res, Throwable t) {
                WeakHashMap<String, String> log_data = new WeakHashMap<>();
                log_data.put(Logger.STATUS, Integer.toString(statusCode));
                log_data.put(Logger.RES, res);
                log_data.put(Logger.THROWABLE, t.toString());
                Logger.log(Logger.USER_NAME_SEND_REQUEST_FAILED, log_data);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable t, JSONObject obj) {
                WeakHashMap<String, String> log_data = new WeakHashMap<>();
                log_data.put(Logger.STATUS, Integer.toString(statusCode));
                log_data.put(Logger.JSON, obj.toString());
                log_data.put(Logger.THROWABLE, t.toString());
                Logger.log(Logger.USER_NAME_SEND_REQUEST_FAILED, log_data);
            }
        };

        client.addHeader("Accept", "application/json");
        client.addHeader("Authorization", "Token " + user.AccessToken);
        client.post( App.getBaseURL() + "registration/user_name_send", params, jrep);
    }
}