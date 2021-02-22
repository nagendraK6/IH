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
}