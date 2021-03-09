package com.relylabs.InstaHelo.models;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

import java.util.ArrayList;



public class EventElement {

    public Integer eventID;

    public String eventTitle;

    public String eventChannelName;

    public ArrayList<String> eventPhotoUrls;

    public ArrayList<EventCardUserElement> userElements;

    public String roomSlug;

    public long scheduleTimestamp;

    public Boolean isScheduled;

    public Boolean hasStarted;

    public Boolean isRoomAdmin;

    public EventElement() {
        this.eventID = -1;
        this.eventTitle = "";
        this.eventChannelName = "";
        this.roomSlug = "";
        this.scheduleTimestamp = 0;
        this.isScheduled = Boolean.FALSE;
        this.hasStarted = Boolean.TRUE;
        this.isRoomAdmin = false;
    }
}