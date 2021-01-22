package com.example.musicplayer;

import android.media.MediaPlayer;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import java.util.logging.Handler;

public class DurationThread extends Thread{
    private String TAG  ="jms8732";
    private boolean stop = false;
    private DurationHandler handler;
    private MediaPlayer mPlayer;
    public DurationThread(DurationHandler handler){
        this.handler = handler;
    }

    public void setPlayer(MediaPlayer mediaPlayer){
        this.mPlayer = mediaPlayer;
    }

    public void postStop(){
        stop = true;
    }

    @Override
    public void run() {
        Log.d(TAG, "=====start======");
        while(!stop){
            SystemClock.sleep(1000);
            Message msg = new Message();
            msg.what = 1;
            msg.arg1 = mPlayer.getCurrentPosition();

            handler.sendMessage(msg);
        }
        Log.d(TAG, "=====stop=====");
    }
}
