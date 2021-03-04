package com.relylabs.InstaHelo.followerList;

import android.content.Context;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.shape.CornerFamily;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.relylabs.InstaHelo.App;
import com.relylabs.InstaHelo.OtherProfile;
import com.relylabs.InstaHelo.R;
import com.relylabs.InstaHelo.Utils.Logger;
import com.relylabs.InstaHelo.models.User;
import com.squareup.picasso.Picasso;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.WeakHashMap;

import cz.msebera.android.httpclient.Header;


public class FollowingListAdapter extends RecyclerView.Adapter<FollowingListAdapter.ViewHolder> {

    private ArrayList<String> names, usernames,bio,img,user_ids;
    private LayoutInflater mInflater;
    private FollowingListAdapter.ItemClickListener mClickListener;
    private Context context;
    private ArrayList<String> currStatus;
    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS =0 ;
    // data is passed into the constructor
    FollowingListAdapter(Context context, ArrayList<String> names, ArrayList<String> usernames,ArrayList<String> bio,ArrayList<String> img,ArrayList<String> currentStatus,ArrayList<String> user_ids) {
        this.mInflater = LayoutInflater.from(context);
        this.names = names;
        this.usernames = usernames;
        this.bio = bio;
        this.img = img;
        this.context = context;
        this.currStatus = currentStatus;
        this.user_ids = user_ids;
    }

    @Override
    public FollowingListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.following_list_item, parent, false);
        return new FollowingListAdapter.ViewHolder(view);
    }

    // binds the data to the textview in each cell
    @Override
    public void onBindViewHolder(final FollowingListAdapter.ViewHolder holder, final int position) {
        holder.name.setText(this.names.get(position));
        holder.username.setText("("+this.usernames.get(position)+")");
        String currBio = this.bio.get(position);
        if(currBio.length()>50){
            currBio = currBio.substring(0,50);
            currBio = currBio + "...";
        }
        holder.description.setText(currBio);
        holder.description.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                holder.description.setText(bio.get(position));
            }
        });
        holder.name.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mClickListener != null) mClickListener.onItemClick(position);
            }
        });
        if (this.currStatus.get(position).equals("Follow")) {
            holder.follow.setBackground(holder.itemView.getContext().getDrawable(R.drawable.follow_cta_action));
        } else {
            holder.follow.setBackground(holder.itemView.getContext().getDrawable(R.drawable.following_state));
        }

        holder.prof.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OtherProfile otherprof = new OtherProfile();
                Bundle args = new Bundle();
                args.putString("user_id",user_ids.get(position));
                otherprof.setArguments(args);
                loadFragment(otherprof,v);
            }
        });

        holder.follow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String curr_status = currStatus.get(position);
                if(curr_status.equals("Following")){
                    holder.follow.setBackground(holder.itemView.getContext().getDrawable(R.drawable.follow_cta_action));

                    final User user = User.getLoggedInUser();
                    AsyncHttpClient client = new AsyncHttpClient();
                    boolean running = false;
                    RequestParams params = new RequestParams();
                    params.add("uid",user_ids.get(position));
                    JsonHttpResponseHandler jrep = new JsonHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
//                            hide_busy_indicator();
                            broadcastforupdate(holder.itemView);
                            currStatus.set(position,"Follow");
                            Log.d("response_follow",response.toString());
                            holder.follow.setBackground(holder.itemView.getContext().getDrawable(R.drawable.follow_cta_action));
                        }



                        @Override
                        public void onFailure(int statusCode, Header[] headers, Throwable t, JSONObject obj) {
                        }
                    };

                    client.addHeader("Accept", "application/json");
                    client.addHeader("Authorization", "Token " + user.AccessToken);
                    client.post(App.getBaseURL() + "registration/unfollow_user", params, jrep);
                }
                else if(curr_status.equals("Follow")){
                    holder.follow.setBackground(holder.itemView.getContext().getDrawable(R.drawable.following_state));
                    final User user = User.getLoggedInUser();
                    AsyncHttpClient client = new AsyncHttpClient();
                    boolean running = false;
                    RequestParams params = new RequestParams();
                    params.add("uid",user_ids.get(position));
                    JsonHttpResponseHandler jrep = new JsonHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
//                            hide_busy_indicator();
                            broadcastforupdate(holder.itemView);
                            currStatus.set(position,"Following");
                            Log.d("response_follow",response.toString());
                            holder.follow.setBackground(holder.itemView.getContext().getDrawable(R.drawable.following_state));
                        }



                        @Override
                        public void onFailure(int statusCode, Header[] headers, Throwable t, JSONObject obj) {
                        }
                    };

                    client.addHeader("Accept", "application/json");
                    client.addHeader("Authorization", "Token " + user.AccessToken);
                    client.post(App.getBaseURL() + "registration/follow_user", params, jrep);
                }

            }
        });

        float radius = context.getResources().getDimension(R.dimen.default_corner_radius_profile_follow);
        if (!this.img.get(position).equals("")) {
            holder.prof.setShapeAppearanceModel(holder.prof.getShapeAppearanceModel()
                    .toBuilder()
                    .setTopRightCorner(CornerFamily.ROUNDED, radius)
                    .setTopLeftCorner(CornerFamily.ROUNDED, radius)
                    .setBottomLeftCorner(CornerFamily.ROUNDED, radius)
                    .setBottomRightCorner(CornerFamily.ROUNDED, radius)
                    .build());
            Picasso.get().load(this.img.get(position)).into(holder.prof);
        }
        holder.prof.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OtherProfile otherprof = new OtherProfile();
                Bundle args = new Bundle();
                args.putString("user_id",user_ids.get(position));
                otherprof.setArguments(args);
                loadFragment(otherprof,v);
            }
        });


    }


    // total number of cells
    @Override
    public int getItemCount() {
        return names.size();
    }


    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView name;
        TextView username;
        TextView description;
        ImageView contact_selected;
        TextView follow;
        ShapeableImageView prof;
        Boolean is_selected;
        View v;

        ViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.name);
            username = itemView.findViewById(R.id.username_follow);
            description = itemView.findViewById(R.id.description);
            follow = itemView.findViewById(R.id.user_action);
            prof = itemView.findViewById(R.id.profile_img_noti);
            itemView.setOnClickListener(this);
            v = itemView;
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(getAdapterPosition());
        }
    }


    // allows clicks events to be caught
    public void setClickListener(FollowingListAdapter.ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    private void loadFragment(Fragment fragment_to_start, View view) {
        AppCompatActivity activity = (AppCompatActivity) view.getContext();
        FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
        ft.add(R.id.fragment_holder, fragment_to_start);
        ft.commit();
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(int position);
    }

    private void broadcastforupdate(View view) {
        AppCompatActivity activity = (AppCompatActivity) view.getContext();
        Intent intent = new Intent("update_from_follow");
        activity.sendBroadcast(intent);
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
    }
}