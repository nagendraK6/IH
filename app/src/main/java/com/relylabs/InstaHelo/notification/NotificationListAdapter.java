package com.relylabs.InstaHelo.notification;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.shape.CornerFamily;
import com.relylabs.InstaHelo.R;
import com.squareup.picasso.Picasso;

import org.ocpsoft.prettytime.PrettyTime;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class NotificationListAdapter extends RecyclerView.Adapter<NotificationListAdapter.ViewHolder> {
    public static final SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private ArrayList<String> text_arr, username,img_arr,time_arr;
    private LayoutInflater mInflater;
    private NotificationListAdapter.ItemClickListener mClickListener;
    private Context context;
    private ArrayList<String> currStatus;
    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS =0 ;
    // data is passed into the constructor
    NotificationListAdapter(Context context, ArrayList<String> text_arr, ArrayList<String> username,ArrayList<String> img_arr,ArrayList<String> time_arr) {
        this.mInflater = LayoutInflater.from(context);
        this.text_arr = text_arr;
        this.username = username;
        this.img_arr = img_arr;
        this.time_arr = time_arr;
        this.context = context;
    }

    @Override
    public NotificationListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.notification_list_item, parent, false);
        return new NotificationListAdapter.ViewHolder(view);
    }

    // binds the data to the textview in each cell
    @Override
    public void onBindViewHolder(final NotificationListAdapter.ViewHolder holder, final int position) {
        if(!this.img_arr.get(position).equals("")){
            float radius = context.getResources().getDimension(R.dimen.default_corner_radius_profile_notify);
            holder.prof.setShapeAppearanceModel(holder.prof.getShapeAppearanceModel()
                    .toBuilder()
                    .setTopRightCorner(CornerFamily.ROUNDED, radius)
                    .setTopLeftCorner(CornerFamily.ROUNDED, radius)
                    .setBottomLeftCorner(CornerFamily.ROUNDED, radius)
                    .setBottomRightCorner(CornerFamily.ROUNDED, radius)
                    .build());
            Picasso.get().load(this.img_arr.get(position)).into(holder.prof);
        }
        holder.notification_text.setText(this.text_arr.get(position));
        PrettyTime p = new PrettyTime();
        Date time_date = null;
        try {
            time_date = inputFormat.parse(this.time_arr.get(position));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        String ti = p.format(time_date);
        holder.time.setText(ti);
    }


    // total number of cells
    @Override
    public int getItemCount() {
        return username.size();
    }


    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView time;
        TextView notification_text;
        ShapeableImageView prof;


        ViewHolder(View itemView) {
            super(itemView);
            notification_text = itemView.findViewById(R.id.notification_text);
            time = itemView.findViewById(R.id.time);
            prof = itemView.findViewById(R.id.profile_img_noti);
            itemView.setOnClickListener(this);
        }
        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(getAdapterPosition());
        }
    }


    // allows clicks events to be caught
    public void setClickListener(NotificationListAdapter.ItemClickListener itemClickListener) {
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
