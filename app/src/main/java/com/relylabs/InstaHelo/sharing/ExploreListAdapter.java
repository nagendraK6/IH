package com.relylabs.InstaHelo.sharing;

import android.Manifest;
import android.content.Context;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.shape.CornerFamily;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.relylabs.InstaHelo.App;
import com.relylabs.InstaHelo.MainScreenFragment;
import com.relylabs.InstaHelo.OtherProfile;
import com.relylabs.InstaHelo.R;
import com.relylabs.InstaHelo.Utils.Logger;
import com.relylabs.InstaHelo.models.Contact;
import com.relylabs.InstaHelo.models.User;
import com.relylabs.InstaHelo.models.UserWithImage;
import com.relylabs.InstaHelo.onboarding.SuggestedProfileToFollowFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.WeakHashMap;

import cz.msebera.android.httpclient.Header;
import de.hdodenhof.circleimageview.CircleImageView;

import static com.relylabs.InstaHelo.Utils.Helper.nextScreen;

public class ExploreListAdapter extends RecyclerView.Adapter<ExploreListAdapter.ViewHolder> {

    private ArrayList<UserWithImage> users_to_show;
    private LayoutInflater mInflater;
    private ExploreListAdapter.ItemClickListener mClickListener;
    private Context context;

    ExploreListAdapter(Context context, ArrayList<UserWithImage> all_available_users) {
        this.mInflater = LayoutInflater.from(context);
        this.users_to_show = all_available_users;
        this.context = context;
    }

    // inflates the cell layout from xml when needed
    @Override
    public ExploreListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.explore_user_item, parent, false);
        return new ExploreListAdapter.ViewHolder(view);
    }

    // binds the data to the textview in each cell
    @Override
    public void onBindViewHolder(final ExploreListAdapter.ViewHolder holder, final int position) {
        holder.name.setText(this.users_to_show.get(position).FirstName);
        holder.bio.setText(this.users_to_show.get(position).bio);
        holder.display_user_name.setText(this.users_to_show.get(position).display_user_name);
        if (this.users_to_show.get(position).hasFollowed) {
            holder.user_action.setBackground(holder.itemView.getContext().getDrawable(R.drawable.following_state));
        } else {
            holder.user_action.setBackground(holder.itemView.getContext().getDrawable(R.drawable.follow_cta_action));
        }
        String image_url = this.users_to_show.get(position).profileImageURL;
        if (image_url!= null &&  !image_url.equals("")) {
            Glide.with(holder.itemView.getContext()).load(image_url).diskCacheStrategy(DiskCacheStrategy.ALL).into(holder.contact_image);
        } else {
            holder.contact_image.setImageDrawable(holder.itemView.getContext().getDrawable(R.drawable.empty_user_profile_image));
        }

        holder.v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mClickListener != null) mClickListener.onItemClick(position);
            }
        });

    }

    @Override
    public int getItemCount() {
        return users_to_show.size();
    }


    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView name, bio, display_user_name, user_action;
        ShapeableImageView contact_image;
        View v;

        ViewHolder(View itemView) {
            super(itemView);
            contact_image = itemView.findViewById(R.id.profile_img_noti);
            bio = itemView.findViewById(R.id.description);
            display_user_name = itemView.findViewById(R.id.username_follow);
            user_action = itemView.findViewById(R.id.user_action);
            user_action.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (users_to_show.get(getAdapterPosition()).hasFollowed) {
                        users_to_show.get(getAdapterPosition()).hasFollowed = Boolean.FALSE;
                    } else {
                        users_to_show.get(getAdapterPosition()).hasFollowed = Boolean.TRUE;
                    }

                    if (users_to_show.get(getAdapterPosition()).hasFollowed) {
                        user_action.setBackground(itemView.getContext().getDrawable(R.drawable.following_state));
                    } else {
                        user_action.setBackground(itemView.getContext().getDrawable(R.drawable.follow_cta_action));
                    }

                    send_server_request_follow_un_follow(users_to_show.get(getAdapterPosition()).UserId, users_to_show.get(getAdapterPosition()).hasFollowed);
                }
            });
            float radius = itemView.getResources().getDimension(R.dimen.default_corner_radius_profile_follow);
            contact_image.setShapeAppearanceModel(contact_image.getShapeAppearanceModel()
                    .toBuilder()
                    .setTopRightCorner(CornerFamily.ROUNDED,radius)
                    .setTopLeftCorner(CornerFamily.ROUNDED,radius)
                    .setBottomLeftCorner(CornerFamily.ROUNDED,radius)
                    .setBottomRightCorner(CornerFamily.ROUNDED,radius)
                    .build());

            name = itemView.findViewById(R.id.name);
            itemView.setOnClickListener(this);
            v = itemView;
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(getAdapterPosition());
        }

        private void send_server_request_follow_un_follow(Integer uid, Boolean has_followed) {
            final User user = User.getLoggedInUser();
            AsyncHttpClient client = new AsyncHttpClient();
            boolean running = false;
            String path = has_followed ? "registration/follow_user" : "registration/unfollow_user";
            RequestParams params = new RequestParams();
            params.add("uid", String.valueOf(uid));
            JsonHttpResponseHandler jrep = new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                }



                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable t, JSONObject obj) {
                }
            };

            client.addHeader("Accept", "application/json");
            client.addHeader("Authorization", "Token " + user.AccessToken);
            client.post(App.getBaseURL() + path, params, jrep);
        }
    }


    // allows clicks events to be caught
    public void setClickListener(ExploreListAdapter.ItemClickListener itemClickListener) {
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
