package com.example.myapplication;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

public class PreCachingLayoutManager extends LinearLayoutManager {
    private Context context;
    private int size, height;

    public PreCachingLayoutManager(Context context , int size) {
        super(context);
        this.context = context;
        this.size = size;

        height = context.getResources().getDimensionPixelOffset(R.dimen.music_layout_height);
    }

    @Override
    protected void calculateExtraLayoutSpace(@NonNull RecyclerView.State state, @NonNull int[] extraLayoutSpace) {

        if(getOrientation() == LinearLayoutManager.VERTICAL){
            extraLayoutSpace[0] = height * size;
            extraLayoutSpace[1] = height * size;
        }
    }
}