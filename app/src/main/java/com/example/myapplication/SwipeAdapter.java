package com.example.myapplication;

import androidx.recyclerview.widget.RecyclerView;

public interface SwipeAdapter {
    void swipeDelete(RecyclerView.ViewHolder viewHolder, int direction);

}
