package com.example.myapplication;

import android.content.Context;
import android.os.Parcelable;
import android.text.Layout;
import android.util.AttributeSet;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

public class PreCachingLayoutManager extends LinearLayoutManager {
    private Context context;
    private int size, height;
    private boolean isFirst;

    public PreCachingLayoutManager(Context context, int size) {
        super(context);
        this.context = context;
        this.size = size;
        this.isFirst = true;
        height = context.getResources().getDimensionPixelOffset(R.dimen.music_layout_height);
    }

    @Override
    protected void calculateExtraLayoutSpace(@NonNull RecyclerView.State state, @NonNull int[] extraLayoutSpace) {
        extraLayoutSpace[1] = size * height;
    }


    private void log(String text) {
        Log.d("jms8732", text);
    }
}