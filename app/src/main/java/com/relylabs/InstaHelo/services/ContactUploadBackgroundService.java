package com.relylabs.InstaHelo.services;

import android.Manifest;
import android.app.Activity;
import android.app.IntentService;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.provider.ContactsContract;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.JobIntentService;
import androidx.core.content.ContextCompat;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.SyncHttpClient;
import com.relylabs.InstaHelo.App;
import com.relylabs.InstaHelo.MainActivity;
import com.relylabs.InstaHelo.models.Contact;
import com.relylabs.InstaHelo.models.RelySystem;
import com.relylabs.InstaHelo.models.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;

import static com.relylabs.InstaHelo.Utils.Helper.cleanPhoneNo;

public class ContactUploadBackgroundService extends Service {

    public ContactUploadBackgroundService() {
        super();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("debug_data", "All work complete");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //if (!checkPermission()) {
        //    return Service.START_NOT_STICKY;
       // }

        /*RelySystem rs = RelySystem.getSystemSettings();
        ContactsState cs = getContactsToupload(rs.last_contact_cursor_uploaded, 500);
        upload_to_server_contacts_and_return_invited_users(cs);*/
        return Service.START_NOT_STICKY;
    }


    /*private  class ContactsState {
        public ArrayList<Contact> contacts;
        public Integer new_offset;

        public ContactsState(ArrayList<Contact> all_c, Integer new_o){
            contacts = all_c;
            new_offset = new_o;
        }
    }

    public void processContacts() {
        RelySystem rs = RelySystem.getSystemSettings();
        ContactsState cs = getContactsToupload(rs.last_contact_cursor_uploaded, 500);
        upload_to_server_contacts_and_return_invited_users(cs);
    }

    public ContactsState getContactsToupload(int offset, int limit){
        ArrayList<Contact> contacts_to_upload = new ArrayList<>();
        ContentResolver cr = getContentResolver();
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null);

        int i = 0;
        if (cur.getCount() > 0) {
            cur.moveToPosition(offset);
            while (cur.moveToNext()) {
                offset = offset + 1;
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
                    String refinedPhone = cleanPhoneNo(phone, getApplicationContext());

                    if (name  == null) {
                        Log.d("debug_c", "name is null. phone is " + phone);
                    }
                    contacts_to_upload.add(new Contact(name, refinedPhone, false, false));
                    i++;
                    if (i >= limit) {
                        break;
                    }
                }
            }
        }

        return new ContactsState(contacts_to_upload, offset);
    }

    private void upload_to_server_contacts_and_return_invited_users(ContactsState cs) {
        Log.d("debug_data", "no more contacts upload");
        if (cs.contacts.size() == 0) {
            return;
        }

        final User user = User.getLoggedInUser();
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();

        ArrayList<String> contact_name = new ArrayList<>();
        ArrayList<String> contact_number = new ArrayList<>();

        for (int i = 0; i < cs.contacts.size(); i++) {
            contact_name.add(cs.contacts.get(i).getName());
            contact_number.add(cs.contacts.get(i).getPhone());
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
                        RelySystem rs = RelySystem.getSystemSettings();
                        rs.last_contact_cursor_uploaded = cs.new_offset;
                        rs.save();
                        return;
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
        client.post(App.getBaseURL() + "registration/update_contact_book", params, jrep);
    }

    public boolean checkPermission() {
        int currentAPIVersion = Build.VERSION.SDK_INT;
        if (currentAPIVersion >= android.os.Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                return false;
            } else {
                return true;
            }
        } else {
            return true;
        }
    }*/
}





