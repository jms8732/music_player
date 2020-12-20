package com.example.myapplication;

import android.Manifest;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.RippleDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;

public class MusicService extends Service implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, MediaPlayer.OnPreparedListener {
    private MediaPlayer mp = null;
    private MusicBinder binder = new MusicBinder();
    private int position;
    private String currentMusic = null; //현재 실행되는 음악 id
    private ArrayList<String> showMusicList;
    private String[] musicOrder;
    private boolean orderStatus, loopStatus;
    private SaveFileManager saveFileManager;
    private NotificationManager manager;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            showMusicList = intent.getStringArrayListExtra("list");
            String tar = intent.getStringExtra("target");
            int code = intent.getIntExtra("code", -1);

            if (code == 1) {
                //드래그가 끝난 후
                makeMusicOrder(currentMusic, orderStatus);
            } else if (code == 2) {
                //삭제가 끝난 후
                if (tar != null && tar.equals(currentMusic)) {
                    Intent aIntent = new Intent("com.example.activity");
                    aIntent.putExtra("code", 3);
                    sendBroadcast(aIntent);

                    saveFileManager.saveMusicList(showMusicList);
                }
            }
        }
    };

    private BroadcastReceiver remote_receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int code = intent.getIntExtra("code", -1);

            log("Code: " + code);
            if (code == -1) {

            }
            if (code == 1) {
                //remote view의 플레이 버튼 클릭
                log("play.......");
                if(mp.isPlaying()){
                    pauseMusic();
                }else
                    restartMusic();
            } else if(code == 2  || code == 4){
                //2 는 이전 곡, 4 는 다음 곡 선택
                boolean check = true;
                if(code == 2)
                    check = false;

                moveMusic(check);
            } else if (code == 3) {
                log("stopForeground...");
                if (mp.isPlaying())
                    pauseMusic();

                stopForeground(true);

                if (!checkActivityAlive()) {
                    //현재 액티비티가 살아있지 않는 경우
                    stopSelf();
                }
            }
        }
    };

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

        if (manager == null)
            manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        registerReceiver(receiver, new IntentFilter("com.example.service"));
        registerReceiver(remote_receiver, new IntentFilter("com.example.remote"));

        orderStatus = saveFileManager.loadOrderStatus();
        loopStatus = saveFileManager.loadLoopStatus();
        currentMusic = saveFileManager.loadId();

        if (showMusicList == null)
            showMusicList = searchMusicPath();
    }


    //안드로이드 내에 존재하는 파일들을 탐색하여 확장자 mp3를 가진 파일들을 찾는 메소드
    private ArrayList<String> searchMusicPath() {
        ArrayList<String> list = new ArrayList<>();
        String previousMusicList = saveFileManager.loadMusicList();

        if (previousMusicList == null) {
            Cursor cursor = MusicSearcher.findId(this);

            while (cursor != null && cursor.moveToNext()) {
                list.add(cursor.getString(0));
            }
        } else {
            StringTokenizer st = new StringTokenizer(previousMusicList);

            while (st.hasMoreElements()) {
                list.add(st.nextToken());
            }
        }
        //마지막에 '맨 위로' 처리
        list.add(null);

        return list;
    }


    public ArrayList<String> getMusicList() {
        return this.showMusicList;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mp != null) {
            mp.release();
        }

        unregisterReceiver(receiver);
        unregisterReceiver(remote_receiver);
        MainActivity.recyclerView = null;
        log("Service onDestroy....");
    }

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
    public void onPrepared(MediaPlayer mp) {
        //음악이 준비된 상태라면
        log("Completed Prepared music...");
        mp.start();

        Intent intent = new Intent("com.example.activity");
        intent.putExtra("id", musicOrder[position]);
        intent.putExtra("code",0);
        sendBroadcast(intent);

        Notification noti = makeNotification(currentMusic);
        startForeground(1,noti);
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        log("=====================error found=========================");
        return true; //음악 강제 종료를 피하기 위해 true값 반환
    }

    public String getId() {
        if (currentMusic == null)
            return null;
        return currentMusic;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        log("Service onStartCommand.....");

        int code = intent.getIntExtra("code", -1);
        String chooseId = intent.getStringExtra("data");

        if (code == -1) {
            //없을 경우
        } else if (code == 1) {
            //adapter에서 item을 클릭 한 경우
            if (!isSameMusic(chooseId)) {
                //선택한 음악이 기존의 음악과 다른 경우
                currentMusic = chooseId;

                makeMusicOrder(chooseId, orderStatus);
                String path = MusicSearcher.findPath(this, chooseId);

                startMusic(path);
            } else {
                //같은 경우,
                if (mp.isPlaying()) {
                    //현재 노래가 진행될 경우
                    pauseMusic();
                } else {
                    if (musicOrder == null) {
                        makeMusicOrder(chooseId, orderStatus);
                        startMusic(MusicSearcher.findPath(this, currentMusic));
                    } else {
                        restartMusic();
                    }
                }
            }
        }
        return START_REDELIVER_INTENT;
    }

    //음악 순서를 만드는 메소드
    public void makeMusicOrder(String current, boolean order) {
        if (musicOrder == null)
            musicOrder = new String[showMusicList.size() - 1];

        position = 0;
        musicOrder[position] = current;

        //상태에 따라 오더를 만든다.
        if (order)
            sequentialOrder(current);
        else
            randomOrder(current);


        orderStatus = order;
        saveFileManager.saveOrderStatus(orderStatus);
        saveFileManager.saveLastId(current);
        saveFileManager.saveMusicList(showMusicList);
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

    private void sequentialOrder(String current) {
        int start_idx = showMusicList.indexOf(current);

        for (int i = 1; i < showMusicList.size() - 1; i++) {
            musicOrder[i] = showMusicList.get((start_idx + i) % (showMusicList.size() - 1));
        }

        log("===========make SequentialOrder==========");
    }

    private void randomOrder(String current) {
        int start_idx = showMusicList.indexOf(current);
        boolean[] visited = new boolean[showMusicList.size() - 1];
        visited[start_idx] = true;

        for (int i = 1; i < visited.length; i++) {
            musicOrder[i] = showMusicList.get(getIdx(visited));
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

        Notification noti = makeNotification(currentMusic);
        startForeground(1,noti);

        Intent intent = new Intent("com.example.activity");
        intent.putExtra("code",2);
        sendBroadcast(intent);
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

        Notification noti = makeNotification(currentMusic);
        startForeground(1,noti);

        Intent intent = new Intent("com.example.activity");
        intent.putExtra("code",1);

        sendBroadcast(intent);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        log("-----------------------------finish------------------------------");
        if (loopStatus) {
            mp.seekTo(0);
            mp.start();
        } else {
            moveMusic(true);
        }
    }

    public int getCurrentProgress() {
        if (mp == null)
            return 0;
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
            position = (position + 1) % (showMusicList.size() - 1);
        } else {
            log("previous music.....");
            if (position - 1 < 0)
                position = (showMusicList.size() - 2);
            else
                position = (position - 1) % (showMusicList.size() - 1);
        }
        log("-----------------------------Position: " + position + "----------------------------------");

        saveFileManager.saveLastId(musicOrder[position]);
        currentMusic = musicOrder[position];

        String path = MusicSearcher.findPath(this, musicOrder[position]);
        startMusic(path);
    }


    private Notification makeNotification(String id) {
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

        PendingIntent pIntent = PendingIntent.getActivity(this, 102, aIntent, PendingIntent.FLAG_UPDATE_CURRENT);

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


        Bitmap bm = getAlbumart(albumId);
        if (bm == null)
            contentView.setImageViewResource(R.id.remoteView_image, R.drawable.album_white);
        else
            contentView.setImageViewBitmap(R.id.remoteView_image, bm);

        if(mp.isPlaying()) {
            contentView.setImageViewResource(R.id.remoteView_play, R.drawable.remote_pause);
        }else
            contentView.setImageViewResource(R.id.remoteView_play, R.drawable.remote_play);

        Intent cIntent = new Intent("com.example.remote");
        cIntent.putExtra("code", 3);
        PendingIntent pIntent = PendingIntent.getBroadcast(this, 3, cIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        contentView.setOnClickPendingIntent(R.id.remoteView_close, pIntent);

        Intent bIntent = new Intent("com.example.remote");
        bIntent.putExtra("code", 1);
        PendingIntent pIntent1 = PendingIntent.getBroadcast(this, 1, bIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        contentView.setOnClickPendingIntent(R.id.remoteView_play, pIntent1);

        Intent aIntent = new Intent("com.example.remote");
        aIntent.putExtra("code",2);
        PendingIntent pIntent2 = PendingIntent.getBroadcast(this,2,aIntent,PendingIntent.FLAG_UPDATE_CURRENT);
        contentView.setOnClickPendingIntent(R.id.remoteView_fast_rewind,pIntent2);

        Intent dIntent = new Intent("com.example.remote");
        dIntent.putExtra("code",4);
        PendingIntent pIntent3 = PendingIntent.getBroadcast(this,4,dIntent,PendingIntent.FLAG_UPDATE_CURRENT);
        contentView.setOnClickPendingIntent(R.id.remoteView_fast_forward,pIntent3);
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
