package com.example.myapplication;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

public class MusicService extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, SwipeControllerActions {
    private static final String TAG = "jms8732";
    private Deque<Music> playList;
    private MediaPlayer mPlayer;
    private boolean isPrepared, shuffle;
    private List<Music> musicList;
    private Music music;
    private int count, repeat;
    private NotificationManager notificationManager;
    private SharedPreferences prefs;
    private final String channelId = "musicChannel";
    private AudioManager audioManager;

    private MediaButtonReceiver mediaButtonReceiver;
    private HeadPhonePlugReceiver headPhonePlugReceiver;
    private BroadcastReceiver actionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String mode = intent.getStringExtra("mode");
            Log.d(TAG, "[Service] onReceive: " + mode);

            switch (mode) {
                case "play":
                    if(mPlayer.isPlaying())
                        pause();
                    else
                        restart();
                    break;
                case "rewind":
                    rewind();
                    break;
                case "forward":
                    forward();
                    break;
                case "close":
                    stopForeground(true);
                    if (!checkActivityAlive()) {
                        //액티비티가 Task에 존재하지 않는 경우
                        stopSelf();
                    } else {
                        mPlayer.pause();
                        invalidatePauseView();
                    }
                    break;
            }
        }
    };

    //엑티비티가 살아있는 지 판단하는 메소드
    private boolean checkActivityAlive() {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> tasks = activityManager.getRunningTasks(Integer.MAX_VALUE);

        for (ActivityManager.RunningTaskInfo task : tasks) {
            if (getPackageName().equalsIgnoreCase(task.baseActivity.getPackageName()))
                return true;
        }

        return false;
    }

    @Override
    public void onLeftClicked(int position) {
        Log.d(TAG, "onLeftClicked: " + position);
    }

    @Override
    public void onRightClicked(int position) {
        Log.d(TAG, "onRightClicked: " + position);
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "[Service] onCreate.....");
        super.onCreate();

        if (mPlayer == null) {
            mPlayer = new MediaPlayer();
            mPlayer.setOnErrorListener(this);
            mPlayer.setOnPreparedListener(this);
            mPlayer.setOnCompletionListener(this);
        }

        if (notificationManager == null) {
            notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            createNotificationChannel("MusicChannel1");
        }

        if (audioManager == null)
            audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        if (prefs == null)
            prefs = getSharedPreferences("music", MODE_PRIVATE);

        registerReceiver(actionReceiver, new IntentFilter("com.example.service"));

        IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);

        registerReceivers();

        SharedPreferences prefs = getSharedPreferences("music", MODE_PRIVATE);
        shuffle = prefs.getBoolean("shuffle", true);
        repeat = prefs.getInt("repeat", 0);

        isPrepared = false;
    }

    private void registerReceivers() {
        registerReceiver(actionReceiver, new IntentFilter("com.example.service"));

        IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);

        headPhonePlugReceiver = new HeadPhonePlugReceiver();
        registerReceiver(headPhonePlugReceiver, filter);

        audioManager.registerMediaButtonEventReceiver(new ComponentName(getApplicationContext(), MediaButtonReceiver.class));

        mediaButtonReceiver = new MediaButtonReceiver();

        filter = new IntentFilter(Intent.ACTION_MEDIA_BUTTON);
        registerReceiver(mediaButtonReceiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "[Service] onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    public boolean isPlaying() {
        return mPlayer.isPlaying();
    }

    public boolean isPrepared() {
        return isPrepared;
    }

    //음악 플레이 리스트 만들기
    public void createPlayList(int start) {
        if (playList == null)
            playList = new LinkedList<>();
        else
            playList.clear();

        this.music = musicList.get(start); //현재 음악

        if (!shuffle) {
            //순차
            Log.d(TAG, "[Service] create sequence list.....");
            count = start;
            playList.addAll(musicList.subList(start + 1, musicList.size()));
            playList.addAll(musicList.subList(0, start));
        } else {
            //랜덤
            Log.d(TAG, "[Service] create shuffle list....");
            count = 0;
            List<Music> temp = new ArrayList<>(musicList);
            temp.remove(start); //시작 음악을 제외한 나머지 음악들

            Collections.shuffle(temp);
            playList.addAll(temp);
        }
    }

    public void setMusicList(List<Music> temp) {
        musicList = temp;
    }

    public void setMusic(Music m) {
        music = m;
    }

    //노래 실행
    public void start(Music music) {
        Log.d(TAG, "[Service] start...");

        //todo 수정
        if (music != null) {
            int start = musicList.indexOf(music);
            createPlayList(start);
        }

        try {
            mPlayer.reset();
            savePosition(musicList.indexOf(this.music));
            mPlayer.setDataSource(getApplicationContext(), Uri.parse(this.music.getPath()));
            mPlayer.prepareAsync();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //마지막 노래 인덱스 저장
    private void savePosition(int position) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("previous", position);
        editor.apply();
    }

    //현재 음악
    public Music getMusic() {
        return music;
    }

    //노래 재시작
    public void restart() {
        Log.d(TAG, "[Service] restart...");
        mPlayer.start();
        invalidateRestartView();
        startForeground(101, buildNotification());
    }

    //노래 일시중지
    public void pause() {
        Log.d(TAG, "[Service] pause...");
        mPlayer.pause();
        invalidatePauseView();
        startForeground(101, buildNotification());
    }

    //다음 곡 재생
    public void forward() {
        Log.d(TAG, "[Service] forward...");
        Log.d(TAG, "Count: " + count);
        if (count + 1 <= playList.size() || repeat == 1) {
            invalidateCompleteView();
            count = (count + 1) % playList.size();
            playList.add(music);

            this.music = playList.pollFirst();
            start(null);
        } else {
            Toast.makeText(this, "마지막 곡", Toast.LENGTH_SHORT).show();
        }

    }


    public int getCurrentPosition() {
        return mPlayer.getCurrentPosition();
    }

    //이전 곡 재생
    public void rewind() {
        Log.d(TAG, "[Service] rewind...");
        if (count - 1 >= 0 || repeat == 1) {
            invalidateCompleteView();
            count--;
            playList.addFirst(music);

            this.music = playList.pollLast();
            start(null);
        } else
            Toast.makeText(this, "첫 곡", Toast.LENGTH_SHORT).show();
    }

    public void setShuffle(boolean s) {
        shuffle = s;
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("shuffle", shuffle);
        editor.apply();

        createPlayList(musicList.indexOf(music));
    }

    public boolean getShuffle() {
        return shuffle;
    }

    public void setRepeat(int r) {
        repeat = r;
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("repeat", repeat);
        editor.apply();

        if (repeat == 0) {
            count = musicList.indexOf(music);
        }
    }

    public int getRepeat() {
        return repeat;
    }


    @Override
    public void onCompletion(MediaPlayer mp) {
        isPrepared = false;

        if (repeat == 0 || repeat == 1) {
            //반복 해제 or 전체 곡 반복
            forward();
        } else {
            //현재 곡 반복
            invalidateCompleteView();
            mp.seekTo(0);
            mp.start();
            invalidateStartView();
        }
    }

    public void seekTo(int pos){
        mPlayer.seekTo(pos);
    }

    //음악 실행 완료 뷰 갱신
    private void invalidateCompleteView() {
        Intent intent = new Intent("com.example.activity");
        intent.putExtra("mode", "complete");
        sendBroadcast(intent);

        intent = new Intent("com.example.detailActivity");
        intent.putExtra("mode", "complete");
        sendBroadcast(intent);
    }

    //정지 뷰 갱신
    private void invalidatePauseView() {
        Intent intent = new Intent("com.example.activity");
        intent.putExtra("mode", "pause");
        sendBroadcast(intent);

        intent = new Intent("com.example.detailActivity");
        intent.putExtra("mode", "pause");
        sendBroadcast(intent);
    }

    //음악 재재생 뷰 갱신
    private void invalidateRestartView() {
        Intent intent = new Intent("com.example.activity");
        intent.putExtra("mode", "restart");
        sendBroadcast(intent);

        intent = new Intent("com.example.detailActivity");
        intent.putExtra("mode", "restart");
        sendBroadcast(intent);
    }

    //음악 재생 뷰 갱신
    private void invalidateStartView() {
        Intent intent = new Intent("com.example.activity");
        intent.putExtra("mode", "start");
        intent.putExtra("pos", musicList.indexOf(music));
        sendBroadcast(intent);

        intent = new Intent("com.example.detailActivity");
        intent.putExtra("mode", "start");
        sendBroadcast(intent);
    }

    //Notification 채널 생성
    private void createNotificationChannel(String channelName) {
        if (Build.VERSION_CODES.O <= Build.VERSION.SDK_INT) {
            NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Music Player Channel");
            channel.enableLights(false);
            channel.enableVibration(false);
            channel.setVibrationPattern(new long[]{0});

            notificationManager.createNotificationChannel(channel);
        }
    }

    //Notification 생성
    private Notification buildNotification() {
        NotificationCompat.Builder ret = null;
        if (Build.VERSION_CODES.O <= Build.VERSION.SDK_INT) {
            ret = new NotificationCompat.Builder(getApplicationContext(), channelId);
        } else
            ret = new NotificationCompat.Builder(getApplicationContext());

        ret.setWhen(System.currentTimeMillis());
        ret.setContentText("Play");
        ret.setSmallIcon(R.drawable.music_note);
        ret.setAutoCancel(false);
        ret.setContent(buildRemoteViews());

        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent pIntent = PendingIntent.getActivity(getApplicationContext(), 101, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        ret.setContentIntent(pIntent);

        return ret.build();
    }

    //RemoteView 생성
    private RemoteViews buildRemoteViews() {
        RemoteViews ret = new RemoteViews(getApplicationContext().getPackageName(), R.layout.remote_view);

        ret.setTextViewText(R.id.remote_view_titie, music.getTitle());
        ret.setTextViewText(R.id.remote_view_artist, music.getArtist());

        Uri image = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), music.getAlbum());

        if (image == null)
            ret.setImageViewResource(R.id.remote_view_image, R.drawable.music_note);
        else
            ret.setImageViewUri(R.id.remote_view_image, image);

        if (isPlaying()) {
            ret.setImageViewResource(R.id.remote_view_play, R.drawable.pause);
        } else
            ret.setImageViewResource(R.id.remote_view_play, R.drawable.play);

        addAction(ret);

        return ret;
    }

    //RemoteView에 액션 부여
    private void addAction(RemoteViews remoteViews) {
        String packageName = "com.example.service";
        int[] resource_id = {R.id.remote_view_forward, R.id.remote_view_play, R.id.remote_view_rewind, R.id.remote_view_close};
        String[] val = {"forward", "play", "rewind", "close"};
        int[] requestCode = {0, 1, 2, 3};

        for (int i = 0; i < resource_id.length; i++) {
            Intent intent = new Intent(packageName);
            intent.putExtra("mode", val[i]);

            PendingIntent pIntent = PendingIntent.getBroadcast(getApplicationContext(), requestCode[i], intent, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteViews.setOnClickPendingIntent(resource_id[i], pIntent);
        }
    }


    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return true;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        isPrepared = true;

        mp.start();
        invalidateStartView();
        startForeground(101, buildNotification());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "[Service] onDestroy");
        unregisterReceiver(actionReceiver);
        unregisterReceiver(headPhonePlugReceiver);
        unregisterReceiver(mediaButtonReceiver);
        mPlayer.release();
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new MyBinder();
    }

    public class MyBinder extends Binder {
        public MusicService getInstance() {
            return MusicService.this;
        }
    }
}
