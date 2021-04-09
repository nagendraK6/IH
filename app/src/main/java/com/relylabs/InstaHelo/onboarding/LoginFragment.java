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
import android.text.Html;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.text.TextWatcher;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.ybq.android.spinkit.style.FadingCircle;
import com.hbb20.CountryCodePicker;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.relylabs.InstaHelo.App;
import com.relylabs.InstaHelo.MainActivity;
import com.relylabs.InstaHelo.R;
import com.relylabs.InstaHelo.Utils.Logger;
import com.relylabs.InstaHelo.models.User;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.WeakHashMap;

import cz.msebera.android.httpclient.Header;

import com.relylabs.InstaHelo.Utils.Helper;


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

        final TextView textView = view.findViewById(R.id.tos_view);
        textView.setText(R.string.terms_of_service);
        textView.setMovementMethod(LinkMovementMethod.getInstance());

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
                if (phone_number_txt.length() > 0) {
                    next_phone.setBackground(activity_ref.getDrawable(R.drawable.next_enabled));
                } else {
                    next_phone.setBackground(activity_ref.getDrawable(R.drawable.next_disabled));
                }
            }
        });
    }

    private void termsAndCondition(String phone_number)  {
        if (phone_number.length() <= 1) {
            return;
        }
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(activity_ref, android.R.style.Theme_DeviceDefault_Light_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(activity_ref);
        }


        Logger.log(Logger.PHONE_ADD_REQUEST_START);
        final SpannableString s = new SpannableString(getString(R.string.terms_of_service)); // msg should have url to enable clicking
        Linkify.addLinks(s, Linkify.ALL);




        LayoutInflater inflater = this.getLayoutInflater();
        View layout = inflater.inflate(R.layout.login_dialog_fragment    /*my layout here*/, null);

        final TextView textView = layout.findViewById(R.id.tos_text);
        textView.setText(R.string.terms_of_service);
        textView.setMovementMethod(LinkMovementMethod.getInstance());


        builder.setView(layout)
                .setPositiveButton("I AGREE" , new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        process(phone_number_txt);
                    }})
                .setNegativeButton("Don't Agree", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void  process(String phone_number) {
        if (phone_number.length() > 1) {

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
                                            busy.setVisibility(View.INVISIBLE);
                                            Toast.makeText(getContext(), error_message, Toast.LENGTH_LONG).show();
                                            phone_no.setText("");
                                            return;
                                        }

                                        //Toast.makeText(getContext(), getString(R.string.otp_send_text), Toast.LENGTH_SHORT).show();
                                        busy.setVisibility(View.INVISIBLE);

                                        Integer user_id =  response.getInt("user_id");
                                        User user = User.getLoggedInUser();

                                        if (user != null) {
                                            // user back pressed and added new phone number
                                            user.delete();
                                        }

                                        user = new User();
                                        user.CountryCode = country_code.getSelectedCountryCodeWithPlus();
                                        user.PhoneNo = phone_number;
                                        user.UserID = user_id;
                                        user.FirstName = "";
                                        user.LastName = "";
                                        user.IsOTPVerified = false;
                                        user.BioDescription = "";
                                        user.UserSteps = "LOGIN";
                                        user.save();
                                        Logger.log(Logger.PHONE_ADD_REQUEST_SUCCESS);
                                        Helper.nextScreen(activity_ref);
                                        // move to code verification
                                    } catch (JSONException e) {
                                        busy.setVisibility(View.INVISIBLE);
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

                            busy.setVisibility(View.VISIBLE);
                            client.post( App.getBaseURL() + "registration/login_phone", params, jrep);

                        }
        }
}