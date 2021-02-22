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
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.shape.CornerFamily;
import com.relylabs.InstaHelo.R;
import com.relylabs.InstaHelo.models.EventCardUserElement;
import com.relylabs.InstaHelo.models.EventElement;
import com.relylabs.InstaHelo.models.User;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

public class NewsFeedCardElementAdapter extends RecyclerView.Adapter<NewsFeedCardElementAdapter.ViewHolder> {

    private ArrayList<EventCardUserElement> mData;
    private LayoutInflater mInflater;
    private NewsFeedCardElementAdapter.ItemClickListener mClickListener;

    // data is passed into the constructor
    public NewsFeedCardElementAdapter(Context context, ArrayList<EventCardUserElement> data) {
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
    }

    // inflates the cell layout from xml when needed
    @Override
    public NewsFeedCardElementAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.users_line_item_on_main_screen, parent, false);
        return new NewsFeedCardElementAdapter.ViewHolder(view);
    }

    // binds the data to the textview in each cell
    @Override
    public void onBindViewHolder(@NonNull NewsFeedCardElementAdapter.ViewHolder holder, int position) {
        holder.user_f_name.setText(mData.get(position).Name);
        EventCardUserElement e = mData.get(position);
        if (!e.IsSpeaker) {
            holder.speech_icon.setVisibility(View.INVISIBLE);
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
        ImageView speech_icon;

        ViewHolder(View itemView) {
            super(itemView);
            user_f_name = itemView.findViewById(R.id.user_first_name);
            speech_icon = itemView.findViewById(R.id.speech_icon);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onTagClick(getAdapterPosition());
        }
    }

    // convenience method for getting data at click position
    EventCardUserElement getItem(int id) {
        return mData.get(id);
    }

    // allows clicks events to be caught
    public void setClickListener(NewsFeedCardElementAdapter.ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onTagClick(int index);
    }
}



