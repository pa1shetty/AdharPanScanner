package com.example.adharpanscanner;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface UserDao {
    @Query("SELECT * FROM UserData")
    List<UserData> getAll();

    @Insert
    void insertAll(UserData user);


    @Query("DELETE FROM UserData")
     void delete();

    @Query("SELECT * FROM userdata ORDER BY uid DESC LIMIT 1")
    UserData getRecentUser();

    @Query("INSERT INTO userdata(user_name,user_adhar_no) VALUES (:userName, :userAdharNo)")
    void insertAdharData(String userName,String userAdharNo);

    @Query("SELECT COUNT(uid) FROM userdata")
    int getCount();
    @Query("UPDATE userdata SET user_name = :userName,user_adhar_no = :userAdharNo WHERE uid = :uid")
    void updateUseAdhar(int uid, String userName, String userAdharNo);

    @Query("INSERT INTO userdata(user_pan_no) VALUES (:userPan)")
    void insertPanData(String userPan);

    @Query("UPDATE userdata SET user_pan_no = :userPan WHERE uid = :uid")
    void updateUsePan(int uid, String userPan);


}