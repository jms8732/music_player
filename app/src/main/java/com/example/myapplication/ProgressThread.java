package com.example.myapplication;

import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

public class ProgressThread extends Thread implements Status{
    private boolean stop = false, last = false;
    private String TAG = "jms8732";
    private MediaPlayer mp;
    private Handler handler;

    public ProgressThread(MediaPlayer mp, Handler handler) {
        this.mp = mp;
        this.handler =handler;
    }

    public void preparedStop() {
        stop = true;
    }

    public void lastMusic(){
        last = true;
    }

    @Override
    public void run() {
        Log.d(TAG,"===== Thread start =====");
        while (!stop) {
            SystemClock.sleep(1000);

            Message msg = new Message();
            msg.what = SEND_MSG;
            msg.arg1 = mp.getCurrentPosition();
            handler.sendMessage(msg);
        }

        if(last){
            Message msg = new Message();
            msg.what = SEND_MSG;
            msg.arg1 = 0;
            handler.sendMessage(msg);
        }
        Log.d(TAG,"===== Thread stop =====");
    }
}
