package com.example.myapplication;

import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.text.PrecomputedTextCompat;
import androidx.core.widget.TextViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.FileDescriptor;

public class MusicHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    TextView title, artist, duration;
    ImageView image, moveButton;
    private myPositionListener listener;

    public MusicHolder(@NonNull View itemView ) {
        super(itemView);

        title = (TextView) itemView.findViewById(R.id.title);
        artist = (TextView) itemView.findViewById(R.id.artist);
        image = (ImageView) itemView.findViewById(R.id.image);
        duration = (TextView) itemView.findViewById(R.id.duration);
        moveButton = (ImageView) itemView.findViewById(R.id.moveButton);

        itemView.setOnClickListener(this);

    }

    public void setListener(myPositionListener listener) {
        this.listener = listener;
    }

    @Override
    public void onClick(View v) {
        this.listener.getPosition(getAdapterPosition());
    }

    public void binding(Context context, String id) {
        String t = MusicSearcher.findDisplayName(context, id);
        String a = MusicSearcher.findArtist(context, id);
        String d = convertDuration(MusicSearcher.findDuration(context, id));
        int albumId = MusicSearcher.findAlbumId(context, id);

        title.setText(t);
        artist.setText(a);
        duration.setText(d);
    /*    moveButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    Log.d("jms8732", "Action down.........");
                    dragListener.onStartDrag(holder);
                }
                return true;
            }
        });*/
        Glide.with(context)
                .load(getAlbumart(context, albumId))
                .placeholder(R.drawable.album)
                .override(50, 50)
                .thumbnail(0.1f)
                .into(image);
    }

    //1000 millisec = 1sec;
    private String convertDuration(long duration) {
        String ret = null;

        long hour = (duration / 3600000);
        long minute = (duration % 3600000) / 60000;
        long sec = ((duration % 3600000) % 60000) / 1000;

        if (hour > 0)
            ret = String.format("%02d:%02d:%02d", hour, minute, sec);
        else
            ret = String.format("%02d:%02d", minute, sec);
        return ret;
    }

    //mp3의 섬네일
    private Bitmap getAlbumart(Context context, long album_id) {
        Bitmap bm = null;
        try {
            final Uri sArtworkUri = Uri
                    .parse("content://media/external/audio/albumart");

            Uri uri = ContentUris.withAppendedId(sArtworkUri, album_id);

            ParcelFileDescriptor pfd = context.getContentResolver()
                    .openFileDescriptor(uri, "r");

            if (pfd != null) {
                FileDescriptor fd = pfd.getFileDescriptor();
                bm = BitmapFactory.decodeFileDescriptor(fd);
            }
        } catch (Exception e) {
        }
        return bm;
    }
}
