package com.relylabs.InstaHelo.models;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;

import java.util.ArrayList;

@Table(name = "EventElement")
public class EventElement  extends Model {

    @Column(name = "eventID", unique = true, onUniqueConflict = Column.ConflictAction.REPLACE)
    public Integer eventID;

    @Column(name = "eventTitle")
    public String eventTitle;


    public EventElement() {
        super();
        this.eventID = -1;
    }

    public EventElement(
            Integer eventID,
            String eventTitle
    ) {
        super();
        this.eventID = eventID;
        this.eventTitle = eventTitle;
    }


    public String getEventTitle() {
        return eventTitle;
    }
}