package com.relylabs.InstaHelo.models;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import  com.activeandroid.annotation.Table;
import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;

// Deprecared
@Table(name = "SystemProperties")
public class SystemProperties extends Model {
    @Column(name = "UserID", unique = true, onUniqueConflict = Column.ConflictAction.REPLACE)
    public Integer UserID;

    @Column(name = "is_foreground")
    public Boolean is_foreground;

    @Column(name = "timestamp_updated")
    public Long timestamp_updated;


    @Column(name = "last_contact_cursor_uploaded")
    public Integer last_contact_cursor_uploaded;

    public SystemProperties() {
        super();
        is_foreground = true;
        timestamp_updated = System.currentTimeMillis()/1000;
        last_contact_cursor_uploaded = 0;
    }

    public static void updateState(boolean is_foreground) {
        Integer uid = User.getLoggedInUserID();
        if (uid == null) {
            return;
        }

        SystemProperties rs = getSystemSettings();
        if (rs == null) {
            rs = new SystemProperties();
        }

         rs.is_foreground = is_foreground;
         rs.timestamp_updated = System.currentTimeMillis()/1000;
         rs.save();
    }

    public static SystemProperties getSystemSettings() {
        return (SystemProperties) new Select().from(SystemProperties.class) .executeSingle();
    }

    public static void deleteAll() {
        new Delete().from(SystemProperties.class).execute();
    }
}