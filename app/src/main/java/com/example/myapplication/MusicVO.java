package com.example.myapplication;

import android.os.Parcel;
import android.os.Parcelable;

public class MusicVO implements Parcelable {
    private String title, artist, path, id;
    private int duration ;
    private int album_id;

    public static final Creator<MusicVO> CREATOR = new Creator<MusicVO>() {
        @Override
        public MusicVO createFromParcel(Parcel source) {
            return new MusicVO(source);
        }

        @Override
        public MusicVO[] newArray(int size) {
            return new MusicVO[size];
        }
    };

    protected MusicVO(Parcel in){
        title = in.readString();
        artist= in.readString();
        duration = in.readInt();
        album_id = in.readInt();
        path = in.readString();
        id = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeString(artist);
        dest.writeInt(duration);
        dest.writeInt(album_id);
        dest.writeString(path);
        dest.writeString(id);
    }

    public MusicVO(String title, int duration, String artist, int album_id, String path, String id){
        this.title  = title;
        this.duration = duration;
        this.artist = artist;
        this.album_id = album_id;
        this.path = path;
        this.id = id;
    }

    public void setTitle(String title){
        this.title = title;
    }

    public void setDuration(int duration){
        this.duration = duration;
    }

    public String getTitle(){
        return this.title;
    }

    public int getDuration(){
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

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getId(){
        return this.id;
    }

    public void setId(String id){
        this.id = id;
    }
}
