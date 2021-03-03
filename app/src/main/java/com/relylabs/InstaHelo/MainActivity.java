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
import com.relylabs.InstaHelo.services.ActiveRoomService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;

import static com.relylabs.InstaHelo.Utils.Helper.cleanPhoneNo;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "debug_data";

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

    private  boolean registered = false;
    @Override
    protected void onResume() {
        super.onResume();
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






    public void readContactsAndStoreLocal(){
        ContentResolver cr = MainActivity.this.getContentResolver();
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null);

        if (cur.getCount() > 0) {
            while (cur.moveToNext()) {
                String id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
                String name = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                if (Integer.parseInt(cur.getString(cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
                    // get the phone number
                    Cursor pCur = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" = ?",
                            new String[]{id}, null);

                    String phone = "";
                    while (pCur.moveToNext()) {
                        phone = pCur.getString(
                                pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    }

                    pCur.close();
                    String refinedPhone = cleanPhoneNo(phone, MainActivity.this);
                    Log.d("debug_p", refinedPhone);
                    if(!refinedPhone.equals("ERROR")){
                        if (!Contact.checkIfExists(refinedPhone)) {
                            new Contact(name, refinedPhone, false, false).save();
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        Log.d("debug_data", "activity destroyed");
        // RoomHelper.sendServiceLeaveChannel(MainActivity.this);
        super.onDestroy();
    }

    private void upload_to_server_contacts_and_return_invited_users(ArrayList<Contact> contacts) {
        if (contacts.size() == 0) {
            return;
        }

        final User user = User.getLoggedInUser();
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();

        ArrayList<String> contact_name = new ArrayList<>();
        ArrayList<String> contact_number = new ArrayList<>();

        for (int i = 0; i < contacts.size(); i++) {
            contact_name.add(contacts.get(i).getName());
            contact_number.add(contacts.get(i).getPhone());
        }

        JSONArray mJSONArray_names = new JSONArray(contact_name);
        JSONArray mJSONArray_numbers = new JSONArray(contact_number);

        params.add("contact_names", mJSONArray_names.toString());
        params.add("contact_numbers", mJSONArray_numbers.toString());

        JsonHttpResponseHandler jrep = new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    String error_message = response.getString("error_message");
                    if (error_message.equals("ERROR")) {
                        return;
                    }

                    JSONArray alread_invited_users = response.getJSONArray("invited_contacts");
                    Log.d("debug_data", "Already invited list is " + String.valueOf(alread_invited_users.length()));
                    for(int i = 0; i < alread_invited_users.length(); i++) {
                        String  phn = alread_invited_users.getString(i);
                        Contact c = Contact.getContact(phn);
                        if (c != null) {
                            c.IsInvited = Boolean.TRUE;
                            c.IsUploaded = Boolean.TRUE;
                            c.save();
                        }
                    }

                    // else mark users selected as done
                    for (int i = 0; i < contacts.size(); i++) {
                        Log.d("debug_data", "Contact marked uploaded");
                        contacts.get(i).IsUploaded =  Boolean.TRUE;
                        contacts.get(i).save();
                    }
                    Log.d("debug_data", "Contact uploaded");
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
        client.post(App.getBaseURL() + "registration/store_contacts", params, jrep);
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