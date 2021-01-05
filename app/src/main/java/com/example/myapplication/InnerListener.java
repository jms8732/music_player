package com.example.myapplication;

public interface InnerListener {
    void pauseMusic();
    void restartMusic();
    void startMusic(Music music, boolean play);
    void reviseProgressbar(int progress);
    void reviseLoop(boolean loop);
    void reviseThumbnailShow(boolean show);
}
