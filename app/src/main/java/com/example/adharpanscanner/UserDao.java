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

}