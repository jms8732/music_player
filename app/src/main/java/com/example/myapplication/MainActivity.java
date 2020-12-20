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
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.skydoves.transformationlayout.TransformationLayout;

import java.io.FileDescriptor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, onStartDragListener {
    public static RecyclerView recyclerView;
    private TextView thumbnail_title, thumbnail_artist, music_title, music_artist, current_duration, total_duration;
    private ImageView below_thumbnail, thumbnail_play, play, music_image, fast_rewind, fast_forward, repeat, loop;
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
    private boolean isService, isCreate;
    private final int SEND_INFO = 1, SEND_STOP = 2;
    private ItemTouchHelper touchHelper = null;
    private RelativeLayout recycler_parent;

    private BroadcastReceiver serviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String id = intent.getStringExtra("id");
            int code = intent.getIntExtra("code", -1);

            if(code == 0) {
                //음악을 선택하여 실행한 경우
                makeMusicView(id);
            }else if (code == 1 || code == 2) {
                //1 인 경우, 음악을 잠시 멈춘다, 2 인 경우, 음악을 재실행
                boolean check = true;
                if(code == 1)
                    check = false;

                changePlayButton(check);
            } else if (code == 3) {
                //현재 진행중인 음악을 삭제한 경우
                mService.pauseMusic();
                belowMusicMenu.setVisibility(View.GONE);

                mService.stopForeground(true);
            }
        }
    };

    private ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            mService = binder.getService();

            log("Service connected...");
            String id = mService.getId();
            if (isCreate) { //액티비티가 새롭게 생성될 경우
                init(id);
                isCreate = false;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            log("Service is disconnected...");
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 101:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    log("granted....");
                    Intent intent = new Intent(this, MusicService.class);
                    bindService(intent, conn, BIND_AUTO_CREATE);
                    registerReceiver(serviceReceiver, new IntentFilter("com.example.activity"));
                }
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        log("onCreate...");
        registerReceiver(serviceReceiver, new IntentFilter("com.example.activity"));

        isCreate = true;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        log("onDestroy...");
        isCreate = false;
        unregisterReceiver(serviceReceiver);
    }
    //뷰 세팅
    private void init(String id) {
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

        thumbnail_play = (ImageView) findViewById(R.id.play);
        thumbnail_play.setOnClickListener(this);

        thumbnail_title = (TextView) findViewById(R.id.thumbnail_title);
        thumbnail_artist = (TextView) findViewById(R.id.thumbnail_artist);
        belowMusicMenu = (LinearLayout) findViewById(R.id.belowMusicMenu);
        below_thumbnail = (ImageView)findViewById(R.id.below_thumbnail);
        transformationLayout = (TransformationLayout) findViewById(R.id.transformation_layout);
        recycler_parent = (RelativeLayout)findViewById(R.id.recycler_parent);

        belowMusicMenu.setOnClickListener(this);

        if(recyclerView == null) {
            recyclerView = (RecyclerView) findViewById(R.id.recycle_view);
            // recyclerView.addItemDecoration(new ItemDecoration(getApplicationContext(),R.drawable.recyclerview_divider));
            recyclerView.setNestedScrollingEnabled(false);

            adapter = new MusicRecyclerAdapter(this, recyclerView, this);

            if (touchHelper == null)
                touchHelper = new ItemTouchHelper(new ItemMoveCallback(adapter));

            ArrayList<String> list = mService.getMusicList();
            touchHelper.attachToRecyclerView(recyclerView);
            adapter.setList(list);

            recyclerView.setAdapter(adapter);
            recyclerView.setNestedScrollingEnabled(false);

            recyclerView.setHasFixedSize(true);
            recyclerView.setItemViewCacheSize(list.size());
            recyclerView.setDrawingCacheEnabled(true);
            recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);

            PreCachingLayoutManager pclm = new PreCachingLayoutManager(this, list.size());
            recyclerView.setLayoutManager(pclm);

        }else {
            //todo 개선 필
            ViewGroup vg = (ViewGroup)recyclerView.getParent();
            vg.removeView(recyclerView);
            recycler_parent.addView(recyclerView);
        }
        setLayout(id);
    }

    @Override
    protected void onStart() {
        super.onStart();
        log("start....");

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(this, MusicService.class);
            bindService(intent, conn, 0);
            startService(intent);
        } else
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 101);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        log("onRestart...");
    }

    @Override
    protected void onResume() {
        super.onResume();
        log("onResume...");
    }

    @Override
    protected void onPause() {
        super.onPause();
        log("onPause...");
    }

    @Override
    protected void onStop() {
        super.onStop();
        log("onStop....");


        handler.sendEmptyMessage(SEND_STOP);

        if (isServiceAlive()) {
            Intent intent = new Intent(this, MusicService.class);
            stopService(intent);
            unbindService(conn);
        }
    }

    private boolean isServiceAlive() {
        ActivityManager am = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> l = am.getRunningServices(50);
        Iterator<ActivityManager.RunningServiceInfo> i = l.iterator();
        while (i.hasNext()) {
            ActivityManager.RunningServiceInfo runningServiceInfo = i
                    .next();

            if (runningServiceInfo.service.getClassName().equals(MusicService.class.getName())) {
                if (!runningServiceInfo.foreground) {
                    log("stop foreground in activity...");
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        touchHelper.startDrag(viewHolder);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        int vol = manager.getStreamVolume(AudioManager.STREAM_MUSIC);
        if(keyCode == KeyEvent.KEYCODE_VOLUME_DOWN | keyCode == KeyEvent.KEYCODE_VOLUME_UP){
            if(keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
                vol -=1;
            else
                vol += 1;

            if(vol >= 0 && vol <= manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)){
                manager.setStreamVolume(AudioManager.STREAM_MUSIC,vol,AudioManager.FLAG_SHOW_UI | AudioManager.FLAG_PLAY_SOUND);
                speaker.setProgress(vol);
            }

            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    //확장된 레이아웃 세팅
    private void setLayout(String id) {
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
        loop = (ImageView) music_detail.findViewById(R.id.loop);
        loop.setOnClickListener(this);

        if (mService.getOrderStatus())
            repeat.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.repeat, null));
        else
            repeat.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.shuffle, null));

        if (mService.getLoopStatus())
            loop.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.loop_activate, null));
        else
            loop.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.loop_deactivate, null));

        current_duration = (TextView) music_detail.findViewById(R.id.current_duration);
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
                manager.setStreamVolume(AudioManager.STREAM_MUSIC, progress,AudioManager.FLAG_SHOW_UI | AudioManager.FLAG_PLAY_SOUND);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        if (id != null) { //이전에 한번이라도 튼 경우
            makeMusicView(id);
        }

        log("setLayout...");
    }

    //하단 음악 플레이어 뷰를 구성하는 메소드
    private void makeMusicView(String id) {
        String title = MusicSearcher.findDisplayName(this, id);
        String artist = MusicSearcher.findArtist(this, id);
        int duration = MusicSearcher.findDuration(this, id);
        int albumId = MusicSearcher.findAlbumId(this, id);

        belowMusicMenu.setVisibility(View.VISIBLE);
        thumbnail_title.setText(title);
        thumbnail_title.setSelected(true); //marquee 진행
        thumbnail_artist.setText(artist);

        Bitmap bm = getAlbumart(albumId);

        if(bm == null)
            below_thumbnail.setImageDrawable(ResourcesCompat.getDrawable(getResources(),R.drawable.album_white,null));
        else
            below_thumbnail.setImageBitmap(bm);

        music_title.setText(title);
        music_artist.setText(artist);
        total_duration.setText(convertDuration(duration));
        current_duration.setText(convertDuration(mService.getCurrentProgress()));

        music_progress.setMax(MusicSearcher.findDuration(this, id));
        music_progress.setProgress(mService.getCurrentProgress());

        Glide.with(this)
                .load(getAlbumart(albumId))
                .placeholder(R.drawable.album)
                .into(music_image);

        changePlayButton(mService.isPlaying());
    }

    private void changePlayButton(boolean p) {
        if (p) {
            //노래가 실행 중
            play.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.remote_pause, null));
            thumbnail_play.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.remote_pause, null));
            thread = new MusicThread();
            thread.start();
        } else {
            //노래가 멈춤
            play.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.remote_play, null));
            thumbnail_play.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.remote_play, null));
            handler.sendEmptyMessage(SEND_STOP);
        }
    }

    @Override
    public void onClick(View v) {
        if (v == play || v == thumbnail_play) {
            Intent intent = new Intent(this, MusicService.class);
            intent.putExtra("code", 1);
            intent.putExtra("data", mService.getId());

            bindService(intent, conn, 0);

            if (Build.VERSION_CODES.O <= Build.VERSION.SDK_INT) {
                startForegroundService(intent);
            } else
                startService(intent);

        } else if (v == belowMusicMenu) {
            if (!transformationLayout.isTransformed()) {
                transformationLayout.startTransform();
            } else {
                finishTransform();
            }
        } else if (v == fast_forward || v == fast_rewind) {
            boolean check = true;
            if (v == fast_rewind)
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

            mService.makeMusicOrder(mService.getId(), !status);
        } else if (v == loop) {
            boolean status = mService.getLoopStatus();
            if (!status) {
                Toast.makeText(this, "현재 곡 반복 재생", Toast.LENGTH_SHORT).show();
                loop.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.loop_activate, null));
            } else {
                Toast.makeText(this, "전체 곡 재생", Toast.LENGTH_SHORT).show();
                loop.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.loop_deactivate, null));
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