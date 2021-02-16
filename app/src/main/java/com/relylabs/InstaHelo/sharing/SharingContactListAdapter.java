package com.relylabs.InstaHelo.sharing;

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

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.relylabs.InstaHelo.App;
import com.relylabs.InstaHelo.R;
import com.relylabs.InstaHelo.models.Contact;
import com.relylabs.InstaHelo.models.User;
import com.relylabs.InstaHelo.onboarding.SuggestedProfileToFollowFragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;

public class SharingContactListAdapter extends RecyclerView.Adapter<SharingContactListAdapter.ViewHolder> {

    private ArrayList<String> contact_names, contact_numbers;
    private LayoutInflater mInflater;
    private SharingContactListAdapter.ItemClickListener mClickListener;
    private Context context;

    // data is passed into the constructor
    SharingContactListAdapter(Context context, ArrayList<String> contact_names, ArrayList<String> contact_numbers) {
        this.mInflater = LayoutInflater.from(context);
        this.contact_numbers = contact_numbers;
        this.contact_names = contact_names;
        this.context = context;
    }

    // inflates the cell layout from xml when needed
    @Override
    public SharingContactListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.contact_list_invite_item, parent, false);
        return new SharingContactListAdapter.ViewHolder(view);
    }

    // binds the data to the textview in each cell
    @Override
    public void onBindViewHolder(final SharingContactListAdapter.ViewHolder holder, final int position) {
        holder.name.setText(this.contact_names.get(position));

        holder.name.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mClickListener != null) mClickListener.onItemClick(position);

            }
        });

        holder.v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mClickListener != null) mClickListener.onItemClick(position);

            }
        });
    }

    // total number of cells
    @Override
    public int getItemCount() {
        return contact_names.size();
    }


    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView name;
        ImageView contact_selected;
        Boolean is_selected;
        View v;

        ViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.contact_name);
            itemView.setOnClickListener(this);
            v = itemView;
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(getAdapterPosition());
        }
    }


    // allows clicks events to be caught
    public void setClickListener(SharingContactListAdapter.ItemClickListener itemClickListener) {
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
