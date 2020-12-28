package com.example.myapplication;

import android.app.Application;
import android.content.Intent;
import android.os.Message;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.databinding.BindingAdapter;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.bumptech.glide.Glide;

public class MainViewModel extends AndroidViewModel implements InnerListener {
    private final MutableLiveData<String> title = new MutableLiveData<>();
    private final MutableLiveData<String> artist = new MutableLiveData<>();
    private final MutableLiveData<Boolean> thumbnailPlay = new MutableLiveData<>();
    private final MutableLiveData<Integer> image = new MutableLiveData<>();
    private final MutableLiveData<Integer> total_duration = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isPlaying = new MutableLiveData<>();
    private final MutableLiveData<Integer> speaker = new MutableLiveData<>();
    private final MutableLiveData<Integer> progress = new MutableLiveData<>();
    private final MutableLiveData<Music> currentMusic = new MutableLiveData<>();


    public MainViewModel(@NonNull Application application) {
        super(application);
        thumbnailPlay.setValue(false);
        isPlaying.setValue(false);
        progress.setValue(0);
        total_duration.setValue(0);
    }

    public MutableLiveData<Integer> getSpeaker() {
        return speaker;
    }

    public void setSpeaker(int volume) {
        speaker.setValue(volume);
    }

    public void setProgress(int progress) {
        this.progress.setValue(progress);
    }

    public MutableLiveData<Boolean> getThumbnailPlay() {
        return thumbnailPlay;
    }

    public MutableLiveData<String> getTitle() {
        return title;
    }

    public MutableLiveData<String> getArtist() {
        return artist;
    }

    public MutableLiveData<Integer> getImage() {
        return image;
    }

    public MutableLiveData<Music> getCurrentMusic() {
        return currentMusic;
    }

    public MutableLiveData<Boolean> getIsPlaying() {
        return isPlaying;
    }

    public MutableLiveData<Integer> getProgress() {
        return progress;
    }

    public MutableLiveData<Integer> getTotal_duration() {
        return total_duration;
    }

    @Override
    public void restartMusic() {
        thumbnailPlay.setValue(true);
    }

    @Override
    public void startMusic(Music music) {
        String title = music.getTitle();
        String artist = music.getArtist();
        int resId = music.getImage();
        int duration = music.getDuration();

        this.title.setValue(title);
        this.artist.setValue(artist);
        this.image.setValue(resId);
        this.total_duration.setValue(duration);

        if (!isPlaying.getValue())
            isPlaying.setValue(true);

        thumbnailPlay.setValue(true);
        currentMusic.setValue(music);
    }

    @Override
    public void pauseMusic() {
        thumbnailPlay.setValue(false);
    }

    @Override
    public void reviseProgressbar(int progress) {
        this.progress.setValue(progress);
    }

    @BindingAdapter("android:loadUrl")
    public static void loadUrl(ImageView view, int resId) {
        Glide.with(view.getContext())
                .load(Util.getAlbumart(view.getContext(), resId))
                .thumbnail(0.4f)
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

}
