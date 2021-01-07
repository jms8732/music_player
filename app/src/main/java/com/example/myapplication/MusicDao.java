package com.example.myapplication;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.ArrayList;
import java.util.List;

@Dao
public interface MusicDao {
    @Query("select * from music")
    LiveData<List<Music>> getAll();

    @Query("select * from music where id not in (:deleteIds)")
    LiveData<List<Music>> loadMusicList(List<String> deleteIds);

    @Insert
    void insertAll(List<Music> musics);

    @Delete
    void delete(Music music);

    @Update
    void update(Music music);


}
