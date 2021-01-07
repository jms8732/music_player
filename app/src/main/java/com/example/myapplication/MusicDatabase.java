package com.example.myapplication;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Music.class}, version = 1)
public abstract class MusicDatabase extends RoomDatabase {
    private static MusicDatabase instance;

    public abstract MusicDao musicDao();

    public static MusicDatabase getInstance(Context context){
        if(instance == null){
            instance = Room.databaseBuilder(context,MusicDatabase.class,"music-db").build();
        }

        return instance;
    }

    public static void destroyInstance(){
        instance = null;
    }
}
