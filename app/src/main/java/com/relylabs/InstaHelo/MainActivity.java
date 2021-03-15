package com.relylabs.InstaHelo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.WindowManager;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.relylabs.InstaHelo.Utils.Helper;
import com.relylabs.InstaHelo.Utils.RoomHelper;
import com.relylabs.InstaHelo.models.Contact;
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
import com.relylabs.InstaHelo.rooms.ScheduleRoom;
import com.relylabs.InstaHelo.services.ActiveRoomService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.Header;

import static com.relylabs.InstaHelo.Utils.Helper.cleanPhoneNo;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "debug_data";
    public static String param = "";

    BroadcastReceiver broadcastintent = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String user_action =  intent
                    .getStringExtra("user_action");
            Integer uid;
            switch (user_action) {
               /* case "contact_update":
                    Log.d("debug_data", "Received data from the other fragments to upload contact");
                    readContactsAndStoreLocal();
                    ArrayList<Contact> get_all_pending_contacts = Contact.getAllContactsNotUploaded();
                    upload_to_server_contacts_and_return_invited_users(get_all_pending_contacts);
                    break;*/
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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
        IntentFilter update_from_fragment = new IntentFilter("update_to_main_activity");
        Log.d("debug_data", "Activity registered to receive update");
        registerReceiver(broadcastintent, update_from_fragment);
        try {
            processIntents();
        } catch (Exception ex) {
            Log.d("intent", "Info in event link");
        }
    }

    private void processIntents() {
        String action = getIntent().getAction();
        Uri data =  getIntent().getData();
        if(data !=null) {
            List<String> params = data.getPathSegments();
            int size = params.size();
            if (size == 1) {
                param = params.get(0);
            }


            ScheduleRoom room = new ScheduleRoom();
            Bundle args = new Bundle();
            args.putString("room_slug", param);
            room.setArguments(args);


            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.add(R.id.fragment_holder, room);
            ft.commitAllowingStateLoss();
        }
    }

    private void setUpFragment() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragment_holder, findFragment());
        ft.commitAllowingStateLoss();
    }

    @Override
    protected void onPause() {
        Log.d("debug_data", "Activity unregistered to receive update");
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("debug_data", "Main Screen activity resume");
        Helper.sendRequestForContactProcess(MainActivity.this);
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String action = intent.getAction();
        Uri data = intent.getData();
        if(data !=null){
            List<String > params = data.getPathSegments();
            int size = params.size();
            if(size==1){
                param = params.get(0);
            }


            ScheduleRoom room = new ScheduleRoom();
            Bundle args = new Bundle();
            args.putString("room_slug",param);
            room.setArguments(args);


            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.add(R.id.fragment_holder, room);
            ft.commitAllowingStateLoss();

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("debug_data", "Instahelo on activity result called");
        Fragment uploadType = getSupportFragmentManager().findFragmentById(R.id.fragment_holder);
        if (uploadType != null) {
            Log.d("debug_data", "Fragment activity result");
            uploadType.onActivityResult(requestCode, resultCode, data);
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onDestroy() {
        Log.d("debug_data", "activity destroyed");
        // RoomHelper.sendServiceLeaveChannel(MainActivity.this);
        super.onDestroy();
    }

    private Fragment findFragment() {
        User user = User.getLoggedInUser();
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