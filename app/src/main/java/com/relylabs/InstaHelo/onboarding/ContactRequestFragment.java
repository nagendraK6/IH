package com.relylabs.InstaHelo.onboarding;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.os.Bundle;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.relylabs.InstaHelo.App;
import com.relylabs.InstaHelo.MainScreenFragment;
import com.relylabs.InstaHelo.R;
import com.relylabs.InstaHelo.Utils.Helper;
import com.relylabs.InstaHelo.models.Contact;
import com.relylabs.InstaHelo.models.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;

import static com.relylabs.InstaHelo.Utils.Helper.loadFragment;
import static com.relylabs.InstaHelo.Utils.Helper.nextScreen;
import static com.relylabs.InstaHelo.Utils.Helper.skipScreen;

public class ContactRequestFragment extends Fragment {
    public FragmentActivity activity_ref;
    public static int REQUEST_FOR_READ_CONTACTS = 9;
    ProgressBar busy_indicator;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final User user = User.getLoggedInUser();
        user.UserSteps = "CONTACT_REQUEST";
        user.save();
        return inflater.inflate(R.layout.fragment_contact_request, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TextView next_contact = view.findViewById(R.id.next_contact);
        busy_indicator = view.findViewById(R.id.busy_indicator);
        next_contact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(checkPermission(getContext())) {
                    Helper.sendRequestForContactProcess(activity_ref);
                    loadFragment(new SuggestedProfileToFollowFragment(),activity_ref);
                }
            }
        });
        TextView skip = view.findViewById(R.id.skip);
        skip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                skipScreen(activity_ref);
            }
        });

        User user = User.getLoggedInUser();
        user.UserSteps = "CONTACT_REQUEST";
        user.save();
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
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Activity){
            activity_ref=(FragmentActivity) context;
        }
    }
    @Override
    public void onRequestPermissionsResult(int RC, String per[], int[] PResult) {
        super.onRequestPermissionsResult(RC, per, PResult);
        Log.d("debug_data", "On permission result");
        if (PResult.length > 0 && PResult[0] == PackageManager.PERMISSION_GRANTED) {
            Helper.sendRequestForContactProcess(activity_ref);
            loadFragment(new SuggestedProfileToFollowFragment(),activity_ref);
        } else {
            Log.d("debug_data", "Permission denied");
            loadFragment(new SuggestedProfileToFollowFragment(),activity_ref);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}