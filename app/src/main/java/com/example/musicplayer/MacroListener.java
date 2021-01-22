package com.example.musicplayer;

import android.util.Log;
import android.view.View;

import com.example.musicplayer.databinding.ListRowBinding;

public class MacroListener {
    private final String TAG = "jms8732";
    private MacroAdapter mAdapter;

    public MacroListener(MacroAdapter adapter) {
        this.mAdapter = adapter;
    }

    public void rawItemClick(int pos) {
       mAdapter.onItemClick(pos);
    }

}
