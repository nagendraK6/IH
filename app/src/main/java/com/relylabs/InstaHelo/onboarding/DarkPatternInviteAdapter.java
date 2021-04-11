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
import com.relylabs.InstaHelo.models.Contact;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

public class DarkPatternInviteAdapter extends RecyclerView.Adapter<DarkPatternInviteAdapter.ViewHolder> {

    private ArrayList<Contact> contacts;
    Boolean selected_all = Boolean.FALSE;
    private LayoutInflater mInflater;
    private ItemClickListener mClickListener;
    private Context context;

    // data is passed into the constructor
    DarkPatternInviteAdapter(Context context, ArrayList<Contact> contacts, Boolean selected_all) {
        this.mInflater = LayoutInflater.from(context);
        this.contacts = contacts;
        this.context = context;
        this.selected_all = selected_all;
    }

    // inflates the cell layout from xml when needed
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.dark_patter_inviter_element, parent, false);
        return new ViewHolder(view);
    }

    // binds the data to the textview in each cell
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Contact c = contacts.get(position);
        holder.display_name.setText(c.Name);
        if (c.Name.length() > 0) {
            String first_char = c.Name.substring(0, 1);
            holder.blank_profile_image.setText(first_char);
        }
        holder.phone_no.setText(c.Phone);
        if (selected_all) {
            holder.tic_mark_selected_deselected.setImageDrawable(holder.itemView.getContext().getDrawable(R.drawable.tic_mark_selected));
        } else {
            holder.tic_mark_selected_deselected.setImageDrawable(holder.itemView.getContext().getDrawable(R.drawable.tic_mark_not_selected));
        }
    }

    // total number of cells
    @Override
    public int getItemCount() {
        return contacts.size();
    }


    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView phone_no, display_name, blank_profile_image;
        ImageView tic_mark_selected_deselected;
        Boolean selected = selected_all;

        ViewHolder(View itemView) {
            super(itemView);
            display_name = itemView.findViewById(R.id.display_name);
            blank_profile_image = itemView.findViewById(R.id.blank_profile_image);
            phone_no = itemView.findViewById(R.id.phone_no);
            tic_mark_selected_deselected = itemView.findViewById(R.id.tic_mark_selected_deselected);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (selected) {
                selected = false;
                tic_mark_selected_deselected.setImageDrawable(itemView.getContext().getDrawable(R.drawable.tic_mark_not_selected));
            } else {
                selected = true;
                tic_mark_selected_deselected.setImageDrawable(itemView.getContext().getDrawable(R.drawable.tic_mark_selected));
            }
            mClickListener.onItemClick(view, getAdapterPosition(), selected);
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
