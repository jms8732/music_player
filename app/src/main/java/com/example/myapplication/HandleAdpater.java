package com.example.myapplication;

import androidx.lifecycle.MutableLiveData;

public interface HandleAdpater {
    void actionSetting(Music music, int pos);
    void forwardMusic();
    void rewindMusic();
    void setLoop(boolean loop);
}
