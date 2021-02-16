package com.relylabs.InstaHelo.models;

import android.os.Parcel;
import android.os.Parcelable;

public class Contact implements Parcelable {
    private String Name;
    private String Phone;
    private Boolean IsSelected;

    public Contact(String name, String phone) {
        this.Name = name;
        this.Phone = phone;
        this.IsSelected = false;
    }

    public static final Creator<Contact> CREATOR = new Creator<Contact>() {
        @Override
        public Contact createFromParcel(Parcel in) {
            return new Contact(in);
        }

        @Override
        public Contact[] newArray(int size) {
            return new Contact[size];
        }
    };

    public String getName() {
        return Name;
    }


    public String getPhone() {
        return Phone;
    }

    public void setName(String name) {
        Name = name;
    }

    public void setPhone(String phone) {
        Phone = phone;
    }

    public Boolean getSelected() {
        return IsSelected;
    }

    public void setSelected(Boolean selected) {
        IsSelected = selected;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    protected Contact(Parcel in) {
        Name = in.readString();
        Phone = in.readString();
        IsSelected = in.readByte() == 1;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(Name);
        dest.writeString(Phone);
        dest.writeByte((byte) (this.IsSelected ? 1 : 0));
    }
}