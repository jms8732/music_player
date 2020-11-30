package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.skydoves.transformationlayout.OnTransformFinishListener;
import com.skydoves.transformationlayout.TransformationAppCompatActivity;
import com.skydoves.transformationlayout.TransformationCompat;
import com.skydoves.transformationlayout.TransformationLayout;

import net.cachapa.expandablelayout.ExpandableLayout;

import java.io.File;
import java.io.FileDescriptor;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements OnSelectedItemListener, View.OnClickListener {
    private ArrayList<MusicVO> musics = new ArrayList<>();
    private RecyclerView recyclerView;
    private TextView status_show, thumbnail_title, thumbnail_artist, music_title, music_artist, current_duration, total_duration;
    private ImageView thumbnail_play, play, music_image;
    private MusicRecyclerAdapter adapter;
    private LinearLayout belowMusicMenu;
    private TransformationLayout transformationLayout;
    private int position;
    private SeekBar music_progress, speaker;
    private AudioManager manager;
    private View music_detail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        manager = (AudioManager)getSystemService(AUDIO_SERVICE);

        recyclerView = (RecyclerView) findViewById(R.id.recycle_view);
        thumbnail_play = (ImageView) findViewById(R.id.play);
        thumbnail_play.setOnClickListener(this);

        status_show = (TextView) findViewById(R.id.status_show);
        thumbnail_title = (TextView) findViewById(R.id.thumbnail_title);
        thumbnail_artist = (TextView) findViewById(R.id.thumbnail_artist);
        belowMusicMenu = (LinearLayout) findViewById(R.id.belowMusicMenu);
        transformationLayout = (TransformationLayout) findViewById(R.id.transformation_layout);
        transformationLayout.onTransformFinishListener = new OnTransformFinishListener() {
            @Override
            public void onFinish(boolean b) {
            }
        };

        belowMusicMenu.setOnClickListener(this);


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            init();
        } else
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 101);

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 101) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                init();
            }

        }
    }

    //안드로이드 내에 존재하는 파일들을 탐색하여 확장자 mp3를 가진 파일들을 찾는 메소드
    private void searchMusicPath() {
        final Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        final String[] projection = new String[]{MediaStore.Audio.Media.DISPLAY_NAME
                , MediaStore.Audio.Artists.ARTIST
                , MediaStore.Audio.AlbumColumns.ALBUM_ID
                , MediaStore.Audio.AudioColumns.DURATION
                , MediaStore.Audio.AudioColumns.DATA};

        String selection = MediaStore.Files.FileColumns.MIME_TYPE + "=?";
        String[] selectionArgs = new String[]{MimeTypeMap.getSingleton().getMimeTypeFromExtension("mp3")};

        Cursor cursor = this.getContentResolver().query(uri, projection, selection, selectionArgs, null);

        while (cursor != null && cursor.moveToNext()) {
            String title = cursor.getString(0);
            String artist = cursor.getString(1);
            int album_id = cursor.getInt(2);
            int duration = cursor.getInt(3);
            String data = cursor.getString(4);

            musics.add(new MusicVO(title, duration, artist, album_id, data));
        }

        //마지막에 '맨 위로' 처리
        musics.add(null);
    }

    private void init() {
        adapter = new MusicRecyclerAdapter(this, recyclerView);

        searchMusicPath();
        setLayout();
        if (!musics.isEmpty()) {
            status_show.setVisibility(View.GONE);
            adapter.setList(musics);
            adapter.setItemListener(this);

            recyclerView.setAdapter(adapter);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
        }
    }

    //확장된 레이아웃 세팅
    private void setLayout(){
        music_detail = findViewById(R.id.targetView);
        play = (ImageButton)music_detail.findViewById(R.id.play);
        play.setOnClickListener(this);
        music_title = (TextView)music_detail.findViewById(R.id.music_title);
        music_title.setSelected(true);
        music_artist = (TextView)music_detail.findViewById(R.id.artist);
        music_artist.setSelected(true);
        current_duration = (TextView)music_detail.findViewById(R.id.current_duration);
        total_duration = (TextView)music_detail.findViewById(R.id.total_duration);
        music_image = (ImageView)music_detail.findViewById(R.id.music_image);
        speaker = (SeekBar)music_detail.findViewById(R.id.speaker);
        music_progress = (SeekBar)music_detail.findViewById(R.id.music_progress);

        int vol = manager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int max = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        speaker.setProgress(vol);
        speaker.setMax(max);

    }

    private void setPermission(){
        //화면 전환을 permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //시스템에 쓰기 위한
            if (!Settings.System.canWrite(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + this.getPackageName()));
                startActivity(intent);
            }
        } else {
            //시스템에 쓰기 위한
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + this.getPackageName()));
            startActivity(intent);
        }
    }

    //선택된 음악에 대해 화면 변경
    @Override
    public void ChangeLayout(String title, String artist, int position) {
        MusicVO musicVO = musics.get(position);
        belowMusicMenu.setVisibility(View.VISIBLE);
        thumbnail_title.setText(title);
        thumbnail_title.setSelected(true); //marquee 진행
        thumbnail_artist.setText(artist);

        music_artist.setText(artist);
        music_title.setText(title);
        total_duration.setText(convertDuration(musicVO.getDuration()));


        Glide.with(this)
                .load(getAlbumart(musicVO.getAlbum_id()))
                .centerInside()
                .placeholder(R.drawable.ic_launcher_foreground)
                .into(music_image);

    }

    @Override
    public void onClick(View v) {
        if (v == play) {
            Toast.makeText(this, "play click....", Toast.LENGTH_SHORT).show();
        } else if (v == belowMusicMenu) {
            Toast.makeText(this, "belowMusicMenu click....", Toast.LENGTH_SHORT).show();
            if(!transformationLayout.isTransformed()){
                transformationLayout.startTransform();
            }else {
                transformationLayout.finishTransform();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if(transformationLayout.isTransformed())
            transformationLayout.finishTransform();
    }

    private void log(String s) {
        Log.d("jms8732", s);
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
    private Bitmap getAlbumart(long album_id) {
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
}