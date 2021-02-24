package com.relylabs.InstaHelo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;
import com.relylabs.InstaHelo.models.User;
import com.relylabs.InstaHelo.onboarding.AddBioDetailsFragment;
import com.relylabs.InstaHelo.onboarding.ContactRequestFragment;
import com.relylabs.InstaHelo.onboarding.DisplayUserNameAskFragment;
import com.relylabs.InstaHelo.onboarding.FragmentNonInvitedThankYouScreen;
import com.relylabs.InstaHelo.onboarding.FriendsToFollow;
import com.relylabs.InstaHelo.onboarding.GetStartedFragment;
import com.relylabs.InstaHelo.onboarding.InvitedUserNameAskFragment;
import com.relylabs.InstaHelo.onboarding.LoginFragment;
import com.relylabs.InstaHelo.onboarding.NonInvitedUserFirstNameAskFragment;
import com.relylabs.InstaHelo.onboarding.PhoneVerificationFragment;
import com.relylabs.InstaHelo.onboarding.PhotoAskFragment;
import com.relylabs.InstaHelo.onboarding.SuggestedProfileToFollowFragment;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "debug_data";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setUpFragment();

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                            return;
                        }

                        // Get new FCM registration token
                        String token = task.getResult();
                        SharedPreferences.Editor editor = getSharedPreferences("app_shared_pref", MODE_PRIVATE).edit();
                        editor.putString("fcm_token", token);
                        editor.commit();
                    }
                });
    }

    private void setUpFragment() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragment_holder, findFragment());
        ft.commitAllowingStateLoss();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("debug_data", "Instahelo on activity result called");
        Fragment uploadType = getSupportFragmentManager().findFragmentById(R.id.fragment_holder);
        if (uploadType != null) {
            uploadType.onActivityResult(requestCode, resultCode, data);
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private Fragment findFragment() {
        User user = User.getLoggedInUser();
        //  return new AddBioDetailsFragment();

        if (user == null) {
            return new GetStartedFragment();
        }

        if (user.UserSteps.equals("LOGIN")) {
            return new LoginFragment();
        }

        if (user.CompletedOnboarding) {
            return new MainScreenFragment();
        }

        if (user.UserSteps.equals("VERIFY_OTP")) {
            return new PhoneVerificationFragment();
        }

        if (user.IsOTPVerified == Boolean.FALSE) {
            return new PhoneVerificationFragment();
        }

        if (user.CompletedOnboarding) {
            return new MainScreenFragment();
        }

        if (user.UserSteps.equals("INVITED_NAME_ASK")) {
            return new InvitedUserNameAskFragment();
        }


        if (user.UserSteps.equals("NON_INVITED_NAME_ASK")) {
            return new NonInvitedUserFirstNameAskFragment();
        }

        if (user.Username.equals("CONTACT_REQUEST")) {
            return new ContactRequestFragment();
        }

        if (user.UserSteps.equals("RESERVE_DISPLAY_USER_NAME")) {
            return new DisplayUserNameAskFragment();
        }
        if (user.UserSteps.equals(("THANK_YOU_SCREEN"))){
            return new FragmentNonInvitedThankYouScreen();
        }
        if (user.UserSteps.equals(("PHOTO_ASK"))){
            return new PhotoAskFragment();
        }
        if (user.UserSteps.equals(("ADD_BIO"))){
            return new AddBioDetailsFragment();
        }
        if (user.UserSteps.equals(("CONTACT_REQUEST"))){
            return new ContactRequestFragment();
        }
        if (user.UserSteps.equals(("FRIENDS_TO_FOLLOW"))){
            return new FriendsToFollow();
        }
        if (user.UserSteps.equals(("SUGGESTED_PROFILE"))){
            return new SuggestedProfileToFollowFragment();
        }

        return new MainScreenFragment();
    }
}