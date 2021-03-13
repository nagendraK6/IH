package com.relylabs.InstaHelo.rooms;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.TimePicker;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.shape.CornerFamily;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.relylabs.InstaHelo.App;
import com.relylabs.InstaHelo.R;
import com.relylabs.InstaHelo.RoomDisplayFragment;
import com.relylabs.InstaHelo.Utils.Helper;
import com.relylabs.InstaHelo.Utils.RoomHelper;
import com.relylabs.InstaHelo.models.User;
import com.relylabs.InstaHelo.models.UserSettings;
import com.relylabs.InstaHelo.services.ActiveRoomService;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import cz.msebera.android.httpclient.Header;

import static com.relylabs.InstaHelo.Utils.Helper.hideKeyboard;
import static com.relylabs.InstaHelo.Utils.Helper.removefragment;


public class ScheduleForLater extends Fragment {
    private FragmentActivity activity_ref;
    final Calendar myCalendar = Calendar.getInstance();
    String room_type = "social";
    ImageView schedule_event;
    ProgressBar busy_indicator;

    int day_data,month_data,year_data,hour_data,minutes_data;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof Activity){
            activity_ref=(FragmentActivity) context;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_schedule_for_later, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        busy_indicator = view.findViewById(R.id.busy_indicator);
        final User user = User.getLoggedInUser();
        String prof_url = user.ProfilePicURL;
        String title = getArguments().getString("title");
        String type = getArguments().getString("type");
        ShapeableImageView prof = view.findViewById(R.id.profile_img_noti2);
        if(!prof_url.equals("")){
            float radius = activity_ref.getResources().getDimension(R.dimen.default_corner_news_feed_image_bottom);
            prof.setShapeAppearanceModel(prof.getShapeAppearanceModel()
                    .toBuilder()
                    .setTopRightCorner(CornerFamily.ROUNDED,radius)
                    .setTopLeftCorner(CornerFamily.ROUNDED,radius)
                    .setBottomLeftCorner(CornerFamily.ROUNDED,radius)
                    .setBottomRightCorner(CornerFamily.ROUNDED,radius)
                    .build());
            Picasso.get().load(prof_url).into(prof);
        }
        TextView name = view.findViewById(R.id.name_user_curr);
        name.setText(user.FirstName + " " + user.LastName);
        room_type = type;
        if(room_type.equals("public")){
            TextView helper = view.findViewById(R.id.helperText2);
            helper.setText("Only the followers of speakers can join");
            RadioButton r = view.findViewById(R.id.public_room);
            r.setChecked(true);
        }
        TextView topic = view.findViewById(R.id.topic);
        topic.setText(title);
        EditText edittext= (EditText) view.findViewById(R.id.date_pick);
        updateLabelDate(edittext);
        EditText edittexttime = view.findViewById(R.id.time_pick);
        updateLabelTime(edittexttime);
        RadioGroup room_selection = view.findViewById(R.id.room_choice);
        TextView helper = view.findViewById(R.id.helperText2);
        TextView cancel = view.findViewById(R.id.cancel2);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Helper.removeFragmentWithTopAnim(activity_ref);
                Bundle data_bundle = new Bundle();
                data_bundle.putString("user_action", "REMOVE_FRAGMENT");
                Intent intent = new Intent("update_from_schedule");
                intent.putExtras(data_bundle);
                if (activity_ref != null) {
                    activity_ref.sendBroadcast(intent);
                }
            }
        });
        schedule_event = view.findViewById(R.id.schedule_event);
        schedule_event.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                schedule_event.setVisibility(View.INVISIBLE);
                send_process_room_create(title, room_type,myCalendar.getTimeInMillis());
            }
        });
        room_selection.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch(checkedId) {
                    case R.id.social_room:
                        room_type = "social";
                        helper.setText("Only the followers of speakers can join");
                        break;
                    case R.id.public_room:
                        room_type = "public";
                        helper.setText("Anyone can join");
                        break;
                }
            }
        });
        DatePickerDialog.OnDateSetListener date = new DatePickerDialog.OnDateSetListener() {

            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear,
                                  int dayOfMonth) {
                myCalendar.set(Calendar.YEAR, year);
                myCalendar.set(Calendar.MONTH, monthOfYear);
                myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                year_data = year;
                month_data = monthOfYear + 1;
                day_data = dayOfMonth;
                updateLabelDate(edittext);
                Log.d("year_date",String.valueOf(year) + String.valueOf(monthOfYear) + String.valueOf(dayOfMonth));
            }
        };

        edittext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                new DatePickerDialog(getContext(), date, myCalendar
                        .get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                        myCalendar.get(Calendar.DAY_OF_MONTH)).show();
            }
        });

        TextView back = view.findViewById(R.id.back_room);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Helper.removeFragmentWithTopAnim(activity_ref);
            }
        });

        EditText time = view.findViewById(R.id.time_pick);
        time.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                Calendar mcurrentTime = Calendar.getInstance();
                int hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
                int minute = mcurrentTime.get(Calendar.MINUTE);
                TimePickerDialog mTimePicker;
                mTimePicker = new TimePickerDialog(getContext(), new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
                        hour_data = selectedHour;
                        minutes_data = selectedMinute;
                        Log.d("time",String.valueOf(selectedHour) + " " + String.valueOf(selectedMinute));
                        myCalendar.set(Calendar.HOUR_OF_DAY,selectedHour);
                        myCalendar.set(Calendar.MINUTE, selectedMinute);
                        updateLabelTime(edittexttime);
                    }
                }, hour, minute, false);//Yes 24 hour time
                mTimePicker.setTitle("Select Time");
                mTimePicker.show();

            }
        });
    }
    private void updateLabelDate(EditText edittext){
        String myFormat = "MMMM dd, yyyy"; //In which you need put here
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat);

        edittext.setText(sdf.format(myCalendar.getTime()));
    }

    private void updateLabelTime(EditText editText){
        String myFormat = "hh:mm a"; //In which you need put here
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat);

        editText.setText(sdf.format(myCalendar.getTime()).toUpperCase());
