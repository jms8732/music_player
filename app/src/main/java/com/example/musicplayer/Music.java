package com.example.musicplayer;

public class Music {
    private int duration ;
    private long album;
    private String id, title, artist, path;
    private boolean visible;

    public Music(String id, int duration, long album, String title, String artist, String path) {
        this.id = id;
        this.duration = duration;
        this.album = album;
        this.title = title;
        this.artist = artist;
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
}
