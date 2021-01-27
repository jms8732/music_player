package com.example.musicplayer;

import android.text.SpannableStringBuilder;

public class Music {
    private int duration ;
    private long album;
    private SpannableStringBuilder title, artist;
    private String id, path;
    private boolean visible;

    public Music(String id, int duration, long album, String title, String artist, String path) {
        this.id = id;
        this.duration = duration;
        this.album = album;
        this.title = new SpannableStringBuilder(title);
        this.artist = new SpannableStringBuilder(artist);
        this.path = path;
        this.visible = false;
    }


    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public long getAlbum() {
        return album;
    }

    public void setAlbum(long album) {
        this.album = album;
    }

    public SpannableStringBuilder getTitle() {
        return title;
    }

    public void setTitle(SpannableStringBuilder title) {
        this.title = title;
    }

    public SpannableStringBuilder getArtist() {
        return artist;
    }

    public void setArtist(SpannableStringBuilder artist) {
        this.artist = artist;
    }
}
