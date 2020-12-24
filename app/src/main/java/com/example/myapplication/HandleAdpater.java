package com.example.myapplication;

import androidx.lifecycle.MutableLiveData;

public interface HandleAdpater {
    void startMusic(Music music, int status, int pos);
    void restartMusic();
    void pauseMusic();
}
