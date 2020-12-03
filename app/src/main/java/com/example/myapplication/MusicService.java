package com.example.myapplication;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentUris;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.FileDescriptor;
import java.nio.BufferUnderflowException;
import java.nio.channels.InterruptedByTimeoutException;
import java.util.ArrayList;
import java.util.StringTokenizer;

public class MusicService extends Service implements MediaPlayer.OnCompletionListener {
    private MediaPlayer mp = null;
    private MusicBinder binder = new MusicBinder();
    private final int FINISH = 1;
    private int[] music_order;
    private int position;
    private SharedPreferences prefs;
    private ArrayList<MusicVO> list;

    @Override
    public void onCreate() {
        super.onCreate();
        log("Service onCreate...");

        prefs = getSharedPreferences("Status", MODE_PRIVATE);
        position = prefs.getInt("pos", -1);
        String order = prefs.getString("order", null);

        //순서 세팅
        if (order != null) {
            setMusicOrder(order);
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mp != null) {
            mp.stop();
            mp.release();
        }

        log("Service onDestroy....");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        log("Service onStartCommand.....");

        int code = intent.getIntExtra("code", -1);

        if (code == FINISH) {
            stopSelf();
            return super.onStartCommand(intent, flags, startId);
        } else {
            Notification noti = makeNotification((MusicVO) intent.getParcelableExtra("data"));

            if (code != 2) {
                String order = intent.getStringExtra("array");

                setMusicOrder(order);
                savePosition();
                saveOrder(order);

                setMusic(((MusicVO) intent.getParcelableExtra("data")).getPath());
            }

            startForeground(1, noti);
            return START_STICKY_COMPATIBILITY;
        }
    }

    //노래 순서 설정
    private void setMusicOrder(String order) {
        log("Music order : " + order);
        StringTokenizer st = new StringTokenizer(order);
        music_order = new int[Integer.parseInt(st.nextToken())];

        position = 0;

        for (int i = 0; i < music_order.length; i++) {
            music_order[i] = Integer.parseInt(st.nextToken());
        }
    }

    public void saveOrder(String order){
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("order",order);
        editor.apply();
    }

    public void savePosition(){
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("pos",position);
        editor.apply();
    }

    public void setMusicList(ArrayList<MusicVO> list) {
        this.list = list;
    }

    //Mediaplayer 설정
    public void setMusic(String path) {
        //이전에 존재한 Mediaplayer 멈춤
        if (mp != null && mp.isPlaying()) {
            mp.pause();
            mp.release();
        }

        mp = MediaPlayer.create(this, Uri.parse(path));
        mp.setOnCompletionListener(this);
        mp.start();
    }

    public void musicStart() {
        mp.start();
    }

    public void setProgress(int progress) {
        mp.seekTo(progress);
    }

    public void musicPause() {
        mp.pause();
    }

    public boolean isPlaying() {
        return mp.isPlaying();
    }

    public boolean isMpNull() {
        return mp == null ? true : false;
    }

    public int getCurrentDuration() {
        if (mp != null)
            return mp.getCurrentPosition();

        return 0;
    }

    //다음 노래
    public int getNextMusic() {
        ++position;

        savePosition();
        return music_order[position % music_order.length];
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        //현재 실행중인 음악이 종료됬을 시,
        if (position + 1 < music_order.length) {
            //아직 실행 시킬 음악이 남아 있을 경우
            position++;

            savePosition();
            Intent intent = new Intent("com.example.musicPlayer");
            intent.putExtra("pos", music_order[position]);
            sendBroadcast(intent);

            MusicRecyclerAdapter.id = list.get(music_order[position]).getId();

            Notification noti = makeNotification(list.get(music_order[position]));
            setMusic(list.get(music_order[position]).getPath());
            if (Build.VERSION_CODES.O <= Build.VERSION.SDK_INT) {
                startForeground(1, noti);
            } else
                startService(intent);
        } else {
            //없을 경우
            mp.release();
            stopSelf();
        }
    }

    public int getPosition() {
        if(music_order == null)
            return -1;
        return music_order[position];
    }

    private Notification makeNotification(MusicVO musicVO) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "MusicChannel";
            String channelName = "MusicService";

            NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setVibrationPattern(new long[]{0});
            manager.createNotificationChannel(channel);

            builder = new NotificationCompat.Builder(this, channelId);
        } else {
            builder = new NotificationCompat.Builder(this);
        }

        Intent aIntent = new Intent(this, MainActivity.class);

        PendingIntent pIntent = PendingIntent.getActivity(this, 102, aIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        builder.setContentTitle(musicVO.getTitle());
        builder.setContentText(musicVO.getArtist());
        builder.setSmallIcon(R.drawable.play);
        builder.setContentIntent(pIntent);
        builder.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_LIGHTS);
        builder.setVibrate(new long[]{0});

        RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.custom_remoteview);
        settingRemoteView(contentView, musicVO);
        builder.setContent(contentView);

        return builder.build();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        log("onBind...");
        return binder;
    }


    //RemoteView 설정
    private void settingRemoteView(RemoteViews contentView, MusicVO musicVO) {
        contentView.setTextViewText(R.id.remoteView_title, musicVO.getTitle());
        contentView.setTextViewText(R.id.remoteView_artist, musicVO.getArtist());

        Bitmap bitmap = getAlbumart(musicVO.getAlbum_id());

        if (bitmap == null)
            bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.volume);
        contentView.setImageViewBitmap(R.id.remoteView_image, bitmap);

        Intent cIntent = new Intent(this, MusicService.class);
        cIntent.putExtra("code", FINISH);
        PendingIntent pIntent = PendingIntent.getService(this, 0, cIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        contentView.setOnClickPendingIntent(R.id.remoteView_close, pIntent);
    }

    private void log(String s) {
        Log.d("jms8732", s);
    }

    public class MusicBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
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


}
