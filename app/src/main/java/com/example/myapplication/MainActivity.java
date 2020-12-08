package com.example.myapplication;

import android.Manifest;
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
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.skydoves.transformationlayout.TransformationLayout;

import java.io.FileDescriptor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private ArrayList<String> musics = new ArrayList<>();
    private RecyclerView recyclerView;
    private TextView status_show, thumbnail_title, thumbnail_artist, music_title, music_artist, current_duration, total_duration;
    private ImageView thumbnail_play, play, music_image, fast_rewind, fast_forward, repeat, loop;
    private MusicRecyclerAdapter adapter;
    private LinearLayout belowMusicMenu;
    private TransformationLayout transformationLayout;
    private SeekBar music_progress, speaker;
    private AudioManager manager;
    private View music_detail;
    private long pressedTime;
    private MusicService mService;
    private Handler handler = null;
    private MusicThread thread;
    private boolean isService;
    private final int SEND_INFO = 1, SEND_STOP = 2;

    private BroadcastReceiver serviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int position = intent.getIntExtra("pos", -1);
            int code = intent.getIntExtra("code", -1);

            if (code == 1) {
                boolean s = intent.getBooleanExtra("status", false);
                changePlayButton(s);
            } else {
                makeMusicView(position);
            }

        }
    };

    private ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            mService = binder.getService();
            isService = true;

            log("Service connected...");

            mService.setList(musics); //현재 노래 곡 세팅
            int position = mService.getPosition();
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                init(position);
            } else
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 101);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isService = false;
            log("unconnected..");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, conn, BIND_AUTO_CREATE);
        registerReceiver(serviceReceiver, new IntentFilter("com.example.service"));

        handler = new Handler() {
            @Override
            public void handleMessage(@NonNull Message msg) {
                switch (msg.what) {
                    case SEND_INFO:
                        current_duration.setText(convertDuration(msg.arg1));
                        music_progress.setProgress(msg.arg1);
                        break;

                    case SEND_STOP:
                        if (thread != null)
                            thread.stopThread();
                        break;
                }
            }
        };


        manager = (AudioManager) getSystemService(AUDIO_SERVICE);

        recyclerView = (RecyclerView) findViewById(R.id.recycle_view);

        thumbnail_play = (ImageView) findViewById(R.id.play);
        thumbnail_play.setOnClickListener(this);

        status_show = (TextView) findViewById(R.id.status_show);
        thumbnail_title = (TextView) findViewById(R.id.thumbnail_title);
        thumbnail_artist = (TextView) findViewById(R.id.thumbnail_artist);
        belowMusicMenu = (LinearLayout) findViewById(R.id.belowMusicMenu);
        transformationLayout = (TransformationLayout) findViewById(R.id.transformation_layout);
        belowMusicMenu.setOnClickListener(this);

        log("onCreate...");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        log("onDestroy...");

        unregisterReceiver(serviceReceiver);
        handler.sendEmptyMessage(SEND_STOP);
    }


    //뷰 세팅
    private void init(int position) {
        adapter = new MusicRecyclerAdapter(this, recyclerView);

        //todo id 저장
        searchMusicPath();

        if (!musics.isEmpty()) {
            status_show.setVisibility(View.GONE);
            adapter.setList(musics);

            recyclerView.setAdapter(adapter);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
        }

        setLayout(position);

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        int vol = manager.getStreamVolume(AudioManager.STREAM_MUSIC);
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            manager.setStreamVolume(AudioManager.STREAM_MUSIC, vol - 1, AudioManager.FLAG_PLAY_SOUND);
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            manager.setStreamVolume(AudioManager.STREAM_MUSIC, vol + 1, AudioManager.FLAG_PLAY_SOUND);
        }

        vol = manager.getStreamVolume(AudioManager.STREAM_MUSIC);
        speaker.setProgress(vol);
        return super.onKeyDown(keyCode, event);
    }

    //확장된 레이아웃 세팅
    private void setLayout(int position) {
        music_detail = findViewById(R.id.targetView);
        setLayoutHeight();

        music_detail.setOnTouchListener(new OnSwipeTouchListener(this) {
            @Override
            public void onSwipeBottom() {
                if (transformationLayout.isTransformed())
                    finishTransform();
            }
        });

        play = (ImageView) music_detail.findViewById(R.id.detail_play);
        play.setOnClickListener(this);
        music_title = (TextView) music_detail.findViewById(R.id.music_title);
        music_title.setSelected(true);
        music_artist = (TextView) music_detail.findViewById(R.id.artist);
        music_artist.setSelected(true);
        fast_forward = (ImageView) music_detail.findViewById(R.id.fast_forward);
        fast_forward.setOnClickListener(this);
        fast_rewind = (ImageView) music_detail.findViewById(R.id.fast_rewind);
        fast_rewind.setOnClickListener(this);
        repeat = (ImageView) music_detail.findViewById(R.id.repeat);
        repeat.setOnClickListener(this);
        loop = (ImageView)music_detail.findViewById(R.id.loop);
        loop.setOnClickListener(this);

        if (mService.getOrderStatus())
            repeat.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.repeat, null));
        else
            repeat.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.shuffle, null));

        if(mService.getLoopStatus())
            loop.setImageDrawable(ResourcesCompat.getDrawable(getResources(),R.drawable.loop_activate,null));
        else
            loop.setImageDrawable(ResourcesCompat.getDrawable(getResources(),R.drawable.loop_deactivate,null));

        current_duration = (TextView) music_detail.findViewById(R.id.current_duration);
        current_duration.setText(convertDuration(0));

        total_duration = (TextView) music_detail.findViewById(R.id.total_duration);
        music_image = (ImageView) music_detail.findViewById(R.id.music_image);
        speaker = (SeekBar) music_detail.findViewById(R.id.speaker);

        music_progress = (SeekBar) music_detail.findViewById(R.id.music_progress);
        music_progress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                current_duration.setText(convertDuration(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mService.seekTo(seekBar.getProgress());
            }
        });

        int vol = manager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int max = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        speaker.setProgress(vol);
        speaker.setMax(max);
        speaker.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                manager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, AudioManager.FLAG_PLAY_SOUND);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        if (position != -1) { //이전에 한번이라도 튼 경우
            makeMusicView(position);
        }

        log("setLayout...");
    }

    //하단 음악 플레이어 뷰를 구성하는 메소드
    private void makeMusicView(int position) {
        String id = musics.get(position);
        String title = MusicSearcher.findDisplayName(this, id);
        String artist = MusicSearcher.findArtist(this, id);
        int duration = MusicSearcher.findDuration(this, id);
        int albumId = MusicSearcher.findAlbumId(this, id);

        belowMusicMenu.setVisibility(View.VISIBLE);
        thumbnail_title.setText(title);
        thumbnail_title.setSelected(true); //marquee 진행
        thumbnail_artist.setText(artist);

        music_title.setText(title);
        music_artist.setText(artist);
        total_duration.setText(convertDuration(duration));

        music_progress.setMax(duration);

        Glide.with(this)
                .load(getAlbumart(albumId))
                .fitCenter()
                .placeholder(R.drawable.ic_launcher_foreground)
                .into(music_image);

        changePlayButton(mService.isPlaying());
    }

    private void changePlayButton(boolean p) {
        if (p) {
            //노래가 실행 중
            play.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.pause, null));
            thumbnail_play.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.pause, null));
            thread = new MusicThread();
            thread.start();
        } else {
            //노래가 멈춤
            play.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.play, null));
            thumbnail_play.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.play, null));
            handler.sendEmptyMessage(SEND_STOP);
        }
    }

    @Override
    public void onClick(View v) {
        if (v == play || v == thumbnail_play) {
            Intent intent = new Intent(this, MusicService.class);
            intent.putExtra("code", 1);
            intent.putExtra("data", musics.get(mService.getPosition()));
            intent.putExtra("current", mService.getPosition());

            if (Build.VERSION_CODES.O <= Build.VERSION.SDK_INT) {
                startForegroundService(intent);
            } else
                startService(intent);

            changePlayButton(!mService.isPlaying());
        } else if (v == belowMusicMenu) {
            if (!transformationLayout.isTransformed()) {
                transformationLayout.startTransform();
            } else {
                finishTransform();
            }
        } else if (v == fast_forward || v == fast_rewind) {
            boolean check = true;
            if(v == fast_rewind)
                check = false;

            moveMusic(check);
        } else if (v == repeat) {
            boolean status = mService.getOrderStatus();
            if (!status) {
                Toast.makeText(this, "순차 재생", Toast.LENGTH_SHORT).show();
                repeat.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.repeat, null));
            } else {
                Toast.makeText(this, "랜덤 재생", Toast.LENGTH_SHORT).show();
                repeat.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.shuffle, null));
            }

            mService.makeMusicOrder(mService.getPosition(), !status);
        } else if( v== loop){
            boolean status = mService.getLoopStatus();
            if(!status){
                Toast.makeText(this, "현재 곡 반복 재생", Toast.LENGTH_SHORT).show();
                loop.setImageDrawable(ResourcesCompat.getDrawable(getResources(),R.drawable.loop_activate,null));
            }else{
                Toast.makeText(this, "전체 곡 재생", Toast.LENGTH_SHORT).show();
                loop.setImageDrawable(ResourcesCompat.getDrawable(getResources(),R.drawable.loop_deactivate,null));
            }

            log("loop : " + !status);
            mService.setLoopStatus(!status);
        }
    }

    private void moveMusic(boolean b) {
        current_duration.setText(convertDuration(0));
        music_progress.setProgress(0);
        mService.moveMusic(b);
        handler.sendEmptyMessage(SEND_STOP);
    }

    @Override
    public void onBackPressed() {
        if (transformationLayout.isTransformed())
            finishTransform();
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

    //todo scroll 변경
    private void finishTransform() {
        transformationLayout.finishTransform();
    }

    private void log(String s) {
        Log.d("jms8732", s);
    }

    //안드로이드 내에 존재하는 파일들을 탐색하여 확장자 mp3를 가진 파일들을 찾는 메소드
    private void searchMusicPath() {
        Cursor cursor = MusicSearcher.findId(this);

        while (cursor != null && cursor.moveToNext()) {
            musics.add(cursor.getString(0));
        }

        //마지막에 '맨 위로' 처리
        musics.add(null);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 101) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                int position = mService.getPosition();
                init(position);
            }
        }
    }


    //확장한 회면 높이 조절
    private void setLayoutHeight() {
        ViewGroup.LayoutParams params = music_detail.getLayoutParams();
        Point size = new Point();
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        display.getRealSize(size);
        params.height = size.y;
    }

    //1000 millisec = 1sec;
    //todo total Duration 올림 표기
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

        if (bm == null)
            bm = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher_foreground);

        return bm;
    }

    class MusicThread extends Thread {
        boolean stopped = false;

        public void stopThread() {
            stopped = true;
        }

        @Override
        public void run() {
            log("Thread start...");
            while (!stopped) {
                Message message = handler.obtainMessage();
                message.what = SEND_INFO;
                message.arg1 = mService.getCurrentProgress();

                handler.sendMessage(message);
                SystemClock.sleep(1000);

            }
            log("Thread end...");
        }
    }

}