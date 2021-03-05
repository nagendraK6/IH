package com.relylabs.InstaHelo.Utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.telephony.TelephonyManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieSyncManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.relylabs.InstaHelo.MainScreenFragment;
import com.relylabs.InstaHelo.R;
import com.relylabs.InstaHelo.models.User;
import com.relylabs.InstaHelo.onboarding.AddBioDetailsFragment;
import com.relylabs.InstaHelo.onboarding.ContactRequestFragment;
import com.relylabs.InstaHelo.onboarding.DisplayUserNameAskFragment;
import com.relylabs.InstaHelo.onboarding.FragmentNonInvitedThankYouScreen;
import com.relylabs.InstaHelo.onboarding.FriendsToFollow;
import com.relylabs.InstaHelo.onboarding.InvitedUserNameAskFragment;
import com.relylabs.InstaHelo.onboarding.LoginFragment;
import com.relylabs.InstaHelo.onboarding.PhoneVerificationFragment;
import com.relylabs.InstaHelo.onboarding.PhotoAskFragment;
import com.relylabs.InstaHelo.onboarding.SuggestedProfileToFollowFragment;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;

import static com.activeandroid.Cache.getContext;


public class Helper {
    private static CookieSyncManager PhoneNumberUtil;

    public static void setTags(TextView pTextView, String pTagString) {
        SpannableString string = new SpannableString(pTagString);

        int start = -1;
        for (int i = 0; i < pTagString.length(); i++) {
            if (pTagString.charAt(i) == '#') {
                start = i;
            } else if (pTagString.charAt(i) == ' ' || (i == pTagString.length() - 1 && start != -1)) {
                if (start != -1) {
                    if (i == pTagString.length() - 1) {
                        i++; // case for if hash is last word and there is no
                        // space after word
                    }

                    final String tag = pTagString.substring(start, i);
                    string.setSpan(new ClickableSpan() {

                        @Override
                        public void onClick(View widget) {
                            Log.d("Hash", String.format("Clicked %s!", tag));
                        }

                        @Override
                        public void updateDrawState(TextPaint ds) {
                            // link color
                            ds.setColor(Color.parseColor("#003569"));
                            ds.setUnderlineText(false);
                        }
                    }, start, i, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    start = -1;
                }
            }
        }

        pTextView.setMovementMethod(LinkMovementMethod.getInstance());
        pTextView.setText(string);
    }

    public static ArrayList<String> getAllShownImagesPath(String directoryName, Context context) {
        Uri uri;
        Cursor cursor;

        int column_index_data, column_index_folder_name;

        ArrayList<String> listOfAllImages = new ArrayList<String>();
        String absolutePathOfImage = null;
        uri = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        String[] projection = { MediaStore.MediaColumns.DATA,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME };

        String orderBy = MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC";

        cursor = context.getContentResolver().query(uri, projection, null,
                null, orderBy);

        column_index_data = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
        if (StringUtils.isEmpty(directoryName)) {
            while (cursor.moveToNext()) {
                absolutePathOfImage = cursor.getString(column_index_data);
                listOfAllImages.add(absolutePathOfImage);
            }
        } else {
            while (cursor.moveToNext()) {
                absolutePathOfImage = cursor.getString(column_index_data);
                File f = new File((absolutePathOfImage));
                String dirName =f.getParentFile().getName();
                if (dirName.equals(directoryName)) {
                    listOfAllImages.add(absolutePathOfImage);
                }
            }
        }

        return listOfAllImages;
    }

    public static  ArrayList<String> getDirectoryNames(Context context) {
        ArrayList<String> names = new ArrayList<>();
        String[] projection = new String[] {"DISTINCT " + MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME};
        Cursor cur = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, null, null);
        StringBuffer list = new StringBuffer();
        while (cur.moveToNext()) {
            names.add(cur.getString((cur.getColumnIndex(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME))));
        }

        return names;
    }

