package com.relylabs.InstaHelo;

import android.content.Context;

import androidx.annotation.NonNull;
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
import android.widget.TextView;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.shape.CornerFamily;
import com.relylabs.InstaHelo.R;
import com.relylabs.InstaHelo.bottomsheet.BottomScheduleRoom;
import com.relylabs.InstaHelo.models.EventElement;
import com.relylabs.InstaHelo.models.User;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import de.hdodenhof.circleimageview.CircleImageView;

public class NewsFeedAdapter extends RecyclerView.Adapter<NewsFeedAdapter.ViewHolder> implements BottomScheduleRoom.ItemClickListener {

    private ArrayList<EventElement> mData;
    private LayoutInflater mInflater;
    private NewsFeedAdapter.ItemClickListener mClickListener;
    final Calendar myCalendar = Calendar.getInstance();
    // data is passed into the constructor
    public NewsFeedAdapter(Context context, ArrayList<EventElement> data) {
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
        holder.tagDisplayView.setText(mData.get(position).eventTitle);

        EventElement e = mData.get(position);
        if (e.eventPhotoUrls.size() < 2) {
            holder.c2.setVisibility(View.INVISIBLE);
        }
        if(e.isScheduled){
            holder.time.setVisibility(View.VISIBLE);
            myCalendar.setTimeInMillis(e.scheduleTimestamp);
            String myFormat = "E, dd MMM yyyy hh:mm a z"; //In which you need put here
            SimpleDateFormat sdf = new SimpleDateFormat(myFormat);
            holder.time.setText(sdf.format(myCalendar.getTime()).toUpperCase());
        }
        String p1 = "";
        String p2 = "";

        if (e.eventPhotoUrls.size() > 0) {
            p1 = e.eventPhotoUrls.get(0);
        }

        if (e.eventPhotoUrls.size() > 1) {
            p2 = e.eventPhotoUrls.get(1);
        }


        if (!p1.equals("")) {
            Picasso.get().load(p1).into(holder.c1);
        } else {
            holder.c1.setImageDrawable(holder.itemView.getContext().getDrawable(R.drawable.empty_inviter));
        }

        if (!p2.equals("")) {
            Picasso.get().load(p2).into(holder.c2);
        } else {
            holder.c2.setImageDrawable(holder.itemView.getContext().getDrawable(R.drawable.empty_inviter));
        }

        NewsFeedCardElementAdapter adapter = new NewsFeedCardElementAdapter(holder.itemView.getContext(), mData.get(position).userElements);
        adapter.setClickListener(new NewsFeedCardElementAdapter.ItemClickListener() {
            @Override
            public void onTagClick(int index) {
                if (mClickListener != null) mClickListener.onTagClick(position);
            }
        });
        holder.speaker_audience_list.setAdapter(adapter);

        PreCachingLayoutManager layoutManager = new PreCachingLayoutManager(holder.itemView.getContext());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        layoutManager.setExtraLayoutSpace(DeviceUtils.getScreenHeight(holder.itemView.getContext()));
        holder.speaker_audience_list.setLayoutManager(layoutManager);

    }

    // total number of cells
    @Override
    public int getItemCount() {
        return mData.size();
    }

    @Override
    public void onItemClick(String item) {
        Log.d("here","bottom");
    }


    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView tagDisplayView;
        ShapeableImageView c1, c2;
        RecyclerView speaker_audience_list;
        TextView time;
        ViewHolder(View itemView) {
            super(itemView);
            tagDisplayView = itemView.findViewById(R.id.event_title);
            c1 = itemView.findViewById(R.id.image_1);
            c2 = itemView.findViewById(R.id.image_2);
            time = itemView.findViewById(R.id.schedule_time);
            speaker_audience_list = itemView.findViewById(R.id.users_in_the_event_for_display);

            float radius = itemView.getResources().getDimension(R.dimen.default_corner_news_feed_image_profile);
            c1.setShapeAppearanceModel(c1.getShapeAppearanceModel()
                    .toBuilder()
                    .setTopRightCorner(CornerFamily.ROUNDED,radius)
                    .setTopLeftCorner(CornerFamily.ROUNDED,radius)
                    .setBottomLeftCorner(CornerFamily.ROUNDED,radius)
                    .setBottomRightCorner(CornerFamily.ROUNDED,radius)
                    .build());

            c2.setShapeAppearanceModel(c1.getShapeAppearanceModel()
                    .toBuilder()
                    .setTopRightCorner(CornerFamily.ROUNDED,radius)
                    .setTopLeftCorner(CornerFamily.ROUNDED,radius)
                    .setBottomLeftCorner(CornerFamily.ROUNDED,radius)
                    .setBottomRightCorner(CornerFamily.ROUNDED,radius)
                    .build());


            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onTagClick(getAdapterPosition());
        }
    }

    // convenience method for getting data at click position
    EventElement getItem(int id) {
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