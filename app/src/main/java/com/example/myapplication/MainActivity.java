package com.example.myapplication;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.myapplication.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "jms8732";
    private ActivityMainBinding binding;
    private long pressedTime = 0;
    private AudioManager audioManager;
    private ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            log("Service connected...");
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            initialLayoutSetting(binder.getService());
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
        binding.recyclerView.setNestedScrollingEnabled(false);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));

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

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            //퍼미션이 허가 된 경우
            connectionService();
        } else
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 101);
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
                boolean check = true;

                for (int i = 0; i < grantResults.length; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        check = false;
                        break;
                    }
                }

                if (check)
                    connectionService();
                break;
        }
    }

    //서비스와 연결 진행
    private void connectionService() {
        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, conn, 0);
        startService(intent);
    }

    //레이아웃 초기 세팅
    private void initialLayoutSetting(final MusicService mService) {
        ItemTouchHelper.Callback callback = new ItemMoveCallback(mService);
        ItemTouchHelper helper = new ItemTouchHelper(callback);
        helper.attachToRecyclerView(binding.recyclerView);

        final MusicViewModel musicViewModel = mService.getMusicViewModel();
        HandleListener handleListener = mService.getHandleListener();
        Adapter adapter = mService.getAdapter();

        handleListener.setItemTouchHelper(helper);

        binding.recyclerView.setAdapter(adapter);
        //binding.recyclerView.setItemViewCacheSize(2);
        binding.setHandler(handleListener);

        binding.setMusicView(musicViewModel);
        binding.musicDetail.setMusicView(musicViewModel);
        binding.musicDetail.setHandler(handleListener);

        musicViewModel.setSpeaker(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
        binding.musicDetail.musicProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser)
                    musicViewModel.setProgressDuration(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mService.seekTo(seekBar.getProgress());
            }
        });

        binding.musicDetail.speaker.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser)
                    musicViewModel.setSpeaker(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                musicViewModel.setSpeaker(seekBar.getProgress());
            }
        });

    }

    //todo 개선필
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            int vol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

            if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
                --vol;
            else
                ++vol;


            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, vol, AudioManager.FLAG_SHOW_UI);
            return true;
        }
        return super.onKeyDown(keyCode, event);
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