package com.example.myapplication;

import android.content.ContentUris;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.widget.ImageView;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;
import androidx.databinding.BindingAdapter;

import com.squareup.picasso.Picasso;

import es.claucookie.miniequalizerlibrary.EqualizerView;

public class Music extends BaseObservable  {
    private String title, artist, path,id;
    private int totalDuration,currentDuration;
    private long album;
    private boolean isplaying, isActivate;


    public Music(String title, String artist, String path, int totalDuration, long album, String id, boolean isplaying) {
        this.title = title;
        this.artist = artist;
        this.path = path;
        this.totalDuration = totalDuration;
        this.album = album;
        this.id = id;
        this.isplaying = isplaying;
        this.isActivate = false;
        this.currentDuration= 0;
    }


    @Bindable
    public long getAlbum() {
        return album;
    }

    public void setAlbum(long album) {
        this.album = album;
        notifyPropertyChanged(BR.album);
    }

    @Bindable
    public boolean isActivate() {
        return isActivate;
    }

    public void setActivate(boolean activate) {
        isActivate = activate;
        notifyPropertyChanged(BR.activate);
    }

    @Bindable
    public boolean isIsplaying() {
        return isplaying;
    }

    public void setIsplaying(boolean isplaying) {
        this.isplaying = isplaying;
        notifyPropertyChanged(BR.isplaying);
    }

    @Bindable
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        notifyPropertyChanged(BR.title);
    }

    @Bindable
    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
        notifyPropertyChanged(BR.artist);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Bindable
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Bindable
    public int getTotalDuration() {
        return totalDuration;
    }
    public void setTotalDuration(int totalDuration) {
        this.totalDuration = totalDuration;
    }

    @Bindable
    public int getCurrentDuration() {
        return currentDuration;
    }

    public void setCurrentDuration(int currentDuration) {
        this.currentDuration = currentDuration;
        notifyPropertyChanged(BR.currentDuration);
    }

    @BindingAdapter("android:thumbnail")
    public static void thumbnail(ImageView view, long album){
        Picasso.get()
                .load(ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"),album))
                .fit()
                .transform(new RoundedCornersTransform())
                .placeholder(R.drawable.ic_launcher_foreground)
                .into(view);
    }

    @BindingAdapter("android:detailAlbum")
    public static void detailAlbum(ImageView view, long album){
        Picasso.get()
                .load(ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"),album))
                .fit()
                .placeholder(R.drawable.ic_launcher_foreground)
                .into(view);
    }

    @BindingAdapter("android:activate")
    public static void activate(EqualizerView view, boolean isplaying){
        if(isplaying)
            view.animateBars();
        else
            view.stopBars();
    }
}
