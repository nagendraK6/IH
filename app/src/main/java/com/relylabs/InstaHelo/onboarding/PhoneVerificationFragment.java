package com.relylabs.InstaHelo.onboarding;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.ybq.android.spinkit.style.FadingCircle;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.relylabs.InstaHelo.App;
import com.relylabs.InstaHelo.MainScreenFragment;
import com.relylabs.InstaHelo.R;
import com.relylabs.InstaHelo.Utils.Logger;
import com.relylabs.InstaHelo.models.User;

import android.content.Context;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.WeakHashMap;

import cz.msebera.android.httpclient.Header;

import static com.relylabs.InstaHelo.Utils.Helper.nextScreen;
import static com.relylabs.InstaHelo.Utils.Helper.prevScreen;

/**
 * Created by nagendra on 7/10/18.
 *
 */

public class PhoneVerificationFragment extends Fragment {
    public FragmentActivity activity_ref;
    String otp1_text = "";
    String otp2_text = "";
    String otp3_text = "";
    String otp4_text = "";
    Boolean should_resend_otp = true;
    TextView next_click;
    int timer = 0;
    private Handler handler = new Handler();
    EditText otp1, otp2, otp3, otp4;
    TextView phone_no_label;
    TextView phone_no;
    boolean running = false;
    ProgressBar busy;
    String otp;

    Boolean registered = false;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.phone_verification_fragment, container, false);
        final User user = User.getLoggedInUser();
        user.UserSteps = "VERIFY_OTP";
        user.save();
        busy = view.findViewById(R.id.busy_send_otp);
        FadingCircle cr = new FadingCircle();
        cr.setColor(R.color.neartagtextcolor);
        busy.setIndeterminateDrawable(cr);
        next_click = view.findViewById(R.id.submit_otp);
        next_click.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String otp = otp1_text + otp2_text + otp3_text + otp4_text;
                if (otp.length() == 4) {
                    //Toast.makeText(getContext(), otp, Toast.LENGTH_LONG).show();
                    otpSendToServer(otp, false);
                }
            }
        });

        ImageView prev_button = view.findViewById(R.id.prev_button);
        prev_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("OTP","Here in otp back");
                User user = User.getLoggedInUser();
                user.UserSteps = "LOGIN";
                user.save();
