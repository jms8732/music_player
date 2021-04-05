package com.example.myapplication;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.TaskStackBuilder;
import androidx.databinding.DataBindingUtil;

import com.example.myapplication.databinding.ActivityDetailBinding;
import com.skydoves.transformationlayout.TransformationAppCompatActivity;
import com.skydoves.transformationlayout.TransformationCompat;

import org.jetbrains.annotations.Nullable;

public class DetailActivity extends TransformationAppCompatActivity implements View.OnClickListener {
    private static final String TAG = "jms8732";
    private ActivityDetailBinding binding;
    private MusicService mService;
    private AudioManager manager;
    private DurationThread thread;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String mode = intent.getStringExtra("mode");

            if (mode.equals("start") || mode.equals("restart")) {
                if (mode.equals("start"))
                    binding.setMusic(mService.getMusic());
                invalidatePlay();
            } else if (mode.equals("complete")) {
                invalidatePause();
            } else
                invalidatePause();
        }
    };


    private ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "[Detail Activity] connected.....");
            mService = ((MusicService.MyBinder) service).getInstance();

            init();
            invalidateView();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "[Detail Activity] onServiceDisconnected....");
        }
    };


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "[Detail Activity] onCreate...");
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_detail);
        registerReceiver(receiver, new IntentFilter("com.example.detailActivity"));

        connectService();
    }

    //초기 뷰 갱신
    private void invalidateView() {
        Music music = mService.getMusic();
        music.setCurrentDuration(mService.getCurrentPosition());

        binding.setMusic(music);
        binding.duration.setMax(music.getTotalDuration());
        binding.duration.setProgress(music.getCurrentDuration());

        if (mService.isPlaying())
            invalidatePlay();
        else
            invalidatePause();
    }

    private void init() {
        binding.play.setOnClickListener(this);
        binding.rewind.setOnClickListener(this);
        binding.forward.setOnClickListener(this);
        binding.shuffle.setOnClickListener(this);
        binding.repeat.setOnClickListener(this);

        if (mService.getShuffle())
            binding.shuffle.setImageResource(R.drawable.shuffle_activate);
        else
            binding.shuffle.setImageResource(R.drawable.shuffle_deactivate);

        switch (mService.getRepeat()) {
            case 0:
                binding.repeat.setImageResource(R.drawable.repeat_deactivate);
                break;
            case 1:
                binding.repeat.setImageResource(R.drawable.repeat_activate);
                break;
            case 2:
                binding.repeat.setImageResource(R.drawable.repeat_one_activate);
                break;
        }

        binding.detailTitle.setSelected(true);
        binding.detailAlbum.getRootView().setOnTouchListener(new OnSwipeTouchListener(getApplicationContext()) {
            @Override
            public void onSwipeBottom() {
                onBackPressed(); //액티비티 종료
            }
        });

        if (manager == null)
            manager = (AudioManager) getSystemService(AUDIO_SERVICE);

        binding.volume.setProgress(manager.getStreamVolume(AudioManager.STREAM_MUSIC));
        binding.volume.setMax(manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        binding.volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    manager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, AudioManager.FLAG_SHOW_UI);
                    mService.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        binding.duration.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    seekBar.setProgress(progress);
                    mService.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mService.getMusic().setCurrentDuration(seekBar.getProgress());
                mService.seekTo(seekBar.getProgress());
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //볼륨 조절
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            int vol = manager.getStreamVolume(AudioManager.STREAM_MUSIC);

            if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
                --vol;
            else
                ++vol;

            binding.volume.setProgress(vol);
            manager.setStreamVolume(AudioManager.STREAM_MUSIC, vol, AudioManager.FLAG_SHOW_UI);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    //음악 실행 뷰 갱신(멈춤)
    private void invalidatePause() {
        if (thread != null)
            thread.requestStop();
        binding.play.setImageResource(R.drawable.play);
    }

    //음악 실행 뷰 갱신(실행)
    private void invalidatePlay() {
        thread = new DurationThread();
        thread.setMusic(mService.getMusic());
        thread.start();
        binding.play.setImageResource(R.drawable.pause);
    }

    //서비스 연결
    private void connectService() {
        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, conn, 0);
        startService(intent);
    }

    @Override
    public void onClick(View v) {
        if (v == binding.play) {
            String cur_id = binding.getMusic().getId();
            String next_id = mService.getMusic().getId();

            Log.d(TAG, cur_id + " vs " + next_id);

            if (mService.isPlaying()) {
                //노래가 플레이 중이라면
                mService.pause();
            } else {
                if (mService.isPrepared()) {
                    //노래가 이전에 준비가 되었더라면
                    mService.restart();
                } else {
                    mService.start(mService.getMusic());
                }
            }
        } else if (v == binding.forward) {
            mService.forward();
            binding.setMusic(mService.getMusic());
        } else if (v == binding.rewind) {
            mService.rewind();
            binding.setMusic(mService.getMusic());
        } else if (v == binding.shuffle) {
            mService.setShuffle(!mService.getShuffle());

            if (mService.getShuffle()) {
                binding.shuffle.setImageResource(R.drawable.shuffle_activate);
            } else
                binding.shuffle.setImageResource(R.drawable.shuffle_deactivate);
        } else if (v == binding.repeat) {
            int val = (mService.getRepeat() + 1) % 3;
            mService.setRepeat(val);

            switch (val) {
                case 0:
                    binding.repeat.setImageResource(R.drawable.repeat_deactivate);
                    Toast.makeText(this, "반복 해제", Toast.LENGTH_SHORT).show();
                    break;
                case 1:
                    binding.repeat.setImageResource(R.drawable.repeat_activate);
                    Toast.makeText(this, "전체 곡 반복", Toast.LENGTH_SHORT).show();
                    break;
                case 2:
                    binding.repeat.setImageResource(R.drawable.repeat_one_activate);
                    Toast.makeText(this, "현재 곡 반복", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

    private class DurationThread extends Thread {
        private Music music;
        private boolean stop;

        public DurationThread() {
            stop = false;
        }

        public void setMusic(Music music) {
            this.music = music;
        }

        public void requestStop() {
            stop = true;
        }

        @Override
        public void run() {
            stop = false;
            Log.d(TAG, "[Detail Activity] Start.....");
            while (!stop) {
                music.setCurrentDuration(mService.getCurrentPosition());
                SystemClock.sleep(1000);
            }

            Log.d(TAG, "[Detail Activity] Stop.....");
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(conn);
        unregisterReceiver(receiver);

        if (thread != null)
            thread.requestStop(); //액티비티가 종료되면 스레드 멈춤
        thread = null;
    }
}
