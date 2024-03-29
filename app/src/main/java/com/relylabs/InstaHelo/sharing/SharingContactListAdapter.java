package com.relylabs.InstaHelo.sharing;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.relylabs.InstaHelo.App;
import com.relylabs.InstaHelo.MainScreenFragment;
import com.relylabs.InstaHelo.R;
import com.relylabs.InstaHelo.Utils.Helper;
import com.relylabs.InstaHelo.Utils.Logger;
import com.relylabs.InstaHelo.models.Contact;
import com.relylabs.InstaHelo.models.User;
import com.relylabs.InstaHelo.models.UserSettings;
import com.relylabs.InstaHelo.onboarding.SuggestedProfileToFollowFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.WeakHashMap;

import cz.msebera.android.httpclient.Header;

import static com.relylabs.InstaHelo.Utils.Helper.nextScreen;

public class SharingContactListAdapter extends RecyclerView.Adapter<SharingContactListAdapter.ViewHolder> {

    private ArrayList<String> contact_names, contact_numbers;
    private LayoutInflater mInflater;
    private SharingContactListAdapter.ItemClickListener mClickListener;
    private Context context;
    SharingContactListAdapter(Context context, ArrayList<String> contact_names, ArrayList<String> contact_numbers) {
        this.mInflater = LayoutInflater.from(context);
        this.contact_numbers = contact_numbers;
        this.contact_names = contact_names;
        this.context = context;
    }

    // inflates the cell layout from xml when needed
    @Override
    public SharingContactListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.contact_list_invite_item, parent, false);
        return new SharingContactListAdapter.ViewHolder(view);
    }

    // binds the data to the textview in each cell
    @Override
    public void onBindViewHolder(final SharingContactListAdapter.ViewHolder holder, final int position) {
        holder.name.setText(this.contact_names.get(position));

        holder.name.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mClickListener != null) mClickListener.onItemClick(position);

            }
        });

        holder.v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mClickListener != null) mClickListener.onItemClick(position);
                Log.d("name",contact_names.get(position));
                Log.d("num",contact_numbers.get(position));
                PackageManager packageManager = context.getPackageManager();
                Intent i = new Intent(Intent.ACTION_VIEW);
                String phone = contact_numbers.get(position).toString();
                Boolean isInstalledWhatsapp = isAppInstalled(context, "com.whatsapp");
                sendInvite(phone);
                String message = "Hey " + contact_names.get(position) + " - I have an invite to InstaHelo and want you to join. I added you using " + phone +  ", so make sure to use that number when you register. Here is the link!  https://play.google.com/store/apps/details?id=com.relylabs.InstaHelo";
                if(isInstalledWhatsapp){
                    try {
                        String url = "https://api.whatsapp.com/send?phone="+ phone +"&text=" + URLEncoder.encode(message, "UTF-8");
                        i.setPackage("com.whatsapp");
                        i.setData(Uri.parse(url));
                        if (i.resolveActivity(packageManager) != null) {
                            context.startActivity(i);
                        }
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }
                else{
                    Intent smsIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("sms:"));
                    if(android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
                        smsIntent.setType("vnd.android-dir/mms-sms");

                    smsIntent.putExtra("address", phone);
                    smsIntent.putExtra("sms_body",message);
                    context.startActivity(smsIntent);
                }
            }
        });
    }

    public static Boolean isAppInstalled(Context activity, String package_name){
        PackageManager pm = activity.getPackageManager();
        boolean app_installed;
        try {
            pm.getPackageInfo(package_name, PackageManager.GET_ACTIVITIES);
            app_installed = true;
        }
        catch (PackageManager.NameNotFoundException e) {
            app_installed = false;
        }
        return app_installed;
    }

    public void sendInvite(String username){
        RequestParams params = new RequestParams();
        params.add("username",username);
        final User user = User.getLoggedInUser();
        if (user.InvitesCount <= 0) {
            return;
        }
        AsyncHttpClient client = new AsyncHttpClient();
        JsonHttpResponseHandler jrep = new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    String error_message = response.getString("error_message");
                    Log.d("sharing",response.toString());
                    int invite_count = response.getInt("invites_count");
                    int invited_users_count = response.getInt("invited_users_count");
                    int min_invites_count = response.getInt("min_invites_count");
                    user.InvitesCount = invite_count;
                    user.save();


                    UserSettings us  = UserSettings.getSettings();
                    if (!us.has_seen_create_room_info && invited_users_count > min_invites_count) {
                        us.has_seen_create_room_info = true;
                        us.save();

                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setTitle("Congrats ");
                        builder.setMessage("You can now create the room and your followers can join the discussion.");
                        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });

                        builder.show();
                    }

                    Contact c = Contact.getContact(username);
                    if (c != null) {
                        c.IsInvited = Boolean.TRUE;
                        c.save();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }


            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable t, JSONObject obj) {
            }
        };

        client.addHeader("Accept", "application/json");
        client.addHeader("Authorization", "Token " + user.AccessToken);
        client.post(App.getBaseURL() + "registration/send_invite", params, jrep);
    }


    // total number of cells
    @Override
    public int getItemCount() {
        return contact_names.size();
    }


    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView name;
        ImageView contact_selected;
        Boolean is_selected;
        View v;

        ViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.contact_name);
            itemView.setOnClickListener(this);
            v = itemView;
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(getAdapterPosition());
        }
    }


    // allows clicks events to be caught
    public void setClickListener(SharingContactListAdapter.ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(int position);
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
    }
}
