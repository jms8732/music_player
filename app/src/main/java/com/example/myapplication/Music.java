package com.example.myapplication;

import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.databinding.BindingAdapter;
import androidx.databinding.DataBinderMapper;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;

import java.io.FileDescriptor;

@Entity
public class Music {
    @NonNull
    @PrimaryKey
    private String id;
    private String title, artist, path;
    private int image, duration;


    public Music(@NotNull  String id, String title, String artist, int duration, int image, String path) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.duration = duration;
        this.image = image;
        this.path = path;
    }

    @BindingAdapter("android:loadImage")
    public static void loadImage(ImageView view, int resId) {
        Picasso.get()
                .load(ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), resId))
                .fit()
                .centerCrop()
                .memoryPolicy(MemoryPolicy.NO_CACHE,MemoryPolicy.NO_STORE)
                .placeholder(R.drawable.album_white)
                .into(view);
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getImage() {
        return image;
    }

    public void setImage(int image) {
        this.image = image;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }
}
