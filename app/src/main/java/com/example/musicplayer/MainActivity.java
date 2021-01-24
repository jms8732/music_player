package com.example.musicplayer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.Manifest;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Toast;

import com.example.musicplayer.databinding.ActivityMainBinding;
import com.skydoves.transformationlayout.TransformationCompat;

import javax.crypto.Mac;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private final String TAG = "jms8732";
    private ActivityMainBinding binding;
    private MusicService mService;
    private long pressedTime = 0;
    private AudioManager manager;

    private ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected");
            mService = ((MusicService.MyBinder) service).getService();

            connection();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();

        if (!checkServiceAlive()) {
            Intent intent = new Intent(this, MusicService.class);
            stopService(intent);
            unbindService(conn);
        }
    }

    private boolean checkServiceAlive() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (MusicService.class.getName().equals(service.service.getClassName())) {
                //현재 foreground 서비스가 진행되고 있는 경우
                if (service.foreground) {
                    Log.d(TAG, MusicService.class.getName());
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        checkPermission();
    }

    //서비스 연결
    private void startConnection() {
        Intent intent = new Intent(this, MusicService.class);
        startService(intent);
        bindService(intent, conn, 0);
    }

    private void connection() {
        Log.d(TAG, "connection");
        ItemTouchHelper.Callback callback = new ItemMoveCallback(mService);
        ItemTouchHelper helper = new ItemTouchHelper(callback);
        helper.attachToRecyclerView(binding.primaryRecyclerview);

        MacroListener listener = new MacroListener(mService);
        MusicViewModel model = mService.getModel();
        MusicAdapter adapter = new MusicAdapter(listener, model, helper);

        binding.setListener(listener);
        binding.setModel(model);
        binding.setLifecycleOwner(this);
        binding.primaryRecyclerview.setHasFixedSize(true);
        binding.primaryRecyclerview.setNestedScrollingEnabled(false);
        binding.primaryRecyclerview.setLayoutManager(new LinearLayoutManager(getApplicationContext()));

        DividerItemDecoration decoration = new DividerItemDecoration(getApplicationContext(), DividerItemDecoration.VERTICAL);
        decoration.setDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.custom_divider, null));

        binding.primaryDetail.setListener(listener);
        binding.primaryDetail.setModel(model);
        binding.primaryThumbnail.setOnClickListener(this);

        binding.primaryRecyclerview.addItemDecoration(decoration);
        binding.primaryRecyclerview.setAdapter(adapter);
        mService.setAdapter(adapter);

        binding.primaryDetail.getRoot().setOnTouchListener(new OnSwipeTouchListener(getApplicationContext()) {
            @Override
            public void onSwipeBottom() {
                if (binding.primaryThumbnail.isTransformed())
                    binding.primaryThumbnail.finishTransform();
            }
        });

        seekViewSettings();
    }

    private void seekViewSettings() {
        manager = (AudioManager) getSystemService(AUDIO_SERVICE);
        binding.primaryDetail.speaker.setProgress(manager.getStreamVolume(AudioManager.STREAM_MUSIC));
        binding.primaryDetail.speaker.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser)
                    manager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, AudioManager.FLAG_SHOW_UI);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        binding.primaryDetail.musicProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    mService.seekTo(progress, false);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mService.seekTo(seekBar.getProgress(), true);
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            int vol = manager.getStreamVolume(AudioManager.STREAM_MUSIC);

            if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
                --vol;
            else
                ++vol;

            binding.primaryDetail.speaker.setProgress(vol);
            manager.setStreamVolume(AudioManager.STREAM_MUSIC, vol, AudioManager.FLAG_SHOW_UI);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }


    @Override
    public void onClick(View v) {
        if (v == binding.primaryThumbnail)
            binding.primaryThumbnail.startTransform();
    }

    //퍼미션 체크
    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.FOREGROUND_SERVICE) == PackageManager.PERMISSION_GRANTED)
            startConnection();
        else
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.FOREGROUND_SERVICE}, 101);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 101) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED)
                startConnection();
        }
    }

    @Override
    public void onBackPressed() {
        if (binding.primaryThumbnail.isTransformed())
            binding.primaryThumbnail.finishTransform();
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


}