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

public class SuggestedProfileToFollowAdapter extends RecyclerView.Adapter<SuggestedProfileToFollowAdapter.ViewHolder> {

    private ArrayList<String> contact_names;
    private ArrayList<String> profile_image_urls;
    private ArrayList<String> bio_description;

    private LayoutInflater mInflater;
    private ItemClickListener mClickListener;
    private Context context;

    // data is passed into the constructor
    SuggestedProfileToFollowAdapter(Context context, ArrayList<String> contact_names, ArrayList<String> bio_description, ArrayList<String> profile_image_urls) {
        this.mInflater = LayoutInflater.from(context);
        this.profile_image_urls = profile_image_urls;
        this.contact_names = contact_names;
        this.bio_description = bio_description;
        this.context = context;
    }

    // inflates the cell layout from xml when needed
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.suggested_profiles_items, parent, false);
        return new ViewHolder(view);
    }

    // binds the data to the textview in each cell
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.suggestion_profile_name.setText(contact_names.get(position));
        holder.suggestion_profile_bio.setText(bio_description.get(position));
        if (!profile_image_urls.get(position).equals("")) {
            Picasso.get()
                    .load(profile_image_urls.get(position))
                    .into(holder.suggested_profile_image);
        }
    }

    // total number of cells
    @Override
    public int getItemCount() {
        return contact_names.size();
    }


    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        CircleImageView suggested_profile_image;
        TextView suggestion_profile_name, suggestion_profile_bio;
        ImageView selected, deselected;
        Boolean selected_flag = true;

        ViewHolder(View itemView) {
            super(itemView);
            suggested_profile_image = itemView.findViewById(R.id.suggested_profile_image);
            suggestion_profile_name = itemView.findViewById(R.id.suggestion_profile_name);
            suggestion_profile_bio = itemView.findViewById(R.id.suggestion_profile_bio);
            selected = itemView.findViewById(R.id.selected);
            deselected = itemView.findViewById(R.id.deselected);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (selected_flag) {
                selected_flag = false;
                selected.setVisibility(View.INVISIBLE);
                deselected.setVisibility(View.VISIBLE);
            } else {
                selected_flag = true;
                selected.setVisibility(View.VISIBLE);
                deselected.setVisibility(View.INVISIBLE);
            }
            mClickListener.onItemClick(view, getAdapterPosition(), selected_flag);
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
