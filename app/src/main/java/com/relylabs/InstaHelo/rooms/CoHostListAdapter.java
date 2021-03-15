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


public class CoHostListAdapter extends RecyclerView.Adapter<CoHostListAdapter.ViewHolder> {

    private ArrayList<String> names, usernames,bio,img,user_ids,main_user_ids;
    private LayoutInflater mInflater;
    private CoHostListAdapter.ItemClickListener mClickListener;
    private Context context;
    private ArrayList<Boolean> isChecked;
    CoHostListAdapter(Context context, ArrayList<String> names, ArrayList<String> usernames,ArrayList<String> bio,ArrayList<String> img,ArrayList<String> user_ids, ArrayList<Boolean> isChecked, ArrayList<String> main_user_ids) {
        this.mInflater = LayoutInflater.from(context);
        this.names = names;
        this.usernames = usernames;
        this.bio = bio;
        this.img = img;
        this.context = context;
        this.user_ids = user_ids;
        this.isChecked = isChecked;
        this.main_user_ids = main_user_ids;
        Log.d("isCheck",this.isChecked.toString());
    }

    @Override
    public CoHostListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.cohost_list_item, parent, false);
        return new CoHostListAdapter.ViewHolder(view);
    }

    // binds the data to the textview in each cell
    @Override
    public void onBindViewHolder(final CoHostListAdapter.ViewHolder holder, final int position) {
        holder.name.setText(this.names.get(position));
        int pos = main_user_ids.indexOf(user_ids.get(position));
        holder.username.setText("("+this.usernames.get(position)+")");
        String currBio = this.bio.get(position);
        if(currBio.length()>50){
            currBio = currBio.substring(0,50);
            currBio = currBio + "...";
        }
        holder.description.setText(currBio);
        if(isChecked.get(pos)){
            holder.user_action.setBackground(holder.itemView.getContext().getDrawable(R.drawable.tic_mark_selected));
        }
        else{
            holder.user_action.setBackground(holder.itemView.getContext().getDrawable(R.drawable.tic_mark_not_selected));
        }

        float radius = context.getResources().getDimension(R.dimen.default_corner_radius_profile_follow);
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
        holder.item.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mClickListener.onItemClick(position);
                if(!isChecked.get(pos)){
                    holder.user_action.setBackground(holder.itemView.getContext().getDrawable(R.drawable.tic_mark_selected));
                    isChecked.set(pos,true);
                }
                else{
                    holder.user_action.setBackground(holder.itemView.getContext().getDrawable(R.drawable.tic_mark_not_selected));
                    isChecked.set(pos,false);
                }
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
        TextView username;
        TextView description;
        ImageView contact_selected;
        TextView user_action;
        ShapeableImageView prof;
        Boolean is_selected;
        View v;
        RelativeLayout item;
        ViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.name);
            username = itemView.findViewById(R.id.username_follow);
            description = itemView.findViewById(R.id.description);
            user_action = itemView.findViewById(R.id.user_action);
            prof = itemView.findViewById(R.id.profile_img_noti);
            item = itemView.findViewById(R.id.item);
            itemView.setOnClickListener(this);
            v = itemView;
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(getAdapterPosition());
        }
    }


    // allows clicks events to be caught
    public void setClickListener(CoHostListAdapter.ItemClickListener itemClickListener) {
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