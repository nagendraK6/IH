package com.relylabs.InstaHelo.models;

import android.os.Parcel;
import android.os.Parcelable;

public class UsersInRoom  implements Parcelable {
    public Boolean IsSpeaker;
    public String Name;
    public String profileImageURL;
    public Boolean IsdataFetchRequired;
    public Integer UserId;
    public Boolean IsMuted;

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

    protected UsersInRoom(Parcel in) {
        IsSpeaker = in.readByte() == 1;
        IsMuted = in.readByte() == 1;
        UserId = in.readInt();
        IsdataFetchRequired = in.readByte() == 1;
        Name = in.readString();
        profileImageURL = in.readString();
    }

    public static final Creator<UsersInRoom> CREATOR = new Creator<UsersInRoom>() {
        @Override
        public UsersInRoom createFromParcel(Parcel in) {
            return new UsersInRoom(in);
        }

        @Override
        public UsersInRoom[] newArray(int size) {
            return new UsersInRoom[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (this.IsSpeaker ? 1 : 0));
        dest.writeByte((byte) (this.IsMuted ? 1 : 0));
        dest.writeInt(UserId);
        dest.writeByte((byte) (this.IsdataFetchRequired ? 1 : 0));
        dest.writeString(Name);
        dest.writeString(profileImageURL);
    }
}