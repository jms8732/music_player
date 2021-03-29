package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Canvas;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;

import com.example.myapplication.databinding.ActivityMainBinding;
import com.google.android.material.snackbar.Snackbar;
import com.skydoves.transformationlayout.TransformationCompat;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements clickAdapter, View.OnClickListener, SwipeAdapter {
    private static final String TAG = "jms8732";
    private ActivityMainBinding binding;
    private MusicService mService;
    private SharedPreferences pref;
    private MusicAdapter adapter;


    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String mode = intent.getStringExtra("mode");

            Log.d(TAG, "[Activity] mode: " + mode);
            switch (mode){
                case "start":
                    int pos = intent.getIntExtra("pos",0);
                    binding.recycler.scrollToPosition(pos);
                    invalidateStartView();
                    break;
                case "restart":
                    invalidateRestartView();
                    break;
                case "pause":
                    invalidatePauseView();
                    break;

                case "complete":
                    invalidateCompleteView();
                    break;
            }
        }
    };

    private ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "Service connected...");
            MusicService.MyBinder binder = (MusicService.MyBinder) service;
            mService = binder.getInstance();

            initSettings();
            List<Music> temp = loadMusicList();
            invalidate(temp);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "Service disconnected...");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        TransformationCompat.onTransformationStartContainer(this);
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        checkingPermission();
        registerReceiver(receiver, new IntentFilter("com.example.activity"));
    }

    //뷰 갱신
    private void invalidate(List<Music> musics) {
        MusicClickListener musicClickListener = new MusicClickListener(this);

        binding.setItem(musicClickListener);
        binding.recycler.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        adapter = new MusicAdapter(musics, musicClickListener);
        binding.recycler.setAdapter(adapter);
        binding.recycler.setHasFixedSize(true);
        binding.recycler.setNestedScrollingEnabled(false);

        ItemTouchHelper helper = new ItemTouchHelper(new SwipeHelper(this));
        helper.attachToRecyclerView(binding.recycler);

        int previous = pref.getInt("previous",0);
        binding.recycler.scrollToPosition(previous);

        binding.getMusic().setIsplaying(true);
        if (mService.isPlaying()) {
            invalidatePlay();
            binding.getMusic().setActivate(true);
        }else {
            invalidatePause();
        }
    }


    //음악 실행 뷰 갱신
    private void invalidateStartView(){
        binding.setMusic(mService.getMusic());
        binding.getMusic().setActivate(true);
        binding.getMusic().setIsplaying(true);

        invalidatePlay();
    }

    //음악 재실행 뷰 갱신
    private void invalidateRestartView(){
        binding.getMusic().setActivate(true);

        invalidatePlay();
    }

    //음악 일시정지 뷰 갱신
    private void invalidatePauseView(){
        binding.getMusic().setActivate(false);

        invalidatePause();
    }

    //음악 재생완료 뷰 갱신
    private void invalidateCompleteView(){
        binding.getMusic().setActivate(false);
        binding.getMusic().setIsplaying(false);
        binding.getMusic().setCurrentDuration(0);

        invalidatePlay();
    }

    @Override
    public void rawClick(Music next) {
        Log.d(TAG, "[Activity] rawClick...");
        String cur_id = binding.getMusic().getId();
        String next_id = next.getId();

        Log.d(TAG, cur_id + " vs " + next_id);
        if (cur_id.equals(next_id)) {
            //현재 선택한 음악이 동일 한 경우

            if (mService.isPlaying()) {
                //노래가 플레이 중이라면
                rawPause();
            } else {
                if (mService.isPrepared()) {
                    //노래가 이전에 준비가 되었더라면
                    rawRestart();
                } else {
                    rawStart(next);
                }
            }
        } else {
            //다른 노래인 경우
            binding.getMusic().setIsplaying(false);
            binding.getMusic().setActivate(false);
            rawStart(next);
        }
    }


    //노래 재생
    private void rawStart(Music music) {
        mService.start(music);
    }

    //노래 다시 재생
    private void rawRestart() {
        mService.restart();
    }

    //노래 일시 중지
    private void rawPause( ) {
        mService.pause();
    }

    //뷰 갱신
    private void invalidatePlay() {
        binding.playButton.setImageResource(R.drawable.pause_circle_out);
    }

    //뷰 갱신
    private void invalidatePause() {
        binding.playButton.setImageResource(R.drawable.play_circle_out);
    }

    //음악 리스트를 호출
    private List<Music> loadMusicList() {
        List<Music> ret = new ArrayList<>();
        String deletes = pref.getString("delete", null);

        String[] proj = new String[]{MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Artists.ARTIST,
                MediaStore.Audio.AudioColumns.DURATION,
                MediaStore.Audio.AudioColumns.DATA,
                MediaStore.Audio.Albums.ALBUM_ID};

        String selection = null;
        String[] selectionArgs = null;

        if (deletes != null) {
            String[] split = deletes.split(" ");
            selection = MediaStore.Audio.Media._ID + " not in (" + TextUtils.join(", ", split) + ")";
        }

        Cursor cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, proj, selection, selectionArgs, null);

        int index = 0;
        while (cursor.moveToNext()) {
            String id = cursor.getString(0);
            String title = cursor.getString(1);
            String artist = cursor.getString(2);
            int duration = cursor.getInt(3);
            String path = cursor.getString(4);
            long album = cursor.getLong(5);

            ret.add(new Music(title, artist, path, duration, album, id, false,index++));
        }

        int previous = pref.getInt("previous", 0);

        //초기 설정
        binding.setMusic(ret.get(previous));

        //서비스에 음악 등록
        mService.setMusic(ret.get(previous));
        mService.setMusicList(ret);

        return ret;
    }

    //초기 설정
    private void initSettings() {
        pref = getSharedPreferences("music", MODE_PRIVATE);
        binding.transformationLayout.setOnClickListener(this);
    }

    //권한 채크 메소드
    private void checkingPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            //권한 설정이 된 경우
            Log.d(TAG, "permission ok...");
            connectService();
        } else
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 101);
    }

    //서비스 연결
    private void connectService() {
        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, conn, 0);
        startService(intent);
    }

    @Override
    public void onClick(View v) {
        if (v == binding.transformationLayout) {
            Intent intent = new Intent(getApplicationContext(), DetailActivity.class);
            TransformationCompat.startActivity(binding.transformationLayout, intent);
        }
    }


    @Override
    public void swipeDelete(RecyclerView.ViewHolder viewHolder, int direction) {
       mService.removeMusic(viewHolder,direction,adapter);
    }


    @Override
    protected void onDestroy() {
        Log.d(TAG, "[Activity] onDestroy.....");
        super.onDestroy();
        unregisterReceiver(receiver);

        if(!mService.isPlaying()) {
            stopService(new Intent(this, MusicService.class));
            unbindService(conn);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "permission ok...");
                connectService();
            } else
                ActivityCompat.requestPermissions(this, new String[]{permissions[0]}, 101);
        }
    }
}