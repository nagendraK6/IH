package com.relylabs.InstaHelo.rooms;

import android.content.Context;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.shape.CornerFamily;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.relylabs.InstaHelo.App;
import com.relylabs.InstaHelo.OtherProfile;
import com.relylabs.InstaHelo.R;
import com.relylabs.InstaHelo.Utils.Logger;
import com.relylabs.InstaHelo.models.User;
import com.squareup.picasso.Picasso;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.WeakHashMap;

import cz.msebera.android.httpclient.Header;

import static com.relylabs.InstaHelo.Utils.Helper.loadFragmentAdapter;


public class ScheduleRoomSpeakerAdapter extends RecyclerView.Adapter<ScheduleRoomSpeakerAdapter.ViewHolder> {

    private ArrayList<String> names, usernames,img,user_ids;
    private LayoutInflater mInflater;
    private ScheduleRoomSpeakerAdapter.ItemClickListener mClickListener;
    private Context context;
    // data is passed into the constructor
    ScheduleRoomSpeakerAdapter(Context context, ArrayList<String> names, ArrayList<String> usernames,ArrayList<String> img,ArrayList<String> user_ids) {
        this.mInflater = LayoutInflater.from(context);
        this.names = names;
        this.usernames = usernames;
        this.img = img;
        this.context = context;
        this.user_ids = user_ids;
    }

    @Override
    public ScheduleRoomSpeakerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.schdeule_speaker_item, parent, false);
        return new ScheduleRoomSpeakerAdapter.ViewHolder(view);
    }

    // binds the data to the textview in each cell
    @Override
    public void onBindViewHolder(final ScheduleRoomSpeakerAdapter.ViewHolder holder, final int position) {
        holder.name.setText(this.names.get(position));
        if(!this.img.get(position).equals("")){
            float radius = context.getResources().getDimension(R.dimen.default_corner_radius_profile_schedule);
            holder.prof.setShapeAppearanceModel(holder.prof.getShapeAppearanceModel()
                    .toBuilder()
                    .setTopRightCorner(CornerFamily.ROUNDED, radius)
                    .setTopLeftCorner(CornerFamily.ROUNDED, radius)
                    .setBottomLeftCorner(CornerFamily.ROUNDED, radius)
                    .setBottomRightCorner(CornerFamily.ROUNDED, radius)
                    .build());
            Picasso.get().load(this.img.get(position)).into(holder.prof);
        }
        holder.name.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openProfile(new OtherProfile(),position,v);
            }
        });
        holder.prof.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openProfile(new OtherProfile(),position,v);
            }
        });

    }


    // total number of cells
    @Override
    public int getItemCount() {
        return names.size();
    }


    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        ShapeableImageView prof;
        TextView name;
        ViewHolder(View itemView) {
            super(itemView);
            prof = itemView.findViewById(R.id.speaker_listener_moderator_image);
            name = itemView.findViewById(R.id.user_name_prof);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(getAdapterPosition());
        }
    }


    // allows clicks events to be caught
    public void setClickListener(ScheduleRoomSpeakerAdapter.ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(int position);
    }
    private void openProfile(Fragment otherprof,int position, View v){
        Bundle args = new Bundle();
        args.putString("user_id",user_ids.get(position));
        otherprof.setArguments(args);
        loadFragmentAdapter(otherprof,v);
    }
    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
    }
}