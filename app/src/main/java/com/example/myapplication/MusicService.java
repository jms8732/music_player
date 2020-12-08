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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.StringTokenizer;

public class MusicService extends Service implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, MediaPlayer.OnPreparedListener {
    private MediaPlayer mp = null;
    private MusicBinder binder = new MusicBinder();
    private int[] music_order;
    private int position;
    private String currentMusic = null; //현재 실행되는 음악 id
    private ArrayList<String> list;
    private boolean orderStatus, loopStatus;
    private SaveFileManager saveFileManager;

    @Override
    public void onCreate() {
        super.onCreate();
        log("Service onCreate...");

        if (mp == null) {
            mp = new MediaPlayer();
            mp.setOnCompletionListener(this);
            mp.setOnPreparedListener(this);
            mp.setOnErrorListener(this);
        }

        if (saveFileManager == null)
            saveFileManager = new SaveFileManager(this);

        position = saveFileManager.loadPosition();
        music_order = saveFileManager.loadOrder();
        orderStatus = saveFileManager.loadOrderStatus();
        loopStatus = saveFileManager.loadLoopStatus();
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
        intent.putExtra("pos", music_order[position]);
        sendBroadcast(intent);
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        log("=====================error found=========================");
        return true; //음악 강제 종료를 피하기 위해 true값 반환
    }

    public void setList(ArrayList<String> list) {
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
        String chooseId = intent.getStringExtra("data");

        if (code == -1) {
            //없을 경우
        } else if (code == 1) {
            //adapter에서 item을 클릭 한 경우
            if (!isSameMusic(chooseId)) {
                //선택한 음악이 기존의 음악과 다른 경우
                currentMusic = chooseId;

                makeMusicOrder(current, orderStatus);

                Notification noti = makeNotification(chooseId);
                String path = MusicSearcher.findPath(this, chooseId);
                startForeground(1, noti);

                startMusic(path);
            } else {
                //같은 경우,
                Intent bIntent = new Intent("com.example.service");
                bIntent.putExtra("code", 1);
                if (mp.isPlaying()) {
                    //현재 노래가 진행될 경우
                    pauseMusic();
                    bIntent.putExtra("status", false);
                } else {
                    Notification noti = makeNotification(chooseId);
                    startForeground(1, noti);

                    restartMusic();
                    bIntent.putExtra("status", true);
                }

                sendBroadcast(bIntent);
            }
        } else if (code == 3) {
            //서비스 종료
            mp.stop();
            mp.release();
            mp = null;
            stopSelf();

            saveFileManager.saveOrderStatus(orderStatus);
            saveFileManager.saveOrder(music_order);
            saveFileManager.savePoint(position);
            return START_NOT_STICKY;
        }
        return START_REDELIVER_INTENT;

    }

    //음악 순서를 만드는 메소드
    public void makeMusicOrder(int current, boolean order) {
        if (music_order == null) {
            music_order = new int[list.size() - 1];
        }

        position = 0;
        music_order[position] = current; //현재 노래 번호를 맨 처음으로

        //상태에 따라 오더를 만든다.
        if (order)
            sequentialOrder(current);
        else
            randomOrder(current);

        saveFileManager.savePoint(position);
        saveFileManager.saveOrder(music_order);

        orderStatus = order;
        saveFileManager.saveOrderStatus(orderStatus);
    }

    public boolean getOrderStatus() {
        return this.orderStatus;
    }

    public boolean getLoopStatus() {
        return this.loopStatus;
    }

    public void setLoopStatus(boolean loop) {
        this.loopStatus = loop;
        saveFileManager.saveLoopStatus(loopStatus);
    }

    private void sequentialOrder(int current) {
        for (int i = 1; i < music_order.length; i++) {
            music_order[i] = (current + i) % music_order.length;
        }

        log("===========make SequentialOrder==========");
    }

    private void randomOrder(int current) {
        boolean[] visited = new boolean[music_order.length];
        visited[current] = true;

        for (int i = 1; i < music_order.length; i++) {
            music_order[i] = getIdx(visited);
        }

        log("===========make RandomOrder=============");
    }

    private int getIdx(boolean[] visited) {
        Random rand = new Random();
        while (true) {
            int idx = rand.nextInt(visited.length);
            if (!visited[idx]) {
                visited[idx] = true;
                return idx;
            }
        }
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
        if (mp != null && mp.isPlaying()) //이전에 진행된 음악 멈춤
            mp.pause();

        //현재 새로운 음악으로 설정
        try {
            mp.reset();
            mp.setDataSource(this, Uri.parse(path));
            mp.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
        log("----------------------------start music-----------------------------");
    }

    public void pauseMusic() {
        log("pause music...");
        mp.pause();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        log("-----------------------------finish------------------------------");
        if (loopStatus) {
            mp.seekTo(0);
            mp.start();
        } else {
            moveMusic(true);

            Intent intent = new Intent("com.example.service");
            intent.putExtra("pos", music_order[position]);
            sendBroadcast(intent);

            Notification noti = makeNotification(list.get(music_order[position]));
            startForeground(1, noti);
        }
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

    public void moveMusic(boolean next) {
        if (next) {
            log("next music.....");
            position = (position + 1) % music_order.length;
        } else {
            log("previous music.....");
            if (position - 1 < 0)
                position = music_order.length - 1;
            else
                position = (position - 1) % music_order.length;
        }
        log("-----------------------------Position: " + position + "----------------------------------");

        saveFileManager.savePoint(position);
        currentMusic = list.get(music_order[position]);

        Notification noti = makeNotification(list.get(music_order[position]));
        startForeground(1, noti);

        String path = MusicSearcher.findPath(this, list.get(music_order[position]));
        startMusic(path);
    }


    private Notification makeNotification(String id) {
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


        String title = MusicSearcher.findDisplayName(this, id);
        String artist = MusicSearcher.findArtist(this, id);
        builder.setContentTitle(title);
        builder.setContentText(artist);
        builder.setSmallIcon(R.drawable.play);
        builder.setContentIntent(pIntent);
        builder.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_LIGHTS);
        builder.setVibrate(new long[]{0});

        RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.custom_remoteview);
        settingRemoteView(contentView, id);
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
    private void settingRemoteView(RemoteViews contentView, String id) {
        String title = MusicSearcher.findDisplayName(this, id);
        String artist = MusicSearcher.findArtist(this, id);
        int albumId = MusicSearcher.findAlbumId(this, id);

        contentView.setTextViewText(R.id.remoteView_title, title);
        contentView.setTextViewText(R.id.remoteView_artist, artist);

        Bitmap bitmap = getAlbumart(albumId);

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
