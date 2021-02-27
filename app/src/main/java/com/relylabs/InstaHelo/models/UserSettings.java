package com.relylabs.InstaHelo.models;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import  com.activeandroid.annotation.Table;
import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;


@Table(name = "UserSettings")
public class UserSettings extends Model {

    @Column(name = "UserID", unique = true, onUniqueConflict = Column.ConflictAction.REPLACE)
    public Integer UserID;


    @Column(name = "selected_event_id")
    public Integer selected_event_id;


    @Column(name = "selected_channel_name")
    public String selected_channel_name;


    @Column(name = "selected_channel_display_name")
    public String selected_channel_display_name;

    @Column(name = "is_bottom_sheet_visible")
    public Boolean is_bottom_sheet_visible;


    @Column(name = "is_current_role_speaker")
    public Boolean is_current_role_speaker;


    @Column(name = "is_current_user_admin")
    public Boolean is_current_user_admin;

    @Column(name = "is_muted")
    public Boolean is_muted;

    @Column(name = "is_self_hand_raised")
    public Boolean is_self_hand_raised;


    @Column(name = "audience_hand_raised")
    public Boolean audience_hand_raised;

    public UserSettings() {
        super();
        User u = User.getLoggedInUser();
        this.UserID = u.UserID;
        this.is_muted = false;
        this.is_bottom_sheet_visible = false;
        this.selected_channel_name = "";
        this.is_self_hand_raised = false;
        this.selected_channel_display_name = "";
        this.selected_event_id = -1;
        this.is_current_role_speaker = false;
        this.is_current_user_admin = false;
        this.audience_hand_raised = false;
    }

    public static UserSettings getSettings() {
        return (UserSettings) new Select().from(UserSettings.class) .executeSingle();
    }

    public static void deleteAll() {
        new Delete().from(UserSettings.class).execute();
    }
}