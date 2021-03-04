package com.relylabs.InstaHelo.notification;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.shape.CornerFamily;
import com.relylabs.InstaHelo.OtherProfile;
import com.relylabs.InstaHelo.R;
import com.squareup.picasso.Picasso;

import org.ocpsoft.prettytime.PrettyTime;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class NotificationListAdapter extends RecyclerView.Adapter<NotificationListAdapter.ViewHolder> {
    public static final SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private ArrayList<String> text_arr, username,img_arr,time_arr,user_ids;
    private LayoutInflater mInflater;
    private NotificationListAdapter.ItemClickListener mClickListener;
    private Context context;
    private ArrayList<String> currStatus;
    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS =0 ;
    // data is passed into the constructor
    NotificationListAdapter(Context context, ArrayList<String> text_arr, ArrayList<String> username,ArrayList<String> img_arr,ArrayList<String> time_arr,ArrayList<String> user_ids) {
        this.mInflater = LayoutInflater.from(context);
        this.text_arr = text_arr;
        this.username = username;
        this.img_arr = img_arr;
        this.time_arr = time_arr;
        this.context = context;
        this.user_ids = user_ids;
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
        Integer offset = ZonedDateTime.now().getOffset().getTotalSeconds();
        try {
            time_date = inputFormat.parse(this.time_arr.get(position));
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(time_date);
            calendar.add(Calendar.SECOND,offset);
            time_date = calendar.getTime();
        } catch (ParseException e){
            e.printStackTrace();
        }
        String ti = p.format(time_date);
        holder.time.setText(ti);
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
    private void loadFragment(Fragment fragment_to_start, View view) {
        AppCompatActivity activity = (AppCompatActivity) view.getContext();

        FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
        ft.add(R.id.fragment_holder, fragment_to_start);
        ft.commitAllowingStateLoss();
    }
    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
    }
}