package com.relylabs.InstaHelo.rooms;

import android.content.Context;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.shape.CornerFamily;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.relylabs.InstaHelo.App;
import com.relylabs.InstaHelo.NewsFeedCardElementAdapter;
import com.relylabs.InstaHelo.OtherProfile;
import com.relylabs.InstaHelo.R;
import com.relylabs.InstaHelo.Utils.Logger;
import com.relylabs.InstaHelo.models.User;
import com.squareup.picasso.Picasso;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.WeakHashMap;

import cz.msebera.android.httpclient.Header;

import com.relylabs.InstaHelo.Utils.Helper;


public class FinalCoHostAdapter extends RecyclerView.Adapter<FinalCoHostAdapter.ViewHolder> {

    private ArrayList<String> names,img,user_ids;
    private LayoutInflater mInflater;
    private FinalCoHostAdapter.ItemClickListener mClickListener;
    private Context context;
    Boolean isChecked = false;
    FinalCoHostAdapter(Context context, ArrayList<String> names,ArrayList<String> img,ArrayList<String> user_ids) {
        this.mInflater = LayoutInflater.from(context);
        this.names = names;
        this.img = img;
        this.context = context;
        this.user_ids = user_ids;
    }

    @Override
    public FinalCoHostAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.final_co_host_item, parent, false);
        return new FinalCoHostAdapter.ViewHolder(view);
    }

    // binds the data to the textview in each cell
    @Override
    public void onBindViewHolder(final FinalCoHostAdapter.ViewHolder holder, final int position) {
        holder.name.setText(this.names.get(position));
        float radius = context.getResources().getDimension(R.dimen.default_corner_radius_profile_schedule_small);
        if (!this.img.get(position).equals("")) {
            holder.prof.setShapeAppearanceModel(holder.prof.getShapeAppearanceModel()
                    .toBuilder()
                    .setTopRightCorner(CornerFamily.ROUNDED, radius)
                    .setTopLeftCorner(CornerFamily.ROUNDED, radius)
                    .setBottomLeftCorner(CornerFamily.ROUNDED, radius)
                    .setBottomRightCorner(CornerFamily.ROUNDED, radius)
                    .build());
            Picasso.get().load(this.img.get(position)).into(holder.prof);
        }
        if(position == 0){
            holder.remove_host.setVisibility(View.INVISIBLE);
        }
        holder.remove_host.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mClickListener.onItemClick(position);

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
        TextView name;
        ImageView remove_host;
        ShapeableImageView prof;
        View v;
        ViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.name);
            prof = itemView.findViewById(R.id.profile_img_noti);
            remove_host = itemView.findViewById(R.id.remove_host);
            itemView.setOnClickListener(this);
            v = itemView;
        }

        @Override
        public void onClick(View view) {
//            if (mClickListener != null) mClickListener.onItemClick(getAdapterPosition());
        }
    }


    // allows clicks events to be caught
    public void setClickListener(FinalCoHostAdapter.ItemClickListener itemClickListener) {
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