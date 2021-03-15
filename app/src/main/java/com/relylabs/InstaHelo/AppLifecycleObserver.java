package com.relylabs.InstaHelo;

import android.util.Log;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.relylabs.InstaHelo.Utils.Helper;
import com.relylabs.InstaHelo.models.SystemProperties;

public class AppLifecycleObserver implements LifecycleObserver {

    public static final String TAG = AppLifecycleObserver.class.getName();

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onEnterForeground() {
        //run the code we need
        Log.d("debug_data", "App in foreground");
        SystemProperties.updateState(true);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onEnterBackground() {
        //run the code we need
        Log.d("debug_data", "App in background");
        SystemProperties.updateState(false);
    }
}