package com.relylabs.InstaHelo.models;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import  com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;


@Table(name = "Users")
public class User extends Model {

    @Column(name = "UserID", unique = true, onUniqueConflict = Column.ConflictAction.REPLACE)
    public Integer UserID;

    @Column(name = "FirstName")
    public String FirstName;

    @Column(name = "LastName")
    public String LastName;

    @Column(name = "Username")
    public String Username;


    @Column(name = "ProfilePicURL")
    public String ProfilePicURL;

    @Column(name = "CompletedOnboarding")
    public Boolean CompletedOnboarding;


    @Column(name = "InviterName")
    public String InviterName;


    @Column(name = "InvitesCount")
    public Integer InvitesCount;

    @Column(name = "InviterImageURL")
    public String InviterImageURL;


    @Column(name = "IsLoggedInUser")
    public Boolean IsLoggedInUser;

    @Column(name = "IsInvited")
    public Boolean IsInvited;

    @Column(name = "PhoneNo")
    public String PhoneNo;

    @Column(name = "IsOTPVerified")
    public Boolean IsOTPVerified;

    @Column(name = "CountryCode")
    public String CountryCode;

    @Column(name = "AccessToken")
    public String AccessToken;

    @Column(name = "BioDescription")
    public String BioDescription;

    @Column(name = "UserSteps")
    public String UserSteps;

    @Column(name = "ShowWelcomeScreen")
    public Boolean ShowWelcomeScreen;


    @Column(name = "IsStartRoomEnabled")
    public Boolean IsStartRoomEnabled;

    @Column(name = "IsSuperUser")
    public Boolean IsSuperUser;

    @Column(name = "InvitedUsersCount")
    public Integer InvitedUsersCount;

    @Column(name = "SendInvitesToAllUsers")
    public Boolean SendInvitesToAllUsers;


    public User() {
        super();
        this.IsOTPVerified = false;
        this.IsLoggedInUser = false;
        this.FirstName = "";
        this.LastName = "";
        this.ProfilePicURL = "";
        this.BioDescription = "";
        this.CompletedOnboarding = false;
        this.InvitesCount = 0;
        this.IsStartRoomEnabled = false;
        this.IsSuperUser = false;
        this.InvitedUsersCount = 0;
        this.SendInvitesToAllUsers = false;
    }


    public static User getRandom() {
        return new Select().from(User.class).orderBy("RANDOM()").executeSingle();
    }

    public static User getLoggedInUser() {
        User user = new Select().from(User.class).executeSingle();
        return user;
    }

    public static Integer getLoggedInUserID() {
        User current_user = User.getLoggedInUser();
        if (current_user == null) {
            return -1;
        }

        return current_user.UserID;
    }

    public String getFormattedNo() {
        return this.CountryCode + "-" + this.PhoneNo;
    }
}