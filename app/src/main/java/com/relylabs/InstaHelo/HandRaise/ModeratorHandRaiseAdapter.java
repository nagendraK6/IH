package com.relylabs.InstaHelo.HandRaise;


import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.shape.CornerFamily;
import com.relylabs.InstaHelo.R;
import com.relylabs.InstaHelo.models.EventCardUserElement;
import com.relylabs.InstaHelo.models.EventElement;
import com.relylabs.InstaHelo.models.User;
import com.relylabs.InstaHelo.models.UserWithImage;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

public class ModeratorHandRaiseAdapter extends RecyclerView.Adapter<ModeratorHandRaiseAdapter.ViewHolder> {

    private ArrayList<UserWithImage> mData;
    private LayoutInflater mInflater;
    private ModeratorHandRaiseAdapter.ItemClickListener mClickListener;

    // data is passed into the constructor
    public ModeratorHandRaiseAdapter(Context context, ArrayList<UserWithImage> data) {
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
    }

    // inflates the cell layout from xml when needed
    @Override
    public ModeratorHandRaiseAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.hand_raise_users_line_item, parent, false);
        return new ModeratorHandRaiseAdapter.ViewHolder(view);
    }

    // binds the data to the textview in each cell
    @Override
    public void onBindViewHolder(@NonNull ModeratorHandRaiseAdapter.ViewHolder holder, int position) {
        holder.user_f_name.setText(mData.get(position).FirstName);
        UserWithImage e = mData.get(position);
        if (!e.profileImageURL.equals("")) {
            Picasso.get().load(e.profileImageURL).into(holder.user_image);
        }

    }

    // total number of cells
    @Override
    public int getItemCount() {
        return mData.size();
    }


    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView user_f_name;
        CircleImageView user_image;
        View select_icon, reject_icon;

        ViewHolder(View itemView) {
            super(itemView);
            user_image = itemView.findViewById(R.id.audience_image);
            user_f_name = itemView.findViewById(R.id.audience_name);
            select_icon = itemView.findViewById(R.id.select_icon);
            reject_icon = itemView.findViewById(R.id.reject_icon);

            select_icon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mClickListener != null) mClickListener.onTagClick(getAdapterPosition(), "MAKE_SPEAKER");
                }
            });

            reject_icon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mClickListener != null) mClickListener.onTagClick(getAdapterPosition(), "REJECT_SPEAKER");
                }
            });
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
        }
    }

    // convenience method for getting data at click position
    UserWithImage getItem(int id) {
        return mData.get(id);
    }

    // allows clicks events to be caught
    public void setClickListener(ModeratorHandRaiseAdapter.ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onTagClick(int index, String action);
    }
}