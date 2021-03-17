package com.relylabs.InstaHelo.rooms;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.relylabs.InstaHelo.App;
import com.relylabs.InstaHelo.NewsFeedAdapter;
import com.relylabs.InstaHelo.R;
import com.relylabs.InstaHelo.Utils.Helper;
import com.relylabs.InstaHelo.bottomsheet.BottomScheduleRoom;
import com.relylabs.InstaHelo.bottomsheet.SpeakerAdapter;
import com.relylabs.InstaHelo.followerList.FollowerListAdapter;
import com.relylabs.InstaHelo.followerList.FollowingList;
import com.relylabs.InstaHelo.models.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Iterator;

import cz.msebera.android.httpclient.Header;

import static com.relylabs.InstaHelo.Utils.Helper.removefragment;

public class AddCoHostDialog extends Fragment implements CoHostListAdapter.ItemClickListener {

    FragmentActivity activity_ref;
    public static final String TAG = "ActionBottomDialog";
    String query_txt = "";
    ProgressBar show_busy_indicator;
    RecyclerView recyclerView;
    View fragment_view;
    private  ArrayList<String> names = new ArrayList<String>();
    private  ArrayList<String> usernames = new ArrayList<String>();
    private  ArrayList<String> bio = new ArrayList<String>();
    private  ArrayList<String> imgs = new ArrayList<String>();
    private  ArrayList<String> main_names = new ArrayList<String>();
    private  ArrayList<String> main_usernames = new ArrayList<String>();
    private  ArrayList<String> main_bio = new ArrayList<String>();
    private  ArrayList<String> main_imgs = new ArrayList<String>();
    private  ArrayList<String> user_ids_selected = new ArrayList<>();
    private  ArrayList<String> names_selected = new ArrayList<>();
    private  ArrayList<String> imgs_selected = new ArrayList<>();
    private  ArrayList<Boolean > isChecked = new ArrayList<>();
    private  ArrayList<String> already_checked;
    CoHostListAdapter adapter;
    private  ArrayList<String> user_ids = new ArrayList<>();
    private  ArrayList<String> main_user_ids = new ArrayList<>();

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Activity){
            activity_ref=(FragmentActivity) context;
        }
    }




    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_co_host_dialog, container, false);
    }
    public static AddCoHostDialog newInstance() {
        return new AddCoHostDialog();
    }
    //@Override
   // public int getTheme() {
        //return R.style.AppBottomSheetDialogTheme;
    //}

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        already_checked = getArguments().getStringArrayList("user_ids_selected");
        Log.d("already_checked",already_checked.toString());
        TextView lets_go = view.findViewById(R.id.lets_go);
        lets_go.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendResult();
            }
        });
        SearchView search = (SearchView) view.findViewById(R.id.search_people);
        show_busy_indicator = view.findViewById(R.id.show_busy_indicator);
        fragment_view = view;
        recyclerView = view.findViewById(R.id.users_in_profile_list);
        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                search.setIconified(false);
            }
        });
        search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                query_txt = newText;
                user_ids.clear();
                bio.clear();
                imgs.clear();
                usernames.clear();
                names.clear();
                if(query_txt.equals("")){

                    user_ids.addAll(main_user_ids);
                    usernames.addAll(main_usernames);
                    imgs.addAll(main_imgs);
                    names.addAll(main_names);
                    bio.addAll(main_bio);
                }
                else{
                    for(int i=0;i<main_user_ids.size();i++){
                        if(main_names.get(i).toLowerCase().startsWith(newText.toLowerCase()) || main_usernames.get(i).toLowerCase().startsWith(newText.toLowerCase())){
                            user_ids.add(main_user_ids.get(i));
                            names.add(main_names.get(i));
                            usernames.add(main_usernames.get(i));
                            imgs.add(main_imgs.get(i));
                            bio.add(main_bio.get(i));
                        }
                    }
                }
                adapter.notifyDataSetChanged();
