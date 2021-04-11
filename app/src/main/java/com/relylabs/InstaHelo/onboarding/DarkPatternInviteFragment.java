package com.relylabs.InstaHelo.onboarding;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.os.Bundle;

import com.relylabs.InstaHelo.MainScreenFragment;
import com.relylabs.InstaHelo.R;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.os.Bundle;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.relylabs.InstaHelo.App;
import com.relylabs.InstaHelo.R;
import com.relylabs.InstaHelo.Utils.Helper;
import com.relylabs.InstaHelo.models.Contact;
import com.relylabs.InstaHelo.models.SystemProperties;
import com.relylabs.InstaHelo.models.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;

import com.relylabs.InstaHelo.Utils.Helper;
import com.relylabs.InstaHelo.sharing.EndlessRecyclerViewScrollListener;
import com.relylabs.InstaHelo.sharing.SharingContactListAdapter;

public class DarkPatternInviteFragment extends Fragment  implements DarkPatternInviteAdapter.ItemClickListener  {
    public FragmentActivity activity_ref;
    View fragment_view;

    private static final int REQUEST_FOR_READ_CONTACTS = 10;

    RecyclerView recyclerView;
    DarkPatternInviteAdapter adapter;
    ImageView selected_deselected_all;
    Boolean selected_all = Boolean.TRUE;
    String query_txt = "";
    int offset  = 0;
    Boolean has_ended = false;
    EndlessRecyclerViewScrollListener scrollListener;
    ArrayList<Contact> all_data;
    ArrayList<Contact> phone_no_for_selection_deselection;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        fragment_view =  inflater.inflate(R.layout.fragment_dark_pattern_invites, container, false);
        final User user = User.getLoggedInUser();
        user.UserSteps = "DARK_INVITE";
        user.save();
        return fragment_view;
    }
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Activity){
            activity_ref=(FragmentActivity) context;
        }
    }
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (checkPermission(activity_ref)) {
            prepareRecyclerView(25);
        }


        selected_deselected_all = view.findViewById(R.id.selected_deselected_all);
        selected_deselected_all.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selected_all) {
                    selected_all = Boolean.FALSE;
                    selected_deselected_all.setImageDrawable(activity_ref.getDrawable(R.drawable.tic_mark_not_selected));
                } else {
                    selected_all = Boolean.TRUE;
                    selected_deselected_all.setImageDrawable(activity_ref.getDrawable(R.drawable.tic_mark_selected));
                }

                prepareRecyclerView(25);
            }
        });
        TextView skip = view.findViewById(R.id.skip);
        skip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Helper.skipScreen(activity_ref);
            }
        });

        ImageView looks_good_to_me = view.findViewById(R.id.looks_good_to_me);
        looks_good_to_me.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                User user = User.getLoggedInUser();
                if (selected_all) {
                    user.SendInvitesToAllUsers = Boolean.TRUE;
                } else {
                    user.SendInvitesToAllUsers = Boolean.FALSE;
                }

                user.save();
                SystemProperties rs = SystemProperties.getSystemSettings();
                rs.last_contact_cursor_uploaded = 0;
                rs.save();
                Helper.sendRequestForContactProcess(activity_ref);
                if (phone_no_for_selection_deselection.size() == 0) {
                    Helper.skipScreen(activity_ref);
                } else {
                    send_contacts_preference_to_server();
                }
            }
        });
    }


    private void send_contacts_preference_to_server() {
        final User user = User.getLoggedInUser();
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        ArrayList<String> contact_names = new ArrayList<>();
        ArrayList<String> contact_numbers = new ArrayList<>();
        for (int i = 0;i < phone_no_for_selection_deselection.size(); i++) {
            contact_names.add(phone_no_for_selection_deselection.get(i).Name);
            contact_numbers.add(phone_no_for_selection_deselection.get(i).Phone);
        }

        JSONArray mJSONArray_names = new JSONArray(contact_names);
        JSONArray mJSONArray_numbers = new JSONArray(contact_numbers);

        params.add("contact_names", mJSONArray_names.toString());
        params.add("contact_numbers", mJSONArray_numbers.toString());
        params.add("should_send_invite", selected_all == Boolean.TRUE ? "0" : "1");

        JsonHttpResponseHandler jrep = new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                Helper.skipScreen(activity_ref);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String res, Throwable t) {
                Log.d("debug_data", "" + res);
                Helper.skipScreen(activity_ref);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject obj) {
                Helper.skipScreen(activity_ref);
            }
        };

        client.addHeader("Accept", "application/json");
        client.addHeader("Authorization", "Token " + user.AccessToken);
        client.post(App.getBaseURL() + "registration/send_contact_invite_preference", params, jrep);
    }

    void prepareRecyclerView(int limit) {
        offset = 0;
        phone_no_for_selection_deselection = new ArrayList<>();
        has_ended = false;
        all_data = readContacts(limit);

        recyclerView = fragment_view.findViewById(R.id.all_contacts_list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(activity_ref);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new DarkPatternInviteAdapter(activity_ref, all_data, selected_all);
        adapter.setClickListener(this);
        recyclerView.setAdapter(adapter);
        //show_busy_indicator.setVisibility(View.INVISIBLE);


        scrollListener = new EndlessRecyclerViewScrollListener(layoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                // Triggered only when new data needs to be appended to the list
                // Add whatever code is needed to append new items to the bottom of the list
                loadNextDataFromApi(page, limit);
            }
        };


        recyclerView.addOnScrollListener(scrollListener);
    }

    public boolean checkPermission(final Context context) {
        int currentAPIVersion = Build.VERSION.SDK_INT;
        if (currentAPIVersion >= android.os.Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, android.Manifest.permission.READ_CONTACTS)) {
                    requestPermissions(new String[]{android.Manifest.permission.READ_CONTACTS}, REQUEST_FOR_READ_CONTACTS);
                } else {
                    requestPermissions(new String[]{android.Manifest.permission.READ_CONTACTS}, REQUEST_FOR_READ_CONTACTS);
                }
                return false;
            } else {
                return true;
            }
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int RC, String per[], int[] PResult) {
        super.onRequestPermissionsResult(RC, per, PResult);
        if (PResult.length > 0 && PResult[0] == PackageManager.PERMISSION_GRANTED) {
            prepareRecyclerView(25);
        } else {
            Helper.skipScreen(activity_ref);
        }
    }

    public ArrayList<Contact> readContacts(int max_limit){
        ArrayList<Contact> contacts = new ArrayList<>();
        Cursor cur = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            cur = activity_ref.getContentResolver().query(
                    ContactsContract.Contacts.CONTENT_URI,    // Content Uri is specific to individual content providers.
                    null,    // String[] describing which columns to return.
                    null,     // Query arguments.
                    null);
        }

        if (cur == null) {
            return contacts;
        }

        int i = 0;
        if (cur.getCount() > 0) {
            cur.moveToPosition(offset);
            while (cur.moveToNext()) {
                offset = offset + 1;
                String id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
                String name = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                if (name != null &&  query_txt.length() > 0 && !name.toLowerCase().startsWith(query_txt.toLowerCase())) {
                    continue;
                }


                if (Integer.parseInt(cur.getString(cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
                    // get the phone number
                    Cursor pCur = activity_ref.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" = ?",
                            new String[]{id}, null);

                    String phone = "";
                    while (pCur.moveToNext()) {
                        phone = pCur.getString(
                                pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    }

                    pCur.close();
                    String refinedPhone = Helper.cleanPhoneNo(phone, activity_ref);
                    if (name  == null) {
                        Log.d("debug_c", "name is null. phone is " + phone);
                    }
                    contacts.add(new Contact(name, refinedPhone, false, false));
                    i++;
                    if (i >= max_limit) {
                        break;
                    }

                }
            }
        } else {
            has_ended = true;
        }

        return contacts;
    }

    void loadNextDataFromApi(int page, int limit) {
        Log.d("debug_c", "Page called " + String.valueOf(page));
        int old_offset = all_data.size();
        Log.d("debug_c", "Fetching at " + String.valueOf(offset));
        ArrayList<Contact> new_list = readContacts(limit);
        all_data.addAll(new_list);
        if (new_list.size() > 0) {
            //   adapter.notifyDataSetChanged();
            Log.d("debug_c", "adapter called");
            adapter.notifyItemRangeInserted(old_offset, new_list.size());
        }
    }



    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onItemClick(View view, int position, Boolean selected) {
        Log.d("debug_c", selected.toString());
        String phn = all_data.get(position).Phone;
        if (selected_all == Boolean.FALSE) {
            if (selected) {
                phone_no_for_selection_deselection.add(all_data.get(position));
            } else {
                for (int i = 0; i < phone_no_for_selection_deselection.size(); i++) {
                    if(phone_no_for_selection_deselection.get(i).Phone.equals(phn)) {
                        phone_no_for_selection_deselection.remove(i);
                    }
                }
            }
        } else {
            if (!selected) {
                phone_no_for_selection_deselection.add(all_data.get(position));
            } else {
                for (int i = 0; i < phone_no_for_selection_deselection.size(); i++) {
                    if(phone_no_for_selection_deselection.get(i).equals(phn)) {
                        phone_no_for_selection_deselection.remove(i);
                    }
                }
            }
        }
    }
}