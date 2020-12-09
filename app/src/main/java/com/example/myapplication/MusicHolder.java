package com.example.myapplication;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class MusicHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    TextView title, artist, duration;
    ImageView image, moveButton;
    private myPositionListener listener;

    public MusicHolder(@NonNull View itemView) {
        super(itemView);

        title = (TextView) itemView.findViewById(R.id.title);
        artist = (TextView) itemView.findViewById(R.id.artist);
        image = (ImageView) itemView.findViewById(R.id.image);
        duration = (TextView)itemView.findViewById(R.id.duration);
        moveButton = (ImageView)itemView.findViewById(R.id.moveButton);

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
