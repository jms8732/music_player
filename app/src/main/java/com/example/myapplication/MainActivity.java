package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.ActivityManager;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private ArrayList<String> musics = new ArrayList<>();
    private String path = "/storage/emulated/0/";
    private RecyclerView recyclerView;
    private MusicRecyclerAdapter adapter = new MusicRecyclerAdapter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        recyclerView = (RecyclerView)findViewById(R.id.recycle_view);

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
            init();
        }else
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},101);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == 101){
            if(grantResults[0] ==PackageManager.PERMISSION_GRANTED){
                init();
            }

        }
    }

    //안드로이드 내에 존재하는 파일들을 탐색하여 확장자 mp3를 가진 파일들을 찾는 메소드
    private void searchMusicPath(String path){
        File directory = new File(path);
        File[] files = directory.listFiles();

        if(files != null) {
            for (File f : files) {
                if (f.isFile()) {
                    if (f.getAbsolutePath().contains(".mp3")) {
                        //해당 파일이 mp3 파일인 경우
                        musics.add(f.getAbsolutePath());
                    }
                } else //파일이 아니면 해당 경로를 통해 파일 탐색
                    searchMusicPath(f.getAbsolutePath());
            }
        }

        return;
    }

    private void init(){
        searchMusicPath(path);
        adapter.setList(musics);

        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void log(String s) {
        Log.d("jms8732", s);
    }
}