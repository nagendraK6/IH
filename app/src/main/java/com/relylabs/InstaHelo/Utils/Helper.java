package com.relylabs.InstaHelo.Utils;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;


public class Helper {
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
}
