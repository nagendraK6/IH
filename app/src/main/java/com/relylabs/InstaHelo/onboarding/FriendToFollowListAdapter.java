package com.relylabs.InstaHelo.onboarding;

import android.content.Context;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.relylabs.InstaHelo.R;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

public class FriendToFollowListAdapter extends RecyclerView.Adapter<FriendToFollowListAdapter.ViewHolder> {

    private ArrayList<String> contact_names;
    private ArrayList<String> profile_image_urls;

    private LayoutInflater mInflater;
    private ItemClickListener mClickListener;
    private Context context;

    // data is passed into the constructor
    FriendToFollowListAdapter(Context context, ArrayList<String> contact_names, ArrayList<String> profile_image_urls) {
        this.mInflater = LayoutInflater.from(context);
        this.profile_image_urls = profile_image_urls;
        this.contact_names = contact_names;
        this.context = context;
    }

    // inflates the cell layout from xml when needed
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.friend_suggestion_image, parent, false);
        return new ViewHolder(view);
    }

    // binds the data to the textview in each cell
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.nameView.setText(contact_names.get(position));
        if (!profile_image_urls.get(position).equals("")) {
            Picasso.get()
                    .load(profile_image_urls.get(position))
                    .into(holder.profileImageView);
        }
    }

    // total number of cells
    @Override
    public int getItemCount() {
        return contact_names.size();
    }


    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        CircleImageView profileImageView;
        ImageView tick_mark;
        TextView nameView;
        Boolean selected = Boolean.TRUE;

        ViewHolder(View itemView) {
            super(itemView);
            profileImageView = itemView.findViewById(R.id.friend_profile_image);
            nameView = itemView.findViewById(R.id.friend_name);
            tick_mark = itemView.findViewById(R.id.selected_deselected);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) {
                if (selected) {
                    selected = false;
                    tick_mark.setVisibility(View.INVISIBLE);
                } else {
                    selected = true;
                    tick_mark.setVisibility(View.VISIBLE);
                }
                mClickListener.onItemClick(view, getAdapterPosition(), selected);
            }
        }
    }


    // allows clicks events to be caught
    public void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position, Boolean selected);
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
    }
}