//                loadFragment(new LoginFragment());
                prevScreen(activity_ref);
            }
        });

        TextView resend = view.findViewById(R.id.didn_t_rece);
        resend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View fragment_view) {
                if (should_resend_otp) {
                    sendNewOTP();
                    // startTimer(view);
                }
            }
        });

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
    public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
        running = true;
        //phone_no_label.setText("Enter the 4-digit code we sent to\n" + user.getFormattedNo());
        otp1 = view.findViewById(R.id.otp1);
        otp2 = view.findViewById(R.id.otp2);
        otp3 = view.findViewById(R.id.otp3);
        otp4 = view.findViewById(R.id.otp4);


        otp1.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                otp1_text = editable.toString();
                String otp = otp1_text + otp2_text + otp3_text + otp4_text;
                if (otp.length() == 4) {
                    next_click.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.next_enabled));
                } else {
                    next_click.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.next_disabled));
                }

                if (editable.toString().length() == 1) {
                    otp2.requestFocus();
                }
            }
        });

        otp2.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                otp2_text = editable.toString();
                String otp = otp1_text + otp2_text + otp3_text + otp4_text;
                if (otp.length() == 4) {
                    next_click.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.next_enabled));
                } else {
                    next_click.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.next_disabled));
                }

                if (editable.toString().length() == 1) {
                    otp3.requestFocus();
                }
            }
        });

        otp3.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                otp3_text = editable.toString();
                String otp = otp1_text + otp2_text + otp3_text + otp4_text;
                if (otp.length() == 4) {
                    next_click.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.next_enabled));
                } else {
                    next_click.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.next_disabled));
                }

                if (editable.toString().length() == 1) {
                    otp4.requestFocus();
                }
            }
        });

        otp4.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                Logger.log(Logger.OTP_TYPING);
                otp4_text = editable.toString();
                String otp = otp1_text + otp2_text + otp3_text + otp4_text;
                if (otp.length() == 4) {
                    next_click.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.next_enabled));
                } else {
                    next_click.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.next_disabled));
                }
            }

        });

        TextView resend = view.findViewById(R.id.re_send);
        resend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View fragment_view) {
                if (should_resend_otp) {
                    sendNewOTP();
                    // startTimer(view);
                }
            }
        });

        User user = User.getLoggedInUser();
        user.UserSteps = "VERIFY_OTP";
        user.save();

        //startTimer(view);
    }




    private void startTimer(final View fragment_view) {
        final TextView resend = fragment_view.findViewById(R.id.re_send);
        resend.setBackgroundResource(R.drawable.disabled_text);
        resend.setTextColor(Color.BLACK);

        should_resend_otp = false;
        new Thread(new Runnable() {

            @Override
            public void run() {
                while (timer < 30) {
                    timer += 1;

                    handler.post(new Runnable() {

                        @Override
                        public void run() {
                            resend.setText("Send again in " +  (30 - timer) + " seconds");

                            // TODO Auto-generated method stub
                            if (timer == 30) {
                                should_resend_otp = true;
                                TextView resend = fragment_view.findViewById(R.id.re_send);
                                resend.setBackgroundResource(R.drawable.getstartedoval);
                                resend.setText("Send Again");
                                resend.setTextColor(Color.WHITE);
                            }
                        }
                    });


                    try {
                        // Sleep for 200 milliseconds.
                        // Just to display the progress slowly
                        Thread.sleep(1000); //thread will take approx 3 seconds to finish
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                timer = 0;
            }
        }).start();
    }

    @Override
    public void onResume() {
        super.onResume();
        running = true;
        otp1.post(new Runnable() {
            @Override
            public void run() {
                if (activity_ref!= null) {
                    otp1.requestFocus();
                    otp1.setSelection(otp1.getText().length());
                    InputMethodManager imgr = (InputMethodManager) activity_ref.getSystemService(Context.INPUT_METHOD_SERVICE);
                    imgr.showSoftInput(otp1, InputMethodManager.SHOW_IMPLICIT);
                }
            }
        });
    }

    private void sendNewOTP() {
        final User user = User.getLoggedInUser();

        otp1_text = "";
        otp2_text = "";
        otp3_text = "";
        otp4_text = "";

        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();

        JsonHttpResponseHandler jrep = new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    String error_message = response.getString("error_message");
                    if (!error_message.equals("SUCCESS")) {
                        Toast.makeText(getContext(), error_message, Toast.LENGTH_LONG).show();
                    }
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
        client.post(App.getBaseURL() + "registration/otp_resend", params, jrep);
    }

    private void loadFragment(Fragment fragment_to_start) {
        if (running && activity_ref.getSupportFragmentManager() != null) {
            FragmentTransaction ft = activity_ref.getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.fragment_holder, fragment_to_start);
            ft.commitAllowingStateLoss();
        }
    }

    @Override
    public void onDestroy() {
        running = false;
        super.onDestroy();
        if (registered && activity_ref != null) {
            registered = false;
        }
    }

    private void otpSendToServer(String otp, final Boolean auto) {
        if (!auto) {
            Logger.log(Logger.OTP_VERIFY_REQUEST_START);
        } else {
            Logger.log(Logger.AUTO_OTP_VERIFY_REQUEST_START);
        }
        busy.setVisibility(View.VISIBLE);
        final User user = User.getLoggedInUser();

        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        params.add("otp", otp);
        params.add("country_code", user.CountryCode);
        params.add("phone_no", user.PhoneNo);

        SharedPreferences cached = activity_ref.getSharedPreferences("app_shared_pref", Context.MODE_PRIVATE);
        String fcm_token = cached.getString("fcm_token", null);
        params.add("fcm_token", fcm_token);

        JsonHttpResponseHandler jrep = new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    String error_message = response.getString("error_message");

                    if (!error_message.equals("SUCCESS") && running) {
                        busy.setVisibility(View.INVISIBLE);
                        if (!auto) {
                            Toast.makeText(getContext(), error_message, Toast.LENGTH_LONG).show();
                            otp1.setText("");
                            otp2.setText("");
                            otp3.setText("");
                            otp4.setText("");
                            otp1.requestFocus();
                        }
                        return;
                    }
                    user.AccessToken = response.getString("user_token");
                    user.IsOTPVerified = true;
                    try {
                        user.InviterImageURL = response.getString("inviter_image_url");
                        user.IsInvited = response.getBoolean("is_invited");
                        user.InviterName = response.getString("inviter_name");
                        user.FirstName = response.getString("first_name");
                        user.LastName = response.getString("last_name");
                        user.ProfilePicURL = response.getString("user_profile_image_url");
                        user.BioDescription = response.getString("description_bio");
                        user.Username = response.getString("display_user_name");
                        user.CompletedOnboarding = response.getBoolean("has_completed_onboarding");
                        user.InvitesCount = response.getInt("total_invites_count");
                        user.ShowWelcomeScreen = Boolean.TRUE;
                        user.UserID = response.getInt("user_id");
                        user.IsStartRoomEnabled = response.getBoolean("IsStartRoomEnabled");
                    }
                    catch(Exception e){
                        Log.d("User_not_invited",response.toString());
                        user.InviterImageURL = "";
                        user.IsInvited = Boolean.FALSE;
                        user.IsStartRoomEnabled = Boolean.FALSE;
                        user.InviterName = "";
                        user.FirstName = "";
                        user.LastName = "";
                        user.ProfilePicURL = "";
                        user.BioDescription = "";
                        user.Username = "";
                        user.CompletedOnboarding = Boolean.FALSE;
                        user.InvitesCount = 5;
                        user.ShowWelcomeScreen = Boolean.FALSE;
                        user.UserID = 1;

                    }
                    user.save();


                    if (user.CompletedOnboarding) {
                        loadFragment(new MainScreenFragment());
                        return;
                    }
                    nextScreen(activity_ref);
                    if (!auto) {
                        Logger.log(Logger.OTP_VERIFY_REQUEST_SUCCESS);
                    } else {
                        Logger.log(Logger.AUTO_OTP_VERIFY_REQUEST_SUCCESS);
                    }
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
                if (!auto) {
                    Logger.log(Logger.OTP_VERIFY_REQUEST_FAILED, log_data);
                } else {
                    Logger.log(Logger.AUTO_OTP_VERIFY_REQUEST_FAILED, log_data);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable t, JSONObject obj) {
                WeakHashMap<String, String> log_data = new WeakHashMap<>();
                log_data.put(Logger.STATUS, Integer.toString(statusCode));
                log_data.put(Logger.JSON, obj.toString());
                log_data.put(Logger.THROWABLE, t.toString());
                if (!auto) {
                    Logger.log(Logger.OTP_VERIFY_REQUEST_FAILED, log_data);
                } else {
                    Logger.log(Logger.AUTO_OTP_VERIFY_REQUEST_FAILED, log_data);
                }
            }
        };

        client.addHeader("Accept", "application/json");
        client.post(App.getBaseURL() + "registration/verifyotp", params, jrep);
    }
}