//        Log.d("timestamp",String.valueOf(myCalendar.getTimeInMillis()));
    }

    private  void broadcastLocalUpdate(String action, String room_type,long timestamp) {
        String title = getArguments().getString("title");
        Bundle data_bundle = new Bundle();
        data_bundle.putString("user_action", action);
        data_bundle.putString("room_title", title);
        data_bundle.putString("room_type", room_type);
        data_bundle.putLong("timestamp",timestamp);
        Intent intent = new Intent("update_from_room");
        intent.putExtras(data_bundle);
        if (activity_ref != null) {
            activity_ref.sendBroadcast(intent);
        }
    }

    private  void show_busy_indicator() {
        busy_indicator.setVisibility(View.VISIBLE);
    }

    private  void hide_busy_indicator() {
        busy_indicator.setVisibility(View.INVISIBLE);
    }


    public  void send_process_room_create(String title, String room_type,long timestamp) {
        final User user = User.getLoggedInUser();
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        params.add("event_title", title);
        params.add("room_type", room_type);
        params.add("schedule_timestamp", String.valueOf(timestamp));
        show_busy_indicator();

        JsonHttpResponseHandler jrep = new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                /* processExit(); */
                Integer event_id = null;
                try {
                    String room_slug = response.getString("room_slug");
                    ScheduleRoom room = new ScheduleRoom();
                    Bundle args = new Bundle();
                    args.putString("room_slug", room_slug);
                    args.putBoolean("has_just_created", Boolean.TRUE);
                    room.setArguments(args);
                    FragmentTransaction ft = activity_ref.getSupportFragmentManager().beginTransaction();
                    ft.add(R.id.fragment_holder, room);
                    ft.commitAllowingStateLoss();
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                hide_busy_indicator();
                schedule_event.setVisibility(View.VISIBLE);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String res, Throwable t) {
                hide_busy_indicator();
                schedule_event.setVisibility(View.VISIBLE);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable t, JSONObject obj) {
                hide_busy_indicator();
                schedule_event.setVisibility(View.VISIBLE);
            }
        };

        client.addHeader("Accept", "application/json");
        client.addHeader("Authorization", "Token " + user.AccessToken);
        client.post(App.getBaseURL() + "page/create_a_room", params, jrep);
    }
}