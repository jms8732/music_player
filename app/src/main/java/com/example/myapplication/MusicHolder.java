package com.example.myapplication;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class MusicHolder extends RecyclerView.ViewHolder {
    TextView title, artist;
    ImageView image;

    public MusicHolder(@NonNull View itemView) {
        super(itemView);

        title = (TextView) itemView.findViewById(R.id.title);
        artist = (TextView) itemView.findViewById(R.id.artist);
        image = (ImageView) itemView.findViewById(R.id.image);
    }
}
