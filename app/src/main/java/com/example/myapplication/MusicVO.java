package com.example.myapplication;

public class MusicVO{
    private String title, artist;
    private long duration ;
    private int album_id;

    public MusicVO(String title, long duration, String artist, int album_id){
        this.title  = title;
        this.duration = duration;
        this.artist = artist;
        this.album_id = album_id;
    }

    public void setTitle(String title){
        this.title = title;
    }

    public void setDuration(long duration){
        this.duration = duration;
    }

    public String getTitle(){
        return this.title;
    }

    public long getDuration(){
        return this.duration;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public void setAlbum_id(int album_id) {
        this.album_id = album_id;
    }

    public int getAlbum_id() {
        return album_id;
    }

}
