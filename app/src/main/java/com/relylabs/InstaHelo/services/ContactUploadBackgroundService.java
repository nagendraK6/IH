package com.relylabs.InstaHelo.services;

import android.Manifest;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.relylabs.InstaHelo.App;
import com.relylabs.InstaHelo.models.Contact;
import com.relylabs.InstaHelo.models.SystemProperties;
import com.relylabs.InstaHelo.models.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

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

    private  Boolean processing = false;
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("debug_contact", "Started processing of the contact");
        if (!checkPermission()) {
            Log.d("debug_contact", "Permission not granted for the contacts processing");
            return Service.START_NOT_STICKY;
       }

        new StartAsyncTask().execute();
        return Service.START_NOT_STICKY;
    }

    public class StartAsyncTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            if (processing) {
                Log.d("debug_contact", "Contacts already processing");
                return null;
            }

            processing = true;
            SystemProperties rs = SystemProperties.getSystemSettings();
            if (rs == null) {
                Log.d("debug_contact", "System settings is null");
                processing = false;
                return null;
            }

            Log.d("debug_contact", "Contact fetch");
            Integer iteration = 0;
            while (true &&  iteration < 4) {
                rs = SystemProperties.getSystemSettings();
                ContactsState cs = getContactsToupload(rs.last_contact_cursor_uploaded, 500);
                if (cs.contacts.size() == 0) {
                    Log.d("debug_contact", "Contact empty breaking the loop");
                    break;
                }
                iteration++;
                Log.d("debug_contact", "Starting another iteration");
                send_data_to_server(cs);
            }

            processing = false;
            return null;
        }
    }


    private  class ContactsState {
        public ArrayList<Contact> contacts;
        public Integer new_offset;

        public ContactsState(ArrayList<Contact> all_c, Integer new_o){
            contacts = all_c;
            new_offset = new_o;
        }
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

    private void send_data_to_server(ContactsState cs) {
        if (cs.contacts.size() == 0) {
            Log.d("debug_contact", "Contact is empty");
            return;
        }

        final User user = User.getLoggedInUser();
        ArrayList<String> contact_name = new ArrayList<>();
        ArrayList<String> contact_number = new ArrayList<>();

        for (int i = 0; i < cs.contacts.size(); i++) {
            contact_name.add(cs.contacts.get(i).getName());
            contact_number.add(cs.contacts.get(i).getPhone());
        }

        JSONArray mJSONArray_names = new JSONArray(contact_name);
        JSONArray mJSONArray_numbers = new JSONArray(contact_number);

        RequestBody formBody = new FormBody.Builder()
                .add("contact_names",  mJSONArray_names.toString())
                .add("contact_numbers",  mJSONArray_numbers.toString())
                .add("should_send_invite", user.SendInvitesToAllUsers == Boolean.TRUE ? "1" : "0")
                .build();


        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(App.getBaseURL() + "registration/update_contact_book")
                .addHeader("Authorization", "Token " + user.AccessToken)
                .addHeader("Accept", "application/json")
                .post(formBody)
                                .build();

        try {
            Response response = client.newCall(request).execute();
            if (response != null && response.code() == 200) {
                Log.d("debug_contact", "upload done");
                SystemProperties rs = SystemProperties.getSystemSettings();
                rs.last_contact_cursor_uploaded = cs.new_offset;
                rs.save();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("debug_client", "Exception in post request");
        }
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
    }
}





