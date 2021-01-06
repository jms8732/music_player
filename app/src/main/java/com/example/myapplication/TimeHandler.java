package com.example.myapplication;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

public class TimeHandler extends Handler implements  Status {
    private MusicViewModel musicViewModel;
    private ProgressThread pt;
    private String TAG = "jms8732";
    public TimeHandler(MusicViewModel musicViewModel) {
        this.musicViewModel = musicViewModel;
    }

    public void setProgressThread(ProgressThread pt){
        this.pt = pt;
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        switch (msg.what) {
            case SEND_MSG:
                musicViewModel.setProgressDuration(msg.arg1);
                break;
            case SEND_STOP:
                musicViewModel.setProgressDuration(0);
                musicViewModel.updatePlayButton(false);
                pt.preparedStop();
                break;
        }
    }
}
