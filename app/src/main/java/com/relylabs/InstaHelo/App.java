package com.relylabs.InstaHelo;

import android.app.Application;

import com.activeandroid.ActiveAndroid;
import com.facebook.stetho.Stetho;


public class App extends Application {



    @Override public void onCreate() {
        super.onCreate();
        Stetho.initializeWithDefaults(this);
        ActiveAndroid.initialize(this);
    }

    public static String getBaseURL() {
        return "https://www.instahelo.com/";
    }
}