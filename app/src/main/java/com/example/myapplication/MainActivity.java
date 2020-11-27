package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private ArrayList<MusicVO> musics = new ArrayList<>();
    private RecyclerView recyclerView;
    private TextView status_show;
    private MusicRecyclerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        recyclerView = (RecyclerView) findViewById(R.id.recycle_view);
        status_show = (TextView) findViewById(R.id.status_show);


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
                , MediaStore.Audio.AudioColumns.DURATION};

        String selection = MediaStore.Files.FileColumns.MIME_TYPE + "=?";
        String[] selectionArgs = new String[]{MimeTypeMap.getSingleton().getMimeTypeFromExtension("mp3")};

        Cursor cursor = this.getContentResolver().query(uri, projection, selection, selectionArgs, null);

        while (cursor != null && cursor.moveToNext()) {
            String title = cursor.getString(0);
            String artist = cursor.getString(1);
            int album_id = cursor.getInt(2);
            long duration = cursor.getLong(3);

            musics.add(new MusicVO(title,duration,artist,album_id));
        }

        //마지막에 '맨 위로' 처리
        musics.add(null);
    }

    private void init() {
        adapter = new MusicRecyclerAdapter(this,recyclerView);
        searchMusicPath();
        if (!musics.isEmpty()) {
            status_show.setVisibility(View.GONE);
            adapter.setList(musics);

            recyclerView.setAdapter(adapter);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
        }
    }

    private void log(String s) {
        Log.d("jms8732", s);
    }
}