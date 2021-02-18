package com.relylabs.InstaHelo.onboarding;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;


import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.text.TextWatcher;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.ybq.android.spinkit.style.FadingCircle;
import com.hbb20.CountryCodePicker;
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


public class LoginFragment extends Fragment {
    public FragmentActivity activity_ref;
    EditText phone_no;
    TextView next_phone;
    ProgressBar busy;
    TextView phone_desc;
    Boolean running = false;
    CountryCodePicker country_code;
    String phone_number_txt = "";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.login_fragment, container, false);
        phone_no = view.findViewById(R.id.edit_txt_phone);
        phone_desc = view.findViewById(R.id.phone_no_desc);
        country_code = view.findViewById(R.id.country_code);
        busy = view.findViewById(R.id.busy_send);
        FadingCircle cr = new FadingCircle();
        cr.setColor(R.color.neartagtextcolor);
        country_code = (CountryCodePicker) view.findViewById(R.id.country_code);
        busy.setIndeterminateDrawable(cr);
        running = true;
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
    public void onResume() {
        super.onResume();
        running = true;
        phone_no.post(new Runnable() {
            @Override
            public void run() {
                phone_no.requestFocus();
                if (activity_ref!= null) {
                    InputMethodManager imgr = (InputMethodManager) activity_ref.getSystemService(Context.INPUT_METHOD_SERVICE);
                    imgr.showSoftInput(phone_no, InputMethodManager.SHOW_IMPLICIT);
                }
            }
        });
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        next_phone = view.findViewById(R.id.next_phone);
        next_phone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                process(phone_number_txt);
            }
        });
        setupPhoneNo();
    }

    @Override
    public void onPause() {
        running = false;
        super.onPause();
    }

    @Override
    public void onDestroy() {
        running = false;
        super.onDestroy();
        //App.getRefWatcher(activity_ref).watch(this);
    }

    private  void setupPhoneNo() {
        phone_no.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                phone_number_txt = editable.toString();
                if (phone_number_txt.length() == 10) {
                    next_phone.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.next_enabled));
                } else {
                    next_phone.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.next_disabled));
                }
            }
        });
    }

    private void  process(String phone_number) {
        if (phone_number.length() == 10) {
            AlertDialog.Builder builder;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder = new AlertDialog.Builder(getContext(), android.R.style.Theme_DeviceDefault_Light_Dialog_Alert);
            } else {
                builder = new AlertDialog.Builder(getContext());
            }


            Logger.log(Logger.PHONE_ADD_REQUEST_START);
            builder.setMessage(getString(R.string.verify_no_msg) +  " \n\n" +country_code.getSelectedCountryCodeWithPlus() + "-" + phone_number + "\n\n" + getString(R.string.edit_no_msg))
                    .setPositiveButton(getString(R.string.ok) , new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {

                            // continue with delete
                            AsyncHttpClient client = new AsyncHttpClient();
                            RequestParams params = new RequestParams();
                            params.add("phone_no", phone_number);
                            params.add("country_code", country_code.getSelectedCountryCodeWithPlus());
                            JsonHttpResponseHandler jrep= new JsonHttpResponseHandler() {
                                @Override
                                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                                    try {
                                        String error_message = response.getString("error_message");
                                        if (!error_message.equals("SUCCESS")) {
                                            // Toast.makeText(getContext(), error_message, Toast.LENGTH_LONG).show();
                                            phone_no.setText("");
                                            return;
                                        }

                                        //Toast.makeText(getContext(), getString(R.string.otp_send_text), Toast.LENGTH_SHORT).show();
                                        busy.setVisibility(View.INVISIBLE);

                                        Integer user_id =  response.getInt("user_id");
                                        User user = User.getLoggedInUser();

                                        if(user == null) {
                                            //
                                            user = new User();
                                            user.CountryCode = country_code.getSelectedCountryCodeWithPlus();
                                            user.PhoneNo = phone_number;
                                            user.UserID = user_id;
                                            user.FirstName = "";
                                            user.LastName = "";
                                            user.IsOTPVerified = false;
                                            user.BioDescription = "";
                                            user.UserSteps = "VERIFY_OTP";
                                            //user.ProfilePicURL = response.getString("profile_image_url");
                                        } else {
                                            user.UserID = user_id;
                                            user.UserSteps = "VERIFY_OTP";
                                        }

                                        Logger.log(Logger.PHONE_ADD_REQUEST_SUCCESS);
                                        user.save();
                                        loadFragment(new PhoneVerificationFragment());
                                        // move to code verification
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
                                    Logger.log(Logger.PHONE_ADD_REQUEST_FAILED, log_data);
                                }

                                @Override
                                public void onFailure(int statusCode, Header[] headers, Throwable t, JSONObject obj) {
                                    WeakHashMap<String, String> log_data = new WeakHashMap<>();
                                    log_data.put(Logger.STATUS, Integer.toString(statusCode));
                                    if (obj != null) {
                                        log_data.put(Logger.JSON, obj.toString());
                                    }

                                    log_data.put(Logger.THROWABLE, t.toString());
                                    Logger.log(Logger.PHONE_ADD_REQUEST_FAILED, log_data);
                                }
                            };

                            busy.setVisibility(View.VISIBLE);
                            client.post( App.getBaseURL() + "registration/login_phone", params, jrep);

                        }
                    })
                    .setNegativeButton(getString(R.string.edit), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // do nothing
                            phone_no.setText("");

                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();

        }
    }

    private void loadFragment(Fragment fragment_to_start) {
        FragmentTransaction ft = activity_ref.getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragment_holder, fragment_to_start);
        ft.commit();
    }
}