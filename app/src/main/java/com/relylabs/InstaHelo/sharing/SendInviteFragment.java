package com.relylabs.InstaHelo.sharing;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;
import android.os.Bundle;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.relylabs.InstaHelo.App;
import com.relylabs.InstaHelo.R;
import com.relylabs.InstaHelo.Utils.Helper;
import com.relylabs.InstaHelo.models.Contact;
import com.relylabs.InstaHelo.models.User;
import com.relylabs.InstaHelo.onboarding.FriendToFollowListAdapter;
import com.relylabs.InstaHelo.onboarding.FriendsToFollow;
import com.relylabs.InstaHelo.onboarding.SuggestedProfileToFollowFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;

import static androidx.core.content.ContextCompat.getSystemService;
import static com.relylabs.InstaHelo.Utils.Helper.cleanPhoneNo;

public class SendInviteFragment extends Fragment implements SharingContactListAdapter.ItemClickListener  {

    private static final int REQUEST_FOR_READ_CONTACTS = 10;
    private ArrayList<String> contact_names, contact_numbers;
    private ArrayList<String> contact_names_permanent, contact_numbers_permanent;
    private ArrayList<String> contact_numbers_exclude;
    View fragment_view;
    RecyclerView recyclerView;
    SharingContactListAdapter adapter;
    ProgressBar show_busy_indicator;
    ArrayList<Contact> all_contacts;
    private FragmentActivity activity;
    InifiniteListView scrollListener;
    boolean read_from_memory = true;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        all_contacts = new ArrayList<>();
        return inflater.inflate(R.layout.fragment_contact_list_display, container, false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Activity) {
            activity = (FragmentActivity) context;
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final User user = User.getLoggedInUser();
        String invite_number;
        if (user.InvitesCount <= 0){
            invite_number = "YOU DO NOT HAVE ANY INVITES LEFT";
        }
        else{
            invite_number = "YOU HAVE " + user.InvitesCount.toString() + " INVITES";
        }
        TextView invite_top = (TextView) view.findViewById(R.id.main_desc_with_invite_count);
        invite_top.setText(invite_number);
        final ImageView left_move = view.findViewById(R.id.move_back);
        left_move.post( new Runnable() {
            // Post in the parent's message queue to make sure the parent
            // lays out its children before we call getHitRect()
            public void run() {
                final Rect r = new Rect();
                left_move.getHitRect(r);
                r.top += 24;
                r.bottom += 24;
                r.left += 24;
                r.right += 24;
                left_move.setTouchDelegate( new TouchDelegate( r , left_move));
            }
        });


        show_busy_indicator = view.findViewById(R.id.show_busy_indicator);


        left_move.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("debug_f", "Remove started");
                removefragment();
            }
        });

        show_busy_indicator.setVisibility(View.VISIBLE);
        fragment_view = view;
        if (checkPermission(activity)) {
            processContacts(false);
            Helper.sendRequestForContactProcess(activity);
        }

        SearchView search = (SearchView) view.findViewById(R.id.search_contact);
        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                search.setIconified(false);
            }
        });
        search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {

                Log.d("search",query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                //    adapter.getFilter().filter(newText);
                Log.d("search",newText);
                if(newText.length()==0){
                    contact_names.clear();
                    contact_numbers.clear();
                    contact_names.addAll(contact_names_permanent);
                    contact_numbers.addAll((contact_numbers_permanent));
                } else {
                    contact_names.clear();
                    contact_numbers.clear();
                    for (int i=0;i<contact_names_permanent.size();i++){

                        if(contact_names_permanent.get(i).toLowerCase().startsWith(newText.toLowerCase())){
                            contact_names.add(contact_names_permanent.get(i));
                            contact_numbers.add(contact_numbers_permanent.get(i));
                        }
                    }

                    adapter.notifyDataSetChanged();
                }

                return false;
            }
        });


    }


    public void processContacts(boolean read_from_memory) {
        // read contacts from db
        Log.d("debug_c", "Processing contacts");
        if (read_from_memory) {
            all_contacts = readContacts(25);
            Log.d("debug_c", "Processing contacts. Read memory done");
        } else {
            all_contacts = Contact.getTopContactsNotInvited(1000);
            Log.d("debug_c", "Processing contacts. Read disk done");
            if (all_contacts.size() == 0) {
                all_contacts = readContacts(25);
            }
        }

        Log.d("debug_c", "Processing contacts. Rendering");

        prepareRecyclerView(all_contacts);
    }



    public ArrayList<Contact> readContacts(int max_limit){
        ArrayList<Contact> contacts = new ArrayList<>();
        ContentResolver cr = activity.getContentResolver();
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null);


        int i = 0;
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
                    String refinedPhone = cleanPhoneNo(phone, activity);
                    if(!refinedPhone.equals("ERROR")){
                        contacts.add(new Contact(name, refinedPhone, false, false));
                        i++;
                        if (i >= max_limit) {
                            break;
                        }
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
            processContacts(true);
            Helper.sendRequestForContactProcess(activity);
        }
    }


    void prepareRecyclerView(ArrayList<Contact> all_contacts) {
        contact_names = new ArrayList<>();
        contact_numbers = new ArrayList<>();
        contact_names_permanent = new ArrayList<>();
        contact_numbers_permanent = new ArrayList<>();

        for (int i = 0; i < all_contacts.size(); i++) {
            contact_names_permanent.add(all_contacts.get(i).Name);
            contact_numbers_permanent.add(all_contacts.get(i).Phone);
        }

        contact_names.addAll(contact_names_permanent);
        contact_numbers.addAll(contact_numbers_permanent);


        recyclerView = fragment_view.findViewById(R.id.contact_list_display);
        LinearLayoutManager layoutManager = new LinearLayoutManager(activity);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new SharingContactListAdapter(getContext(), contact_names, contact_numbers);
        adapter.setClickListener(this);
        recyclerView.setAdapter(adapter);
        show_busy_indicator.setVisibility(View.INVISIBLE);

        // fetch_contact_list_from_the_server();
    }


    public class StartAsyncTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            Log.d("debug_c", "Processing contacts");
            if (read_from_memory) {
                all_contacts = readContacts(25);
                Log.d("debug_c", "Processing contacts. Read memory done");
            } else {
                all_contacts = Contact.getTopContactsNotInvited(1000);
                Log.d("debug_c", "Processing contacts. Read disk done");
                if (all_contacts.size() == 0) {
                    all_contacts = readContacts(25);
                }
            }

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    prepareRecyclerView(all_contacts);
                    Helper.sendRequestForContactProcess(activity);
                }
            });




            return null;
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    private void removefragment() {
        Log.d("debug_f", "Remove s");
        Fragment f = activity.getSupportFragmentManager().findFragmentById(R.id.fragment_holder);
        FragmentManager manager = activity.getSupportFragmentManager();
        FragmentTransaction trans = manager.beginTransaction();
        trans.remove(f);
        trans.commit();
        Log.d("debug_f", "Remove e");
        manager.popBackStack();
    }

    @Override
    public void onItemClick(int position) {

    }

    private void fetch_contact_list_from_the_server() {
        final User user = User.getLoggedInUser();
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();

        JsonHttpResponseHandler jrep = new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    String error_message = response.getString("error_message");
                    JSONArray all_contacts = response.getJSONArray("all_contacts");
                    Integer contacts_count_on_server = response.getInt("contacts_count_on_server");

                    if (contacts_count_on_server == 0) {
                        Log.d("debug_data", "No contact uploaded on server");
                        return;
                    }

                    contact_names_permanent.clear();
                    contact_numbers.clear();

                    contact_names.clear();
                    contact_numbers.clear();


                    for (int i = 0; i < all_contacts.length(); i++) {
                        JSONObject obj = all_contacts.getJSONObject(i);
                        String name = obj.getString("name");
                        String phone = obj.getString("number");



                        contact_names.add(name);
                        contact_numbers.add(phone);
                        contact_names_permanent.add(name);
                        contact_numbers_permanent.add(phone);
                    }

                    adapter.notifyDataSetChanged();
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
        client.post(App.getBaseURL() + "registration/get_non_invited_users_from_the_contact", params, jrep);
    }
}