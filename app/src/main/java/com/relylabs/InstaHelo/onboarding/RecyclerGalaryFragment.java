package com.relylabs.InstaHelo.onboarding;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.relylabs.InstaHelo.App;
import com.relylabs.InstaHelo.R;
import com.relylabs.InstaHelo.Utils.Helper;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by nagendra on 8/21/18.
 */

public class RecyclerGalaryFragment extends Fragment  implements MyRecyclerViewAdapter.ItemClickListener {
    public static final int REQUEST_FOR_TAKE_PHOTO = 9;
    public FragmentActivity activity_ref;
    RecyclerView recyclerView;
    MyRecyclerViewAdapter adapter;
    String mSelectedImage;
    ArrayList<String> all_images;
    ImageView preview_image;
    View fragment_view;
    String ref = "";
    TextView tvNext;
    private Spinner directorySpinner;
    ArrayList<String> directoryNames = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        fragment_view = inflater.inflate(R.layout.recycler_galary_fragment, container, false);
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
        ImageView closeButton = view.findViewById(R.id.ivCloseShare);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activity_ref.onBackPressed();
            }
        });

        directorySpinner = view.findViewById(R.id.spinnerDirectory);

        processImageSelection();

        if (getArguments() != null) {
            ref =  getArguments()
                    .getString("ref");
        }

        tvNext = fragment_view.findViewById(R.id.tvNext);
    }

    private void loadFragment(Fragment fragment_to_start) {
        FragmentTransaction ft = activity_ref.getSupportFragmentManager().beginTransaction();
        ft.add(R.id.fragment_holder, fragment_to_start);
        ft.addToBackStack(null);
        ft.commitAllowingStateLoss();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //App.getRefWatcher(activity_ref).watch(this);
        if (recyclerView != null) {
            recyclerView.setAdapter(null);
        }
    }

    private void setAlbumNames() {
        directoryNames = Helper.getDirectoryNames(getContext());
        ArrayAdapter<String> dr_adapter = new ArrayAdapter<String>(activity_ref,
                android.R.layout.simple_spinner_item, directoryNames);
        dr_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        directorySpinner.setAdapter(dr_adapter);

        directorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                galleryIntent(directoryNames.get(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    @Override
    public void onItemClick(View view, int position) {
        mSelectedImage = all_images.get(position);
        Bundle data_bundle = new Bundle();
        data_bundle.putString(getString(R.string.user_selected_image), mSelectedImage);
        Fragment frg = new PhotoAskFragment();
        frg.setArguments(data_bundle);
        loadFragment(frg);
    }


    public void processImageSelection() {
        if (activity_ref != null) {
            findPermissionsAndSelectImage();
        }
    }

    public void findPermissionsAndSelectImage() {
        boolean result = checkPermission(activity_ref);
        if (result) {
            setAlbumNames();
        }
    }


    public boolean checkPermission(final Context context) {
        int currentAPIVersion = Build.VERSION.SDK_INT;
        if (currentAPIVersion >= android.os.Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_FOR_TAKE_PHOTO);
                } else {
                    requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_FOR_TAKE_PHOTO);
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
            setAlbumNames();

        } else {
            loadFragment(new AddBioDetailsFragment());
        }
    }

    private void galleryIntent(String directoName) {
        all_images = Helper.getAllShownImagesPath(directoName, getContext());

        // set up the RecyclerView
        recyclerView = fragment_view.findViewById(R.id.all_images);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        adapter = new MyRecyclerViewAdapter(getContext(), all_images);
        adapter.setClickListener(this);
        recyclerView.setAdapter(adapter);
        tvNext = fragment_view.findViewById(R.id.tvNext);
        tvNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bundle data_bundle = new Bundle();
                data_bundle.putString(getString(R.string.user_selected_image), mSelectedImage);
                Fragment frg = new PhotoAskFragment();
                frg.setArguments(data_bundle);
                loadFragment(frg);
            }
        });

        mSelectedImage = all_images.get(0);
        tvNext.setVisibility(View.VISIBLE);
    }
}