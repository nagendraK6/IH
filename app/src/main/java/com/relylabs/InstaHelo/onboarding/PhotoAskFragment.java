package com.relylabs.InstaHelo.onboarding;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.icu.lang.UScript;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
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
import com.relylabs.InstaHelo.MainScreenFragment;
import com.relylabs.InstaHelo.R;
import com.relylabs.InstaHelo.models.User;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import cz.msebera.android.httpclient.Header;
import de.hdodenhof.circleimageview.CircleImageView;

public class PhotoAskFragment extends Fragment {

    String image_file_name = "";
    ProgressBar busy;

    int REQUEST_CAMERA = 1;
    int SELECT_FILE = 0;
    TextView next_photo;
    public static final int REQUEST_FOR_TAKE_PHOTO = 9;
    File imgfile ;
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private static Bundle savedImageState = null;
    String image_storage_path = "";
    Boolean image_updated_in_profile = false;
    String userChoosenTask = "";
    String user_profile_image_url = "";
    CircleImageView crl_image_view;
    ImageView empty;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_photo_ask, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        next_photo = view.findViewById(R.id.next_photo);
        busy = view.findViewById(R.id.busy_send_photo);
        empty = view.findViewById(R.id.form_fields);
        empty.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectImage();
            }
        });

         crl_image_view = view.findViewById(R.id.profile_image_view);
        crl_image_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectImage();
            }
        });

        if (getArguments() != null) {
            image_file_name =  getArguments()
                    .getString(getString(R.string.user_selected_image));

            final CircleImageView proflie_img = view.findViewById(R.id.profile_image_view);
            if (image_file_name != null) {
                Picasso.get().load(new File(image_file_name))
                        .into(proflie_img);
                proflie_img.setVisibility(View.VISIBLE);
                next_photo.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.next_enabled));
            }
        }

        next_photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendImageToServer();
            }
        });

        TextView skip = view.findViewById(R.id.skip);
        skip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadFragment(new AddBioDetailsFragment());
            }
        });
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
       // if (getContext() == null) {
       //     return;
       // }
        boolean result = checkPermission(getContext());
        verifyStoragePermissions(getActivity());
        Log.d("debug_data", "inside camera intent result");
        Log.d("debug_data", "result is " + String.valueOf(result));
        if (result) {
            if (resultCode == Activity.RESULT_OK) {
                if (requestCode == SELECT_FILE) {
                    try {
                        onSelectFromGalleryResult(data);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                else if (requestCode == REQUEST_CAMERA)
                    onCaptureImageResult(data);
            }
        }
    }

    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.READ_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    private void selectImage() {
        final CharSequence[] items = { "Choose from Library",
                "Cancel" };
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Add Photo!");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (getActivity() == null) {
                    return;
                }

                if (items[item].equals("Take Photo")) {
                    boolean result= checkPermissionCamera(getActivity());
                    userChoosenTask="Take Photo";
                    if(result)
                        cameraIntent();
                } else if (items[item].equals("Choose from Library")) {
                    boolean result= checkPermission(getActivity());
                    userChoosenTask="Choose from Library";
                    if(result)
                        galleryIntent();
                } else if (items[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    public boolean checkPermission(final Context context)
    {
        int currentAPIVersion = Build.VERSION.SDK_INT;
        if(currentAPIVersion>=android.os.Build.VERSION_CODES.M)
        {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},  REQUEST_FOR_TAKE_PHOTO);
                } else {
                    requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},  REQUEST_FOR_TAKE_PHOTO);
                }
                return false;
            } else {
                return true;
            }
        } else {
            return true;
        }
    }

    public boolean checkPermissionCamera(final Context context)
    {
        int currentAPIVersion = Build.VERSION.SDK_INT;
        if(currentAPIVersion>=android.os.Build.VERSION_CODES.M)
        {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    requestPermissions(new String[]{android.Manifest.permission.CAMERA},  REQUEST_FOR_TAKE_PHOTO);
                } else {
                    requestPermissions(new String[]{android.Manifest.permission.CAMERA},  REQUEST_FOR_TAKE_PHOTO);
                }
                return false;
            } else {
                return true;
            }
        } else {
            return true;
        }
    }

    private void loadFragment(Fragment fragment_to_start) {
        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragment_holder, fragment_to_start);
        ft.commit();
    }

    private void sendImageToServer() {
        if (image_storage_path.equals("")) {
            return;
        }

        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        try {
            File imgfile = new File(user_profile_image_url);
            params.put("image", imgfile);
            params.put("width", 300);
            params.put("height", 300);
        } catch (FileNotFoundException fexception) {
            fexception.printStackTrace();
        }

        User user = User.getLoggedInUser();
        busy.setVisibility(View.VISIBLE);


        JsonHttpResponseHandler jrep = new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                android.util.Log.d("debug_data", "uploaded the image on server...");

                // update user profile and broadcast the update
                try {
                    user.ProfilePicURL = response.getString("profile_image_url");
                    user.save();
                    loadFragment(new AddBioDetailsFragment());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String res, Throwable t) {
                HashMap logData = new HashMap<String, String>();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable t, JSONObject obj) {
                HashMap logData = new HashMap<String, String>();
            }
        };

        client.addHeader("Accept", "application/json");
        client.addHeader("Authorization", "Token " + user.AccessToken);
        client.post(App.getBaseURL() + "registration/profile_photo_update", params, jrep);
    }


    @Override
    public void onRequestPermissionsResult(int RC, String per[], int[] PResult) {
        super.onRequestPermissionsResult(RC, per, PResult);
        if (PResult.length > 0 && PResult[0] == PackageManager.PERMISSION_GRANTED) {
            if (userChoosenTask.equals("Take Photo")) {
                cameraIntent();
            } else if (userChoosenTask.equals("Choose from Library")) {
                galleryIntent();
            }
        } else {
            Toast.makeText(getActivity(),"Permission is needed to capture image for the profile", Toast.LENGTH_LONG).show();
        }
    }

    private void cameraIntent()
    {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        getActivity().startActivityForResult(intent, REQUEST_CAMERA);
    }

    private void galleryIntent()
    {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        getActivity().startActivityForResult(Intent.createChooser(intent, "Select File"),SELECT_FILE);
    }

    private void onSelectFromGalleryResult(Intent data) throws IOException {

        Bitmap bm = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), data.getData());
        int width = bm.getWidth();
        int height = bm.getHeight();
        int newWidth = 80;
        int newHeight = 80;

        // calculate the scale - in this case = 0.4f
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;

        // createa matrix for the manipulation
        Matrix matrix = new Matrix();
        // resize the bit map
        matrix.postScale(scaleWidth, scaleHeight);
        // rotate the Bitmap
        //matrix.postRotate(45);

        // recreate the new Bitmap
        Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0,
                width, height, matrix, true);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 90, bytes);
        //upload
        image_storage_path = new File(getContext().getExternalCacheDir(), System.currentTimeMillis() + ".jpeg").getAbsolutePath();
        user_profile_image_url= image_storage_path;
        File destination = new File(image_storage_path);
        FileOutputStream fo;
        destination.createNewFile();
        fo = new FileOutputStream(destination);
        fo.write(bytes.toByteArray());
        user_profile_image_url = destination.getAbsolutePath();
        fo.close();
        File imgFile = new  File(destination.getAbsolutePath());
        image_updated_in_profile = true;

        Log.d("debug_data", data.getData().toString());
          Picasso.get().load(data.getData())
                .into(crl_image_view);
        imgfile = new File(data.getData().getPath()) ;
        image_storage_path = data.getData().toString();
        crl_image_view.setVisibility(View.VISIBLE);
        empty.setVisibility(View.INVISIBLE);
        next_photo.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.next_enabled));
    }
    private void onCaptureImageResult(Intent data) {
        Bitmap thumbnail = (Bitmap) data.getExtras().get("data");
        Picasso.get().load(getImageUri(getContext(), thumbnail))
                .into(crl_image_view);
        crl_image_view.setVisibility(View.VISIBLE);
        empty.setVisibility(View.INVISIBLE);
    }

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }
}