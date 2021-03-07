package com.example.myapplication;

import android.util.Log;

public class MusicClickListener {
    private static final String TAG = "jms8732";
    private clickAdapter adapter;

    public MusicClickListener(clickAdapter adapter) {
        this.adapter =adapter;
    }

    public void onItemClick(Music m){
        adapter.rawClick(m);
    }
}
