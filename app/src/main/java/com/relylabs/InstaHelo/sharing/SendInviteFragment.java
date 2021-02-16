package com.relylabs.InstaHelo.sharing;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.os.Bundle;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.relylabs.InstaHelo.App;
import com.relylabs.InstaHelo.R;
import com.relylabs.InstaHelo.models.Contact;
import com.relylabs.InstaHelo.models.User;
import com.relylabs.InstaHelo.onboarding.FriendToFollowListAdapter;
import com.relylabs.InstaHelo.onboarding.FriendsToFollow;
import com.relylabs.InstaHelo.onboarding.SuggestedProfileToFollowFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;

public class SendInviteFragment extends Fragment implements SharingContactListAdapter.ItemClickListener  {

    private static final int REQUEST_FOR_READ_CONTACTS = 10;
    private ArrayList<String> contact_names, contact_numbers;
    private ArrayList<String> contact_numbers_exclude;
    View fragment_view;
    RecyclerView recyclerView;
    SharingContactListAdapter adapter;
    ProgressBar show_busy_indicator;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_contact_list_display, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ImageView left_move = view.findViewById(R.id.move_back);
        show_busy_indicator = view.findViewById(R.id.show_busy_indicator);
        left_move.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removefragment();
            }
        });

        show_busy_indicator.setVisibility(View.VISIBLE);
        fragment_view = view;
        if (checkPermission(getContext())) {
            new StartAsyncTask().execute();
        }
    }


    public class StartAsyncTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            ArrayList<Contact> all_contacts = readContacts();
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    prepareRecyclerView(all_contacts);
                    // WORK on UI thread here
                }
            });
            return null;
        }
    }


    public ArrayList<Contact> readContacts(){
        contact_names = new ArrayList<>();
        contact_numbers = new ArrayList<>();
        ArrayList<Contact> contacts = new ArrayList<>();
        ContentResolver cr = getActivity().getContentResolver();
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
                    if (phone.length() >= 10) {
                        phone = phone.replaceAll("[()\\s-]", "");
                        contacts.add(new Contact(name, phone));
                        contact_names.add(name);
                        contact_numbers.add(phone);
                    }
                }
            }
        }

        return contacts;
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
            new StartAsyncTask().execute();
        }
    }


    void prepareRecyclerView(ArrayList<Contact> all_contacts) {
        recyclerView = fragment_view.findViewById(R.id.contact_list_display);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 1));
        adapter = new SharingContactListAdapter(getContext(), contact_names, contact_numbers);
        adapter.setClickListener(this);
        recyclerView.setAdapter(adapter);
        upload_to_server_contacts(contact_names, contact_numbers);
        show_busy_indicator.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    private void loadFragment(Fragment fragment_to_start) {
        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragment_holder, fragment_to_start);
        ft.commit();
    }

        private void removefragment() {
            Fragment f = getActivity().getSupportFragmentManager().findFragmentById(R.id.fragment_holder);
            FragmentManager manager = getActivity().getSupportFragmentManager();
            FragmentTransaction trans = manager.beginTransaction();
            trans.remove(f);
            trans.commit();
            manager.popBackStack();
        }

    @Override
    public void onItemClick(int position) {

    }

        private void upload_to_server_contacts(ArrayList<String> contact_name, ArrayList<String> contact_number) {
            final User user = User.getLoggedInUser();
            AsyncHttpClient client = new AsyncHttpClient();
            RequestParams params = new RequestParams();

            JSONArray mJSONArray_names = new JSONArray(contact_name);
            JSONArray mJSONArray_numbers = new JSONArray(contact_number);

            params.add("contact_names", mJSONArray_names.toString());
            params.add("contact_numbers", mJSONArray_numbers.toString());

            JsonHttpResponseHandler jrep = new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    try {
                        String error_message = response.getString("error_message");
                        JSONArray all_contact_exclude = response.getJSONArray("all_contact_exclude");
                        if (all_contact_exclude.length() > 0) {
                            ArrayList<String> exclude_lst = new ArrayList<>();
                            for (int i =0 ;i < all_contact_exclude.length(); i++) {
                                JSONObject contact_info = all_contact_exclude.getJSONObject(i);
                                exclude_lst.add(contact_info.getString("contact_number"));
                            }

                            // remove the contact and reload the adapter
                            ArrayList<String> new_contact_names = new ArrayList<>();
                            ArrayList<String> new_contact_numbers = new ArrayList<>();
                            for (int i = 0; i < contact_number.size(); i++) {
                                if (!exclude_lst.contains(contact_numbers.get(i))) {
                                    new_contact_names.add(contact_names.get(i));
                                    new_contact_numbers.add(contact_number.get(i));
                                }
                            }


                            contact_numbers.clear();
                            contact_names.clear();
                            contact_name.addAll(new_contact_names);
                            contact_numbers.addAll(new_contact_numbers);
                            adapter.notifyDataSetChanged();
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
            client.post(App.getBaseURL() + "registration/get_contact_status", params, jrep);
        }
}