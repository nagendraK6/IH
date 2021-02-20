package com.relylabs.InstaHelo.models;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import  com.activeandroid.annotation.Table;
import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;
import com.activeandroid.query.Update;

import java.util.ArrayList;
import java.util.List;


@Table(name = "UsersInRoom")
public class UsersInRoom extends Model {
    @Column(name = "UserId", unique = true, onUniqueConflict = Column.ConflictAction.REPLACE)
    public Integer UserId;

    @Column(name = "Name")
    public String Name;

    @Column(name = "profileImageURL")
    public String profileImageURL;

    @Column(name = "IsdataFetchRequired")
    public Boolean IsdataFetchRequired;


    @Column(name = "IsMuted")
    public Boolean IsMuted;

    @Column(name = "IsSpeaker")
    public Boolean IsSpeaker;

    public UsersInRoom() {
        super();
    }

    public UsersInRoom(Boolean IsSpearker, Boolean IsMuted, Integer UserId) {
        this.IsSpeaker = IsSpearker;
        this.UserId = UserId;
        this.IsMuted = IsMuted;
        this.IsdataFetchRequired = true;
        this.Name = "";
        this.profileImageURL = "";
    }

    public UsersInRoom(Boolean IsSpearker, Boolean IsMuted, Integer UserId, String name, String profileImageURL) {
        this.IsSpeaker = IsSpearker;
        this.UserId = UserId;
        this.IsMuted = IsMuted;
        this.IsdataFetchRequired = false;
        this.Name = name;
        this.profileImageURL = profileImageURL;
    }



    public static ArrayList<UsersInRoom> getAllSpeakers() {
        List<UsersInRoom> all_users = new Select().from(UsersInRoom.class).execute();
        return new ArrayList<UsersInRoom>(all_users);
    }

    public static void deleteAllRecords() {
        new Delete().from(UsersInRoom.class).execute();
    }

    public static void changeMuteState(Integer uid, Boolean mute) {
        UsersInRoom u = getRecords(uid);
        u.IsMuted = mute;
        u.save();
    }

    public static UsersInRoom getRecords(Integer uid) {
        return (UsersInRoom) new Select().from(UsersInRoom.class)
                .where("UserId = ?",uid).executeSingle();
    }
}