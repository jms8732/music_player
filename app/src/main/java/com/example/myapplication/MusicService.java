package com.example.myapplication;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentUris;
import android.content.Intent;
import android.content.ServiceConnection;
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
import android.provider.MediaStore;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.BufferUnderflowException;
import java.nio.channels.InterruptedByTimeoutException;
import java.util.ArrayList;
import java.util.StringTokenizer;

public class MusicService extends Service implements MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener,
        ServiceNumber {
    private MediaPlayer mp = null;
    private MusicBinder binder = new MusicBinder();
    private int[] music_order;
    private int position;
    private SharedPreferences prefs;
    private String currentMusic = null; //현재 실행되는 음악 id
    private ArrayList<MusicVO> list;

    @Override
    public void onCreate() {
        super.onCreate();
        log("Service onCreate...");
        prefs = getSharedPreferences("music", MODE_PRIVATE);

        if (mp == null) {
            mp = new MediaPlayer();
            mp.setOnCompletionListener(this);
            mp.setOnPreparedListener(this);
        }

        loadPosition();
        loadOrder();
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
    public void onPrepared(MediaPlayer mp) {
        //음악이 준비된 상태라면
        log("Completed Prepared music...");
        mp.start();

        Intent intent = new Intent("com.example.service");
        intent.putExtra("code", MUSIC_START);

        intent.putExtra("pos", music_order[position]);
        sendBroadcast(intent);
    }

    public void setList(ArrayList<MusicVO> list) {
        this.list = list;
    }

    public int getPosition() {
        if (music_order == null)
            return -1;
        return music_order[position];
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        log("Service onStartCommand.....");

        int code = intent.getIntExtra("code", -1);
        int current = intent.getIntExtra("current", -1);
        MusicVO musicVO = intent.getParcelableExtra("data");

        if (code == -1) {
            //없을 경우
        } else if (code == 1) {
            //adapter에서 item을 클릭 한 경우
            String id = musicVO.getId();

            if (!isSameMusic(id)) {
                //선택한 음악이 기존의 음악과 다른 경우
                currentMusic = id;

                position = 0;
                makeMusicOrder(current);

                Notification noti = makeNotification(musicVO);
                String path = musicVO.getPath();
                startForeground(1, noti);

                startMusic(path);
            } else {
                //같은 경우,
                if (mp.isPlaying()) {
                    //현재 노래가 진행될 경우
                    pauseMusic();
                } else {
                    Notification noti = makeNotification(musicVO);
                    String path = musicVO.getPath();
                    startForeground(1, noti);

                    restartMusic();
                }
            }
        } else if (code == 3) {
            //서비스 종료
            mp.stop();
            mp.release();
            stopSelf();

            return START_NOT_STICKY;
        }
        return START_STICKY_COMPATIBILITY;

    }

    //음악 순서를 만드는 메소드
    private void makeMusicOrder(int current) {
        if (music_order == null) {
            music_order = new int[list.size() - 1];
        }

        music_order[position] = current; //현재 노래 번호를 맨 처음으로

        //상태에 따라 오더를 만든다.
        sequentialOrder(current);

        savePoint();
        saveOrder();
    }

    private void sequentialOrder(int current) {
        for (int i = 1; i < music_order.length; i++) {
            music_order[i] = (current + i) % music_order.length;
        }
    }

    private void randomOrder() {
    }

    private boolean isSameMusic(String id) {
        if (currentMusic != null) {
            if (id.equals(this.currentMusic)) {
                return true;
            }
        }
        return false;
    }

    public void restartMusic() {
        log("restart music...");
        mp.start();
    }

    public void startMusic(String path) {
        log("start music...");

        //현재 새로운 음악으로 설정
        try {
            mp.reset();
            mp.setDataSource(this, Uri.parse(path));
            mp.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void pauseMusic() {
        log("pause music...");
        mp.pause();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        log("finish");

        Intent intent = new Intent("com.example.service");
        intent.putExtra("code", MUSIC_FINISH);
        sendBroadcast(intent);

    }

    public int getCurrentProgress() {
        return mp.getCurrentPosition();
    }

    public void seekTo(int progress) {
        mp.seekTo(progress);
    }

    public boolean isPlaying() {
        return mp.isPlaying();
    }

    private void savePoint() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("pos", position);
        editor.apply();
    }

    private void saveOrder() {
        SharedPreferences.Editor editor = prefs.edit();
        StringBuilder sb = new StringBuilder();
        sb.append(music_order.length + " ");

        for (int i : music_order) {
            sb.append(i + " ");
        }

        editor.putString("order", sb.toString().trim());
        editor.apply();
    }

    private void loadPosition() {
        position = prefs.getInt("pos", -1);
    }

    private void loadOrder() {
        String order = prefs.getString("order", null);

        if (order != null) {
            StringTokenizer st = new StringTokenizer(order);
            music_order = new int[Integer.parseInt(st.nextToken())];

            for (int i = 0; i < music_order.length; i++) {
                music_order[i] = Integer.parseInt(st.nextToken());
            }
        }
    }

    public void moveMusic(boolean next) {
        if (next) {
            position = (position + 1) % music_order.length;
        } else {
            if(position -1 < 0)
                position = music_order.length-1;
            else
                position = (position-1) % music_order.length;
        }
        log("Position: " + position);

        savePoint();
        currentMusic = list.get(music_order[position]).getId();

        String path = list.get(music_order[position]).getPath();
        startMusic(path);
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
        cIntent.putExtra("code", 3);
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
