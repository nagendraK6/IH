package com.relylabs.InstaHelo;

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
import android.widget.TextView;

import com.relylabs.InstaHelo.R;
import com.relylabs.InstaHelo.models.User;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

public class NewsFeedAdapter extends RecyclerView.Adapter<NewsFeedAdapter.ViewHolder> {

    private ArrayList<String> mData;
    private LayoutInflater mInflater;
    private NewsFeedAdapter.ItemClickListener mClickListener;

    // data is passed into the constructor
    public NewsFeedAdapter(Context context, ArrayList<String> data) {
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
    }

    // inflates the cell layout from xml when needed
    @Override
    public NewsFeedAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.news_feed_item, parent, false);
        return new NewsFeedAdapter.ViewHolder(view);
    }

    // binds the data to the textview in each cell
    @Override
    public void onBindViewHolder(@NonNull NewsFeedAdapter.ViewHolder holder, int position) {
        holder.tagDisplayView.setText(mData.get(position));
        User user = User.getLoggedInUser();
        if (!user.ProfilePicURL.equals("")) {
            Picasso.get().load(user.ProfilePicURL).into(holder.c1);
            Picasso.get().load(user.ProfilePicURL).into(holder.c2);
        } else {
            holder.c1.setImageDrawable(holder.itemView.getContext().getDrawable(R.drawable.empty_inviter));
            holder.c2.setImageDrawable(holder.itemView.getContext().getDrawable(R.drawable.empty_inviter));
        }
    }

    // total number of cells
    @Override
    public int getItemCount() {
        return mData.size();
    }


    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView tagDisplayView;
        CircleImageView c1, c2;

        ViewHolder(View itemView) {
            super(itemView);
            tagDisplayView = itemView.findViewById(R.id.event_title);
            c1 = itemView.findViewById(R.id.image_1);
            c2 = itemView.findViewById(R.id.image_2);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onTagClick(getAdapterPosition());
        }
    }

    // convenience method for getting data at click position
    String getItem(int id) {
        return mData.get(id);
    }

    // allows clicks events to be caught
    public void setClickListener(NewsFeedAdapter.ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onTagClick(int index);
    }
}