    public static void nextScreen(FragmentActivity activity_ref){
        FragmentTransaction ft = activity_ref.getSupportFragmentManager().beginTransaction();
        Boolean isInvited = false;
        final User user = User.getLoggedInUser();
        String currentStep = "";
        if(user==null){
            currentStep = "GET_STARTED";
            isInvited = false;
        }
        else {
            currentStep =  user.UserSteps;
            isInvited = user.IsInvited;
        }
        Log.d("currentStep",currentStep);
        if(currentStep.equals("GET_STARTED")){
            ft.replace(R.id.fragment_holder, new LoginFragment());
            ft.commitAllowingStateLoss();
        }
        else if(currentStep.equals("LOGIN")){
            ft.replace(R.id.fragment_holder, new PhoneVerificationFragment());
            ft.commitAllowingStateLoss();
        }
        else if(currentStep.equals("VERIFY_OTP")){
            ft.replace(R.id.fragment_holder, new DisplayUserNameAskFragment());
            ft.commitAllowingStateLoss();
        }
        else if(currentStep.equals("RESERVE_DISPLAY_USER_NAME") && !isInvited){
            ft.replace(R.id.fragment_holder, new FragmentNonInvitedThankYouScreen());
            ft.commitAllowingStateLoss();
        }
        else if(currentStep.equals("THANK_YOU_SCREEN") && isInvited){
            ft.replace(R.id.fragment_holder, new InvitedUserNameAskFragment());
            ft.commitAllowingStateLoss();
        }
        else if(currentStep.equals("RESERVE_DISPLAY_USER_NAME") && isInvited){
            ft.replace(R.id.fragment_holder, new InvitedUserNameAskFragment());
            ft.commitAllowingStateLoss();
        }
        else if(currentStep.equals("INVITED_NAME_ASK")){
            ft.replace(R.id.fragment_holder, new PhotoAskFragment());
            ft.commitAllowingStateLoss();
        }
        else if(currentStep.equals("PHOTO_ASK")){
            ft.replace(R.id.fragment_holder, new AddBioDetailsFragment());
            ft.commitAllowingStateLoss();
        }
        else if(currentStep.equals("ADD_BIO")){
            ft.replace(R.id.fragment_holder, new ContactRequestFragment());
            ft.commitAllowingStateLoss();
        }
        else if(currentStep.equals("CONTACT_REQUEST")){
            ft.replace(R.id.fragment_holder, new SuggestedProfileToFollowFragment());
            ft.commitAllowingStateLoss();
        }
        else if(currentStep.equals("FRIENDS_TO_FOLLOW")){
            ft.replace(R.id.fragment_holder, new FriendsToFollow());
            ft.commitAllowingStateLoss();
        }
        else if(currentStep.equals("MAIN_SCREEN")){
            ft.replace(R.id.fragment_holder, new MainScreenFragment());
            ft.commitAllowingStateLoss();
        }
    }
    public static void prevScreen(FragmentActivity activity_ref){
        FragmentTransaction ft = activity_ref.getSupportFragmentManager().beginTransaction();
        final User user = User.getLoggedInUser();
        String currentStep = "";
        if(user==null){
            currentStep = "GET_STARTED";
        }
        else {
            currentStep =  user.UserSteps;
        }
        Log.d("prev_state",currentStep);
        if(currentStep.equals("LOGIN")){
            ft.replace(R.id.fragment_holder, new LoginFragment());
            ft.commitAllowingStateLoss();
        }

    }
    public static void skipScreen(FragmentActivity activity_ref){
        FragmentTransaction ft = activity_ref.getSupportFragmentManager().beginTransaction();
        final User user = User.getLoggedInUser();
        String currentStep = "";
        if(user==null){
            currentStep = "GET_STARTED";
        }
        else {
            currentStep =  user.UserSteps;
        }
        if(currentStep.equals("PHOTO_ASK")){
            ft.replace(R.id.fragment_holder, new AddBioDetailsFragment());
            ft.commitAllowingStateLoss();
        }
        else if(currentStep.equals("CONTACT_REQUEST")){
            ft.replace(R.id.fragment_holder, new SuggestedProfileToFollowFragment());
            ft.commitAllowingStateLoss();
        }
    }

    public static String getCountryDialCode(Context context){
        String contryId = null;
        String contryDialCode = null;
        TelephonyManager telephonyMngr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        contryId = telephonyMngr.getSimCountryIso().toUpperCase();
        String[] arrContryCode=context.getResources().getStringArray(R.array.DialingCountryCode);
        for(int i=0; i<arrContryCode.length; i++){
            String[] arrDial = arrContryCode[i].split(",");
            if(arrDial[1].trim().equals(contryId.trim())){
                contryDialCode = arrDial[0];
                break;
            }
        }
        return contryDialCode;
    }
    public static String cleanPhoneNo(String phone,Context context){
        String refined = phone.replaceAll("[^0-9+]", "");
        if(refined.length()<10){
            return "ERROR";
        }
        if(refined.substring(0, 1).equals("+")){
            return refined;
        }
        else{
            refined = StringUtils.stripStart(refined, "0");
            if(refined.length()<10){
                return "ERROR";
            }
            else{
                String country_code = getCountryDialCode(context);
                refined = "+"  +country_code + refined;
                if(refined.length()<10){
                    return "ERROR";
                }
                else{
                    return refined;
                }
            }
        }
    }

    public static void sendRequestForContactProcess(FragmentActivity activity) {
        Bundle data_bundle = new Bundle();
        data_bundle.putString("user_action", "contact_update");
        Intent intent = new Intent("update_to_main_activity");
        intent.putExtras(data_bundle);
        if (activity != null) {
            activity.sendBroadcast(intent);
        }
    }

    public static void hideKeyboard(Context mContext) {
        InputMethodManager imm = (InputMethodManager) mContext
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        View focus_view = ((Activity) mContext).getWindow()
                .getCurrentFocus();

        if (focus_view != null) {
            imm.hideSoftInputFromWindow(focus_view.getWindowToken(), 0);
        }
    }

    public static void removefragment(FragmentActivity activity) {
        if(activity!=null) {
            Fragment f = activity.getSupportFragmentManager().findFragmentById(R.id.fragment_holder);
            FragmentManager manager = activity.getSupportFragmentManager();
            FragmentTransaction trans = manager.beginTransaction();
            trans.remove(f);
            trans.commitAllowingStateLoss();
            manager.popBackStack();
        }
    }
    public static void loadFragmentAdapter(Fragment fragment_to_start,View view) {
        AppCompatActivity activity = (AppCompatActivity) view.getContext();
        FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
        ft.add(R.id.fragment_holder, fragment_to_start);
        ft.commitAllowingStateLoss();
    }

    public static void loadFragment(Fragment fragment_to_start, FragmentActivity activity_ref) {
        if(activity_ref!=null) {
            FragmentTransaction ft = activity_ref.getSupportFragmentManager().beginTransaction();
            ft.add(R.id.fragment_holder, fragment_to_start);
            ft.commitAllowingStateLoss();
        }
    }

    public static void replaceFragment(Fragment fragment_to_start, FragmentActivity activity_ref) {
        if(activity_ref!=null) {
            FragmentTransaction ft = activity_ref.getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.fragment_holder, fragment_to_start);
            ft.commitAllowingStateLoss();
        }
    }
}
