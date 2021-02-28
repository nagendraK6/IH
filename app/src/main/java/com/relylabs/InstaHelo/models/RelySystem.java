package com.relylabs.InstaHelo.models;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import  com.activeandroid.annotation.Table;
import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;


@Table(name = "RelySystem")
public class RelySystem extends Model {
    @Column(name = "is_foreground")
    public Boolean is_foreground;

    @Column(name = "timestamp_updated")
    public Long timestamp_updated;

    public RelySystem() {
        super();
        is_foreground = true;
        timestamp_updated = System.currentTimeMillis()/1000;
    }

    public static void updateState(boolean is_foreground) {
        RelySystem rs = getSystemSettings();
        if (rs == null) {
            rs = new RelySystem();
        }

        rs.is_foreground = is_foreground;
        rs.timestamp_updated = System.currentTimeMillis()/1000;
        rs.save();
    }

    public static RelySystem getSystemSettings() {
        return (RelySystem) new Select().from(RelySystem.class) .executeSingle();
    }

    public static void deleteAll() {
        new Delete().from(RelySystem.class).execute();
    }
}