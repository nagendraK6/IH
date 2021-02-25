package com.relylabs.InstaHelo.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;

import java.util.ArrayList;
import java.util.List;

@Table(name = "Contact")
public class Contact extends Model {

    @Column(name = "Phone", unique = true, onUniqueConflict = Column.ConflictAction.REPLACE)
    public String Phone;

    @Column(name = "Name")
    public String Name;

    @Column(name = "IsUploaded")
    public Boolean IsUploaded;


    @Column(name = "IsInvited")
    public Boolean IsInvited;


    public Contact() {
        super();
        this.Name = "";
        this.Phone = "";
        this.IsUploaded = true;
        this.IsInvited = true;
    }

    public Contact(String name, String phone, Boolean is_uploaded, Boolean IsInvited) {
        this.Name = name;
        this.Phone = phone;
        this.IsUploaded = is_uploaded;
        this.IsInvited = IsInvited;
    }


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

    public static ArrayList<Contact> getAllContactsNotUploaded() {
        // .where("IsUploaded = ?", false).
        List<Contact> all_contacts = new Select().from(Contact.class).where("IsUploaded = ?", false).execute();
        return new ArrayList<Contact>(all_contacts);
    }


    public static ArrayList<Contact> getTopContactsNotInvited(int max_count) {
        List<Contact> all_contacts = new Select().from(Contact.class).where("IsInvited = ?", false).limit(max_count).execute();
        return new ArrayList<Contact>(all_contacts);
    }

    public static boolean checkIfExists(String phone) {
        Contact c = new Select().from(Contact.class).where("Phone = ?", phone).executeSingle();
        return c != null;
    }

    public static Contact getContact(String phone) {
        return new Select().from(Contact.class).where("Phone = ?", phone).executeSingle();
    }
}