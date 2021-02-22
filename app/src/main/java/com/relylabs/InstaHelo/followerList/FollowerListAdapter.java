package com.relylabs.InstaHelo.followerList;

import android.content.Context;

import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.shape.CornerFamily;
import com.relylabs.InstaHelo.R;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;


public class FollowerListAdapter extends RecyclerView.Adapter<FollowerListAdapter.ViewHolder> {

    private ArrayList<String> names, usernames,bio,img;
    private LayoutInflater mInflater;
    private FollowerListAdapter.ItemClickListener mClickListener;
    private Context context;
    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS =0 ;
    // data is passed into the constructor
    FollowerListAdapter(Context context, ArrayList<String> names, ArrayList<String> usernames,ArrayList<String> bio,ArrayList<String> img) {
        this.mInflater = LayoutInflater.from(context);
        this.names = names;
        this.usernames = usernames;
        this.bio = bio;
        this.img = img;
        this.context = context;
    }

    @Override
    public FollowerListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.follower_list_item, parent, false);
        return new FollowerListAdapter.ViewHolder(view);
    }

    // binds the data to the textview in each cell
    @Override
    public void onBindViewHolder(final FollowerListAdapter.ViewHolder holder, final int position) {
        holder.name.setText(this.names.get(position));
        holder.username.setText("("+this.usernames.get(position)+")");
        holder.description.setText(this.bio.get(position));
        holder.name.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mClickListener != null) mClickListener.onItemClick(position);

            }
        });
        holder.follow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("follow_click","Clicked follow");

            }
        });

        float radius = context.getResources().getDimension(R.dimen.default_corner_radius_profile_follow);
        holder.prof.setShapeAppearanceModel(holder.prof.getShapeAppearanceModel()
                .toBuilder()
                .setTopRightCorner(CornerFamily.ROUNDED,radius)
                .setTopLeftCorner(CornerFamily.ROUNDED,radius)
                .setBottomLeftCorner(CornerFamily.ROUNDED,radius)
                .setBottomRightCorner(CornerFamily.ROUNDED,radius)
                .build());
        Picasso.get().load(this.img.get(position)).into(holder.prof);


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
            follow = itemView.findViewById(R.id.follow);
            prof = itemView.findViewById(R.id.profile_img_follow);
            itemView.setOnClickListener(this);
            v = itemView;
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(getAdapterPosition());
        }
    }


    // allows clicks events to be caught
    public void setClickListener(FollowerListAdapter.ItemClickListener itemClickListener) {
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
