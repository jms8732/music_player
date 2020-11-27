package com.example.myapplication;

import android.content.Context;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class LastHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
    private Context context;
    private myPositionListener listener;

    public LastHolder(@NonNull View itemView) {
        super(itemView);
        itemView.setOnClickListener(this);
    }

    public void setListener(myPositionListener listener){
        this.listener = listener;
    }

    @Override
    public void onClick(View v) {
        this.listener.getPosition(getAdapterPosition());
    }
}