//                loadNextDataFromApi(10, true);
                return false;
            }
        });

        ImageView cross = view.findViewById(R.id.cross);
        cross.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendResult();
            }
        });
        getFollowing();
        prepareRecyclerView();
    }
    public void getFollowing(){
        show_busy_indicator();
        final User user = User.getLoggedInUser();
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        int user_id = user.UserID;
        params.add("user_id",String.valueOf(user_id));
        Log.d("inside_get","inside get followuing");
        JsonHttpResponseHandler jrep = new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    hide_busy_indicator();
                    Log.d("/followers",response.toString());
                    JSONArray name = response.getJSONArray("names");
                    JSONArray username = response.getJSONArray("usernames");
                    JSONArray bio_temp = response.getJSONArray("bio");
                    JSONArray imgs_temp = response.getJSONArray(("img"));
                    JSONArray ids = response.getJSONArray("user_ids");
                    if(name!=null){
                        for (int i=0;i<name.length();i++){
                            names.add(name.getString(i));
                            user_ids.add(String.valueOf(ids.getInt(i)));
                            if(already_checked.contains(String.valueOf(ids.getInt(i)))){
                                isChecked.add(true);
                                user_ids_selected.add(String.valueOf(ids.getInt(i)));
                                imgs_selected.add(imgs_temp.getString(i));
                                names_selected.add(name.getString(i));
                            }
                            else{
                                isChecked.add(false);
                            }

                        }
                    }
                    if(username!=null){
                        for (int i=0;i<username.length();i++){
                            usernames.add(username.getString(i));
                        }
                    }
                    if(bio_temp!=null){
                        for(int i=0;i<bio_temp.length();i++){
                            bio.add(bio_temp.getString(i));
                        }
                    }
                    if(imgs_temp!=null){
                        for(int i=0;i<imgs_temp.length();i++){
                            imgs.add(imgs_temp.getString(i));
                        }
                    }
                    main_names.addAll(names);
                    main_usernames.addAll(usernames);
                    main_bio.addAll(bio);
                    main_imgs.addAll(imgs);
                    main_user_ids.addAll(user_ids);
                    adapter.notifyDataSetChanged();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String res, Throwable t) {

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject obj) {
            }
        };

        client.addHeader("Accept", "application/json");
        client.addHeader("Authorization", "Token " + user.AccessToken);
        client.post(App.getBaseURL() + "registration/followers_list_common", params, jrep);
    }
    void show_busy_indicator() {
        show_busy_indicator.setVisibility(View.VISIBLE);
    }

    void hide_busy_indicator() {
        show_busy_indicator.setVisibility(View.INVISIBLE);
    }
    void prepareRecyclerView() {
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 1));
        adapter = new CoHostListAdapter(getContext(), names, usernames,bio, imgs,user_ids,isChecked,main_user_ids);
        adapter.setClickListener(this);
        recyclerView.setAdapter(adapter);

    }
    @Override
    public void onDetach() {
        super.onDetach();
    }

    private void sendResult() {
        if( getTargetFragment() == null ) {
            return;
        }
        Helper.hideKeyboard(activity_ref);
        Intent intent = ScheduleForLater.newIntent(user_ids_selected,names_selected,imgs_selected);
        getTargetFragment().onActivityResult(getTargetRequestCode(), Activity.RESULT_OK, intent);
        Helper.removefragment(activity_ref);
    }

    @Override
    public void onItemClick(int position) {

        String id = user_ids.get(position);
        String name = names.get(position);
        String im = imgs.get(position);
        int pos = main_user_ids.indexOf(id);
        Boolean bool = isChecked.get(pos);
        if(bool){
            Iterator itr = user_ids_selected.iterator();
            while (itr.hasNext())
            {
                String data = (String)itr.next();
                if (data.equals(id))
                    itr.remove();
            }
            Iterator itr2 = names_selected.iterator();
            while (itr2.hasNext())
            {
                String data = (String)itr2.next();
                if (data.equals(name))
                    itr2.remove();
            }
            Iterator itr3 = imgs_selected.iterator();
            while (itr3.hasNext())
            {
                String data = (String)itr3.next();
                if (data.equals(im))
                    itr3.remove();
            }
        }
        else{
            user_ids_selected.add(id);
            names_selected.add(name);
            imgs_selected.add(im);
        }
        isChecked.set(pos,bool);
        Log.d("clicked",user_ids_selected.toString());
        Log.d("clicked",isChecked.toString());
    }


}