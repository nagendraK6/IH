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
import static com.relylabs.InstaHelo.Utils.Helper.removefragment;

public class SendInviteFragment extends Fragment implements SharingContactListAdapter.ItemClickListener  {

    private static final int REQUEST_FOR_READ_CONTACTS = 10;
    private ArrayList<String> contact_names, contact_numbers;
    private ArrayList<String> contact_names_permanent, contact_numbers_permanent;
    View fragment_view;
    RecyclerView recyclerView;
    SharingContactListAdapter adapter;
    ProgressBar show_busy_indicator;
    private FragmentActivity activity;
    EndlessRecyclerViewScrollListener scrollListener;
    String query_txt = "";
    int offset  = 0;
    Boolean has_ended = false;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
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
                removefragment(activity);
            }
        });

        show_busy_indicator.setVisibility(View.VISIBLE);
        fragment_view = view;
        if (checkPermission(activity)) {
            processContacts(true);
            //Helper.sendRequestForContactProcess(activity);
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
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.d("debug_c", "Query text is " + newText);
                has_ended = false;
                query_txt = newText;
                offset = 0;
                contact_names.clear();
                contact_numbers.clear();
                scrollListener.resetState();

               // if (query_txt.length() == 0) {
                processContacts(true);
               /* } else {
                    ArrayList<Contact> new_contacts = readContacts(10);
                    for (int i = 0; i < new_contacts.size(); i++) {
                        contact_names.add(new_contacts.get(i).Name);
                        contact_numbers.add(new_contacts.get(i).Phone);
                    }



                    adapter.notifyDataSetChanged();
                }*/



                return false;
            }
        });
    }


    public void processContacts(boolean read_from_memory) {
        prepareRecyclerView(25);
    }



    public ArrayList<Contact> readContacts(int max_limit){
        ArrayList<Contact> contacts = new ArrayList<>();
        Cursor cur = activity.getContentResolver().query(
                ContactsContract.Contacts.CONTENT_URI,    // Content Uri is specific to individual content providers.
                null,    // String[] describing which columns to return.
                null,     // Query arguments.
                null);

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
                    Cursor pCur = activity.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" = ?",
                            new String[]{id}, null);

                    String phone = "";
                    while (pCur.moveToNext()) {
                        phone = pCur.getString(
                                pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    }

                    pCur.close();
                    String refinedPhone = cleanPhoneNo(phone, activity);
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
            //Helper.sendRequestForContactProcess(activity);
        }
    }


    void prepareRecyclerView(int limit) {

        contact_names = new ArrayList<>();
        contact_numbers = new ArrayList<>();

        has_ended = false;
        ArrayList<Contact> all_data = readContacts(limit);
        for (int i = 0; i < all_data.size(); i++) {
            contact_names.add(all_data.get(i).Name);
            contact_numbers.add(all_data.get(i).Phone);
        }

        recyclerView = fragment_view.findViewById(R.id.contact_list_display);
        LinearLayoutManager layoutManager = new LinearLayoutManager(activity);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new SharingContactListAdapter(getContext(), contact_names, contact_numbers);
        adapter.setClickListener(this);
        recyclerView.setAdapter(adapter);
        show_busy_indicator.setVisibility(View.INVISIBLE);


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

    void loadNextDataFromApi(int page, int limit) {
        Log.d("debug_c", "Page called " + String.valueOf(page));
        int old_offset = contact_numbers.size();
        Log.d("debug_c", "Fetching at " + String.valueOf(offset));
        ArrayList<Contact> new_list = readContacts(limit);
        for (int i  = 0; i < new_list.size(); i++) {
            Log.d("debug_c", new_list.get(i).Name);
            contact_names.add(new_list.get(i).Name);
            contact_numbers.add(new_list.get(i).Phone);
        }

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
    public void onItemClick(int position) {

    }

}