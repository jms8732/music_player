package com.example.myapplication;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {
    private static final int DB_VERSION = 1;

    public DBHelper(Context context){
        super(context,"music_db",null,DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "create table tb_m " +
                "(id primary key, title, artist, duration, album_id, path)";

        db.execSQL(sql);
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if(newVersion == DB_VERSION){
            db.execSQL("drop table tb_m");
            onCreate(db);
        }
    }
}
