package com.relylabs.InstaHelo.rooms;

import android.app.AlertDialog;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.relylabs.InstaHelo.App;
import com.relylabs.InstaHelo.R;
import com.relylabs.InstaHelo.Utils.Logger;
import com.relylabs.InstaHelo.models.User;
import com.relylabs.InstaHelo.models.UsersInRoom;
import com.relylabs.InstaHelo.onboarding.DisplayUserNameAskFragment;
//import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

import cz.msebera.android.httpclient.Header;
import de.hdodenhof.circleimageview.CircleImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;


public class RoomsUsersDisplayListAdapter extends RecyclerView.Adapter<RoomsUsersDisplayListAdapter.ViewHolder> {

    private ArrayList<UsersInRoom> mData;
    private Boolean is_admin_current_user;
    private LayoutInflater mInflater;
    private RoomsUsersDisplayListAdapter.ItemClickListener mClickListener;

    // data is passed into the constructor
    public RoomsUsersDisplayListAdapter(Context context, ArrayList<UsersInRoom> data, Boolean is_admin) {
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
        this.is_admin_current_user = is_admin;
    }


    //speaker_listener_moderator_image
    // image voice_action
    // speaker_listener_moderator_name
    // inflates the cell layout from xml when needed
    @Override
    public RoomsUsersDisplayListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.speaker_listener_moderator_item, parent, false);
        return new RoomsUsersDisplayListAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull List<Object> payloads) {
       // super.onBindViewHolder(holder, position, payloads);
        int viewType = getItemViewType(position);
        Boolean is_muted = false, IsSpeaker = false, IsdataFetchRequired = false;
        String profileImageURL = "";
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads);
            return;
        } else {
            Bundle o = (Bundle) payloads.get(0);
            for (String key : o.keySet()) {
                if (key.equals("IsMuted")) {
                    is_muted = o.getBoolean("IsMuted");
                    if(!is_muted) {
                        holder.voice_action.setBackground(holder.itemView.getContext().getDrawable(R.drawable.mic_on_room_view));
                    } else {
                        holder.voice_action.setBackground(holder.itemView.getContext().getDrawable(R.drawable.mic_off));
                    }
                }

                if (key.equals("profileImageURL")) {
                    profileImageURL = o.getString("profileImageURL");
                    if (profileImageURL != null && !profileImageURL.equals("")) {
                      //  Picasso.get().load(profileImageURL).into(holder.speaker_listener_moderator_image);
                    } else {
                        holder.speaker_listener_moderator_image.setImageDrawable(holder.itemView.getContext().getDrawable(R.drawable.empty_user_profile_image));
                    }
                }

                if (key.equals("IsSpeaker")) {
                    IsSpeaker = o.getBoolean("IsSpeaker");
                    if (!IsSpeaker) {
                        holder.voice_action.setVisibility(View.INVISIBLE);
                    } else {
                        if(!mData.get(position).IsMuted) {
                            holder.voice_action.setBackground(holder.itemView.getContext().getDrawable(R.drawable.mic_on_room_view));
                        } else {
                            holder.voice_action.setBackground(holder.itemView.getContext().getDrawable(R.drawable.mic_off));
                        }
                        holder.voice_action.setVisibility(View.VISIBLE);
                    }
                }

                if (key.equals("IsdataFetchRequired")) {
                    IsdataFetchRequired = o.getBoolean("IsdataFetchRequired");
                    if (IsdataFetchRequired) {
                        fetch_data(mData.get(position).UserId, holder);
                    }
                }
            }
        }
    }


    // binds the data to the textview in each cell

    @Override
    public void onBindViewHolder(@NonNull RoomsUsersDisplayListAdapter.ViewHolder holder, int position) {
   //     super.onBindViewHolder(holder, position) );
        if (!mData.get(position).IsSpeaker) {
            holder.voice_action.setVisibility(View.INVISIBLE);
        } else {
            if(!mData.get(position).IsMuted) {
                holder.voice_action.setBackground(holder.itemView.getContext().getDrawable(R.drawable.mic_on_room_view));
            } else {
                holder.voice_action.setBackground(holder.itemView.getContext().getDrawable(R.drawable.mic_off));
            }
            holder.voice_action.setVisibility(View.VISIBLE);
        }


        RequestOptions options = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .skipMemoryCache(false)
                .centerCrop();


        String image_url = mData.get(position).profileImageURL;
        String name =  mData.get(position).Name;
        holder.speaker_listener_moderator_name.setText(name);
        if (image_url!= null &&  !image_url.equals("")) {
          //  Picasso.get().load(image_url).into(holder.speaker_listener_moderator_image);
            Glide.with(holder.itemView.getContext()).load(image_url).into(holder.speaker_listener_moderator_image);

        } else {
            holder.speaker_listener_moderator_image.setImageDrawable(holder.itemView.getContext().getDrawable(R.drawable.empty_user_profile_image));
        }


        if (mData.get(position).IsdataFetchRequired) {
            fetch_data(mData.get(position).UserId, holder);
        }
    }

    // total number of cells
    @Override
    public int getItemCount() {
        return mData.size();
    }

    private  void fetch_data(Integer uid, RoomsUsersDisplayListAdapter.ViewHolder holder) {
        User user = User.getLoggedInUser();
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        params.add("user_id", String.valueOf(uid));


        JsonHttpResponseHandler jrep= new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                Log.d("debug_data", "data");
                try {
                    String error_message = response.getString("error_message");
                    Log.d("debug_data", error_message);
                    if (error_message.equals("SUCCESS")) {
                        String name = response.getString("Name");
                        String image_url = response.getString("ImageProfileURL");
                        Log.d("debug_data", name);
                        holder.speaker_listener_moderator_name.setText(name);
                        if (!image_url.equals("")) {
                            Glide.with(holder.itemView.getContext()).load(image_url).into(holder.speaker_listener_moderator_image);

                            //   Glide.get().load(image_url).into(holder.speaker_listener_moderator_image);
                        } else {
                            holder.speaker_listener_moderator_image.setImageDrawable(holder.itemView.getContext().getDrawable(R.drawable.empty_user_profile_image));
                        }
                        Log.d("debug_data", "udated info");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String res, Throwable t) {
              //  WeakHashMap<String, String> log_data = new WeakHashMap<>();
              //  log_data.put(Logger.STATUS, Integer.toString(statusCode));
              //  log_data.put(Logger.RES, res);
              //  log_data.put(Logger.THROWABLE, t.toString());
              //  Logger.log(Logger.USER_NAME_SEND_REQUEST_FAILED, log_data);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable t, JSONObject obj) {
               // WeakHashMap<String, String> log_data = new WeakHashMap<>();
               // log_data.put(Logger.STATUS, Integer.toString(statusCode));
              //  log_data.put(Logger.JSON, obj.toString());
              //  log_data.put(Logger.THROWABLE, t.toString());
              //  Logger.log(Logger.USER_NAME_SEND_REQUEST_FAILED, log_data);
            }
        };

        client.addHeader("Accept", "application/json");
        client.addHeader("Authorization", "Token " + user.AccessToken);
        client.post( App.getBaseURL() + "registration/get_user_info", params, jrep);
    }

    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
         CircleImageView speaker_listener_moderator_image;
         ImageView voice_action;
         TextView speaker_listener_moderator_name;

        ViewHolder(View itemView) {
            super(itemView);
            speaker_listener_moderator_image = itemView.findViewById(R.id.speaker_listener_moderator_image);
            voice_action = itemView.findViewById(R.id.voice_action);
            speaker_listener_moderator_name = itemView.findViewById(R.id.speaker_listener_moderator_name);
            User user = User.getLoggedInUser();
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            User user = User.getLoggedInUser();
            if (is_admin_current_user && !user.UserID.equals(mData.get(getAdapterPosition()).UserId)) {
                actions_for_speakers(itemView.getContext(), mData.get(getAdapterPosition()).UserId, mData.get(getAdapterPosition()).IsSpeaker);
            }
        }
    }

    // convenience method for getting data at click position
    UsersInRoom getItem(int id) {
        return mData.get(id);
    }

    // allows clicks events to be caught
    public void setClickListener(RoomsUsersDisplayListAdapter.ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(Integer uid, String action);
    }

    private void actions_for_speakers(Context context, Integer uid, Boolean is_speaker) {
        String txt = is_speaker ? "Move to Audience": "Invite to Speak";
        final CharSequence[] items = { txt };
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (context == null) {
                    return;
                }


                Log.d("debug_audio", items[item].toString());
                if (items[item].equals("Move to Audience")) {
                    mClickListener.onItemClick(uid, "MAKE_AUDIENCE");
                } else if (items[item].equals("Cancel")) {
                    dialog.dismiss();
                } else if (items[item].equals("Invite to Speak")) {
                    mClickListener.onItemClick(uid, "MAKE_SPEAKER");
                }
            }
        });
        builder.show();
    }

    private void fetch_listeners() {

    }
}