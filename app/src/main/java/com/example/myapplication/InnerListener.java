package com.example.myapplication;

public interface InnerListener {
    void pauseMusic();
    void restartMusic();
    void startMusic(Music music);
    void reviseProgressbar(int progress);
}
