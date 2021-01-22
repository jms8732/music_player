package com.example.musicplayer;

import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;

public class DurationHandler extends Handler {
    private DurationThread thread;
    private MusicViewModel model;
    public DurationHandler(MusicViewModel model){
        this.model = model;
    }

    public void setThread(DurationThread thread){
        this.thread = thread;
    }


    @Override
    public void handleMessage(@NonNull Message msg) {
        switch (msg.what){
            case 0:
                thread.postStop();
                break;

            case 1:
                model.setCurrentDuration(msg.arg1);
                break;
        }
    }
}
