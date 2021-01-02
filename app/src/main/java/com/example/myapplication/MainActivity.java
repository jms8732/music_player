package com.example.myapplication;

import android.Manifest;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.os.SystemClock;
import android.text.Layout;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapplication.databinding.ActivityMainBinding;
import com.skydoves.transformationlayout.TransformationLayout;

import java.io.FileDescriptor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "jms8732";
    private ActivityMainBinding binding;
    private MusicService mService;
    private long pressedTime = 0;
    private AudioManager audioManager;
    private MainViewModel viewModel;
    private HandleListener handleListener;
    private ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            log("Service connected...");
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            mService = binder.getService();

            initialLayoutSetting();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            log("Service disconnected....");
        }
    };
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        log("onCreate...");
        super.onCreate(savedInstanceState);
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        binding.setLifecycleOwner(this);
        binding.recyclerView.setHasFixedSize(true);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));

        viewModel = new ViewModelProvider(this, new ViewModelProvider.AndroidViewModelFactory(getApplication())).get(MainViewModel.class);
        binding.musicDetail.getRoot().setOnTouchListener(new OnSwipeTouchListener(getApplicationContext()) {
            @Override
            public void onSwipeBottom() {
                if (binding.thumbnailShow.isTransformed())
                    binding.thumbnailShow.finishTransform();
            }
        });

        binding.musicDetail.speaker.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
        binding.musicDetail.speaker.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser)
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, AudioManager.FLAG_SHOW_UI);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        binding.musicDetail.speaker.setMax(audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        binding.thumbnailMusicTitle.setSelected(true);
        viewModel.setSpeaker(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            //퍼미션이 허가 된 경우
            connectionService();
        } else
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 101);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            int vol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

            if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
                --vol;
            else
                ++vol;

            viewModel.setSpeaker(vol);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, vol, AudioManager.FLAG_SHOW_UI);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onStart() {
        super.onStart();
        log("onStart...");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        log("onRestart...");
    }

    @Override
    protected void onStop() {
        super.onStop();
        log("onStop...");
    }

    @Override
    protected void onPause() {
        super.onPause();
        log("onPause...");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        log("onDestroy...");
        unbindService(conn);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 101:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //퍼미션 허가된 경우
                    connectionService();
                }
        }
    }

    //서비스와 연결 진행
    private void connectionService() {
        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, conn, 0);
        startService(intent);
    }

    //레이아웃 초기 세팅
    private void initialLayoutSetting(){
        mService.setInnerListener(viewModel);

        handleListener = new HandleListener(mService);
        Adapter adapter = new Adapter(getApplicationContext(), mService.getMusicList(), handleListener, viewModel);
        binding.recyclerView.setAdapter(adapter);
        binding.recyclerView.setItemViewCacheSize(mService.getMusicList().size());
        binding.setHandler(handleListener);

        binding.setViewModel(viewModel);
        binding.musicDetail.setViewModel(viewModel);
        binding.musicDetail.setHandler(handleListener);

        binding.musicDetail.musicProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mService.seekTo(seekBar.getProgress());
            }
        });

    }

    @Override
    public void onBackPressed() {
        if (binding.thumbnailShow.isTransformed())
            binding.thumbnailShow.finishTransform();
        else {
            if (pressedTime == 0) {
                Toast.makeText(this, "한 번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT).show();
                pressedTime = System.currentTimeMillis();
            } else {
                long end = System.currentTimeMillis();
                if (end - pressedTime > 2000) { //누른지 2초가 지난 후,
                    Toast.makeText(this, "한 번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT).show();
                    pressedTime = end;
                } else {
                    super.onBackPressed();
                    finish();
                }
            }
        }
    }

    private void log(String s) {
        Log.d(TAG, "[Activity] " + s);
    }
}