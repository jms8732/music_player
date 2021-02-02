package com.example.musicplayer;

import android.app.Application;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.IBinder;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.databinding.BindingAdapter;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.recyclerview.widget.ItemTouchHelper;

import com.squareup.picasso.Picasso;

import es.claucookie.miniequalizerlibrary.EqualizerView;
import kotlin.jvm.internal.markers.KMutableCollection;

public class MusicViewModel {
    private final static String TAG = "jms8732";
    private MutableLiveData<Spannable> title;
    private MutableLiveData<Spannable> artist;
    private MutableLiveData<Long> album;
    private MutableLiveData<Boolean> status;
    private MutableLiveData<Boolean> loop;
    private MutableLiveData<Integer> totalDuration;
    private MutableLiveData<Integer> currentDuration;
    private MutableLiveData<Boolean> sequential;

    public MusicViewModel() {
        title = new MutableLiveData<>();
        album = new MutableLiveData<>();
        artist = new MutableLiveData<>();
        status = new MutableLiveData<>();
        totalDuration = new MutableLiveData<>();
        currentDuration = new MutableLiveData<>();
        loop = new MutableLiveData<>();
        sequential = new MutableLiveData<>();
        sequential.setValue(true);
    }

    public MutableLiveData<Boolean> getSequential() {
        return sequential;
    }

    public void setSequential(boolean repeat) {
        this.sequential.setValue(repeat);
    }

    public MutableLiveData<Boolean> getLoop() {
        return loop;
    }

    public void setLoop(boolean loop) {
        this.loop.setValue(loop);
    }

    public MutableLiveData<Integer> getCurrentDuration() {
        return currentDuration;
    }

    public void setCurrentDuration(int currentDuration) {
        this.currentDuration.setValue(currentDuration);
    }

    public MutableLiveData<Integer> getTotalDuration() {
        return totalDuration;
    }

    public void setTotalDuration(int totalDuration) {
        this.totalDuration.setValue(totalDuration);
    }

    public MutableLiveData<Spannable> getTitle() {
        return title;
    }

    public MutableLiveData<Spannable> getArtist() {
        return artist;
    }

    public MutableLiveData<Long> getAlbum() {
        return album;
    }

    public void setTitle(SpannableStringBuilder title) {
        this.title.setValue(title);
    }

    public void setArtist(SpannableStringBuilder artist) {
        this.artist.setValue(artist);
    }
    public void setAlbum(long album) {
        this.album.setValue(album);
    }

    public MutableLiveData<Boolean> getStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status.setValue(status);
    }

    @BindingAdapter("android:loadThumbnail")
    public static void loadThumbnail(ImageView view, long album){
        Picasso.get()
                .load(ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"),album))
                .fit()
                .transform(new RoundedCornersTransform())
                .placeholder(R.drawable.ic_launcher_foreground)
                .into(view);
    }

    @BindingAdapter("android:buttonStatus")
    public static void buttonStatus(ImageView view, boolean status){
        if(status){
            view.setImageResource(R.drawable.pause);
        }else
            view.setImageResource(R.drawable.play);
    }

    @BindingAdapter("android:activate")
    public static void activate(EqualizerView view, boolean status){
        if(status){
            view.animateBars();
        }else {
            view.stopBars();
        }
    }

    @BindingAdapter("android:setLoop")
    public static void setLoop(ImageView view, boolean loop){
        if(loop)
            view.setImageResource(R.drawable.repeat_activate);
        else
            view.setImageResource(R.drawable.repeat);
    }

    @BindingAdapter("android:setSequential")
    public static void setSequential(ImageView view, boolean seq){
        if(seq)
            view.setImageResource(R.drawable.loop);
        else
            view.setImageResource(R.drawable.loop_activate);
    }

    @BindingAdapter({"bind:helper","bind:holder"})
    public static void onTouch(View view, ItemTouchHelper helper, MusicAdapter.MusicHolder holder){
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    helper.startDrag(holder);
                    return true;
                }
                return false;
            }
        });
    }
}
