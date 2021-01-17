package com.example.myapplication;

import android.app.Application;
import android.content.ContentUris;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.TransitionDrawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.databinding.BindingAdapter;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class MusicViewModel {
    private MutableLiveData<String> title;
    private MutableLiveData<String> artist;
    private MutableLiveData<Boolean> play;
    private MutableLiveData<Long> albumId;
    private MutableLiveData<Integer> totalDuration;
    private MutableLiveData<Boolean> oncePlay;
    private MutableLiveData<Integer> speaker;
    private MutableLiveData<Integer> progressDuration;
    private MutableLiveData<Music> currentMusic;
    private MutableLiveData<Boolean> loop;
    private MutableLiveData<Boolean> random;

    public MusicViewModel() {
        this.title = new MutableLiveData<>();
        this.artist = new MutableLiveData<>();
        this.play = new MutableLiveData<>();
        this.albumId = new MutableLiveData<>();
        this.totalDuration = new MutableLiveData<>();
        this.oncePlay = new MutableLiveData<>();
        this.speaker = new MutableLiveData<>();
        this.progressDuration = new MutableLiveData<>();
        this.currentMusic = new MutableLiveData<>();
        this.loop = new MutableLiveData<>();
        this.random = new MutableLiveData<>();

        play.setValue(false);
        oncePlay.setValue(false);
        progressDuration.setValue(0);
        totalDuration.setValue(0);
        loop.setValue(false);
        random.setValue(false);
    }

    public MutableLiveData<String> getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title.setValue(title);
    }

    public MutableLiveData<String> getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist.setValue(artist);
    }

    public MutableLiveData<Boolean> getPlay() {
        return play;
    }

    public void setPlay(boolean thumbnailPlay) {
        this.play.setValue(thumbnailPlay);
    }

    public MutableLiveData<Long> getAlbumId() {
        return albumId;
    }

    public void setAlbumId(Long albumId) {
        this.albumId.setValue(albumId);
    }

    public MutableLiveData<Integer> getTotalDuration() {
        return totalDuration;
    }

    public void setTotalDuration(int totalDuration) {
        this.totalDuration.setValue(totalDuration);
    }

    public MutableLiveData<Boolean> getOncePlay() {
        return oncePlay;
    }

    public void setOncePlay(boolean playCheck) {
        this.oncePlay.setValue(playCheck);
    }

    public MutableLiveData<Integer> getSpeaker() {
        return speaker;
    }

    public void setSpeaker(int speaker) {
        this.speaker.setValue(speaker);
    }

    public MutableLiveData<Integer> getProgressDuration() {
        return progressDuration;
    }

    public void setProgressDuration(int progressDuration) {
        this.progressDuration.setValue(progressDuration);
    }

    public MutableLiveData<Music> getCurrentMusic() {
        return currentMusic;
    }

    public void setCurrentMusic(Music currentMusic) {
        this.currentMusic.setValue(currentMusic);
    }

    public MutableLiveData<Boolean> getLoop() {
        return loop;
    }

    public void setLoop(boolean loop) {
        this.loop.setValue(loop);
    }

    public MutableLiveData<Boolean> getRandom() {
        return random;
    }

    public void setRandom(boolean b) {
        this.random.setValue(b);
    }

    public void updateMusicView(Music music) {
        progressDuration.setValue(0);
        title.setValue(music.getTitle());
        artist.setValue(music.getArtist());
        albumId.setValue(music.getAlbum_id());
        totalDuration.setValue(music.getDuration());
        currentMusic.setValue(music);

        if (!oncePlay.getValue()) //처음에 한번도 실행하지 않았을 경우
            oncePlay.setValue(!oncePlay.getValue());
    }

    public void updatePlayButton(boolean b) {
        play.setValue(b);
    }

    @BindingAdapter("android:loadImage")
    public static void loadImage(final ImageView view, long albumId) {
        Picasso.get()
                .load(ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"),albumId))
                .fit()
                .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
                .placeholder(R.drawable.album_white)
                .into(view);
    }

    @BindingAdapter("android:loadUrl")
    public static void loadUrl(ImageView view, long albumId) {
        Picasso.get()
                .load(ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"),albumId))
                .fit()
                .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE)
                .placeholder(R.drawable.album_white)
                .into(view);
    }

    @BindingAdapter("android:playThumbnail")
    public static void playThumbnail(ImageView view, boolean playCheck) {
        if (playCheck)
            view.setImageResource(R.drawable.circle_pause);
        else
            view.setImageResource(R.drawable.circle_play);
    }

    @BindingAdapter("android:playDetail")
    public static void playDetail(ImageView view, boolean playCheck) {
        if (playCheck)
            view.setImageResource(R.drawable.pause_white);
        else
            view.setImageResource(R.drawable.play_white);
    }

    @BindingAdapter("android:loadLoopImage")
    public static void loadLoopImage(ImageView view, boolean loop) {
        if (loop)
            view.setImageResource(R.drawable.repeat_activate);
        else
            view.setImageResource(R.drawable.repeat_white);
    }

    @BindingAdapter("android:loadRepeatImage")
    public static void loadRepeatImage(ImageView view, boolean b) {
        if (b) {
            view.setImageResource(R.drawable.shuffle_activate);
        } else
            view.setImageResource(R.drawable.shuffle_white);
    }


}
