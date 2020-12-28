package com.example.myapplication;

import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.widget.ImageView;

import androidx.databinding.BindingAdapter;
import androidx.databinding.DataBinderMapper;

import com.bumptech.glide.Glide;

import java.io.FileDescriptor;

public class Music {
    private String title, artist, path, id;
    private int image, duration;


    public Music(String id, String title, String artist, int duration, int album_id, String path) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.duration = duration;
        this.image = album_id;
        this.path = path;
    }

    @BindingAdapter("android:loadImage")
    public static void loadImage(ImageView view, int resId) {
        Glide.with(view.getContext())
                .load(Util.getAlbumart(view.getContext(), resId))
                .thumbnail(0.25f)
                .skipMemoryCache(false)
                .placeholder(R.drawable.album_white)
                .into(view);
    }

    public String getTitle() {
        return title;
    }

    public int getImage() {
        return image;
    }


    public String getArtist() {
        return artist;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setImage(int image) {
        this.image = image;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }


    public String getId() {
        return id;
    }

    public String getPath() {
        return path;
    }
}
