package com.example.myapplication;

import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.skydoves.transformationlayout.TransformationAppCompatActivity;
import com.skydoves.transformationlayout.TransformationCompat;
import com.skydoves.transformationlayout.TransformationLayout;

import java.io.FileDescriptor;

public class MusicActivity extends TransformationAppCompatActivity implements View.OnClickListener {
    TextView title, artist, current_duration, total_duration;
    ImageView music_image;
    ImageButton play, skip_previous, skip_next;
    private boolean isService = false;
    private MusicService mService;
    private MediaPlayer mp;
    private SeekBar music_progress, speaker;
    private int totalDuration = 0;
    private AudioManager manager;

    ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            mService = binder.getService();
            isService = true;

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isService = false;
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        TransformationCompat.onTransformationStartContainer(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.music_detail);

        title = (TextView) findViewById(R.id.music_title);
        title.setSelected(true); //marquee를 하기 위해

        artist = (TextView) findViewById(R.id.artist);
        current_duration = (TextView) findViewById(R.id.current_duration);
        total_duration = (TextView) findViewById(R.id.total_duration);

        music_image = (ImageView) findViewById(R.id.music_image);

        play = (ImageButton) findViewById(R.id.detail_play);
        play.setOnClickListener(this);

        music_progress = (SeekBar) findViewById(R.id.music_progress);
        speaker = (SeekBar)findViewById(R.id.speaker);
        manager = (AudioManager)getSystemService(AUDIO_SERVICE);

        log("onCreate....");
        init();
    }

    @Override
    public void onClick(View v) {
       if(v == play){
           Toast.makeText(this, "play..", Toast.LENGTH_SHORT).show();
       }
    }

    //화면 설정
    private void init() {
        final MusicVO data = getIntent().getParcelableExtra("data");

        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);

        title.setText(data.getTitle());
        artist.setText(data.getArtist());
        current_duration.setText("00:00");
        total_duration.setText(convertDuration(data.getDuration()));
        music_progress.setMax(data.getDuration());

        Bitmap map = getAlbumart(data.getAlbum_id());
        if (map != null)
            music_image.setImageBitmap(map);

        totalDuration = data.getDuration();
        int volume = manager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int max= manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        speaker.setMax(max);
        speaker.setProgress(volume);
    }

    //1000 millisec = 1sec;
    private String convertDuration(long duration) {
        String ret = null;

        long hour = (duration / 3600000);
        long minute = (duration % 3600000) / 60000;
        long sec = ((duration % 3600000) % 60000) / 1000;

        if (hour > 0)
            ret = String.format("%02d:%02d:%02d", hour, minute, sec);
        else
            ret = String.format("%02d:%02d", minute, sec);
        return ret;
    }

    //mp3의 섬네일
    public Bitmap getAlbumart(long album_id) {
        Bitmap bm = null;
        try {
            final Uri sArtworkUri = Uri
                    .parse("content://media/external/audio/albumart");

            Uri uri = ContentUris.withAppendedId(sArtworkUri, album_id);

            ParcelFileDescriptor pfd = getContentResolver()
                    .openFileDescriptor(uri, "r");

            if (pfd != null) {
                FileDescriptor fd = pfd.getFileDescriptor();
                bm = BitmapFactory.decodeFileDescriptor(fd);
            }
        } catch (Exception e) {
        }
        return bm;
    }

    @Override
    public void onBackPressed() {
       finishAfterTransition();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        log("onDestroy...");
    }

    private void log(String s) {
        Log.d("jms8732", s);
    }

}
