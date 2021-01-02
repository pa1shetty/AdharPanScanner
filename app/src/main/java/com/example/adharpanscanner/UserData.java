package com.example.adharpanscanner;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class UserData {
    @PrimaryKey(autoGenerate = true)
    public int uid;

    @ColumnInfo(name = "user_name")
    public String userName;

    @ColumnInfo(name = "user_adhar_no")
    public String userAdharNo;
    @ColumnInfo(name = "user_pan_no")
    public String userPanNo;
    public String getUserName() {
        return userName;
    }
    public String getUserAdharNo() {
        return userAdharNo;
    }
    public String getUserPanNo() {
        return userPanNo;
    }
}