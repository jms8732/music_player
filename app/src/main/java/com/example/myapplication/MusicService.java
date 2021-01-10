package com.example.myapplication;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.icu.text.MessagePattern;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.text.TextUtilsCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.StringTokenizer;

public class MusicService extends Service implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, MediaPlayer.OnPreparedListener, Status
        , HandleAdpater, ItemMoveCallback.ItemTouchHelperAdapter {
    private static final String TAG = "jms8732", receiverName = "com.example.service";
    private static final int FORESERVICE = 1;
    private static final Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
    private Deque<Music> playList;
    private List<Music> musics;
    private MediaPlayer mp;
    private SharedPreferences prefs;
    private boolean isRand, isLoop;
    private TimeHandler handler;
    private ProgressThread pt;
    private String channelId;
    private NotificationManager manager;
    private NotificationChannel channel;
    private Adapter adapter;
    private Music playMusic;
    private HandleListener handleListener;
    private MusicViewModel musicViewModel;
    private HeadPhoneReceiver headPhoneReceiver;
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int mode = intent.getIntExtra("mode", -1);
            log("mode : " + mode);
            switch (mode) {
                case -1:
                    break;
                case PLAY:
                    if (mp.isPlaying())
                        pauseMusic();
                    else
                        restartMusic();
                    break;
                case FORWARD:
                    forwardMusic();
                    break;
                case REWIND:
                    rewindMusic();
                    break;
                case CLOSE:
                    pauseMusic();
                    stopForeground(true);
                    if (!isActivityAlive()) //액티비티가 살아있지 않을 경우
                        stopSelf();
                    break;
                case PAUSE:
                    if (mp.isPlaying())
                        pauseMusic();
                    break;
            }
        }
    };

    //엑티비티가 살아있는 지 판단하는 메소드
    private boolean isActivityAlive() {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> tasks = activityManager.getRunningTasks(Integer.MAX_VALUE);

        for (ActivityManager.RunningTaskInfo task : tasks) {
            if (getPackageName().equalsIgnoreCase(task.baseActivity.getPackageName()))
                return true;
        }

        return false;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        log("create...");
        //서비스가 연결되면 음악 목록을 불러온다.
        initialSettings();

        if (mp == null) {
            mp = new MediaPlayer();
            mp.setOnCompletionListener(this);
            mp.setOnErrorListener(this);
            mp.setOnPreparedListener(this);
        }

        handler = new TimeHandler(musicViewModel);
    }

    private void initialSettings() {
        bringPreference();

        musics = preparedMusicList();
        initialChannelSetting(); //Notification 채널 세팅
        registerReceivers();
        initialConstructor();
    }

    private void initialConstructor() {
        if (handleListener == null) {
            handleListener = new HandleListener(this);
        }

        if (musicViewModel == null) {
            musicViewModel = new MusicViewModel();
        }

        if (adapter == null) {
            adapter = new Adapter(getApplicationContext(), handleListener, musicViewModel);
            adapter.setMusic(musics);
        }

        if (playList == null)
            playList = new LinkedList<>();

    }

    public HandleListener getHandleListener() {
        return this.handleListener;
    }

    public Adapter getAdapter() {
        return this.adapter;
    }

    public MusicViewModel getMusicViewModel() {
        return this.musicViewModel;
    }


    //Preference값을 가져오는 메소드
    private void bringPreference() {
        prefs = getSharedPreferences("Music", MODE_PRIVATE);
        isRand = prefs.getBoolean("rand", false);
        isLoop = prefs.getBoolean("loop", false);
    }

    //리시버 등록
    private void registerReceivers() {
        registerReceiver(receiver, new IntentFilter(receiverName));
        headPhoneReceiver = new HeadPhoneReceiver();
        registerReceiver(headPhoneReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
    }

    private void unregisterReceivers() {
        unregisterReceiver(receiver);
        unregisterReceiver(headPhoneReceiver);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        log("onStartCommand...");

        componentSetting();
        return super.onStartCommand(intent, flags, startId);
    }

    private void componentSetting() {
        musicViewModel.setLoop(isLoop);
    }

    @Override
    public void onDestroy() {
        log("destroy");
        super.onDestroy();
        unregisterReceivers();
    }

    //음악을 준비하는 메소드
    private List<Music> preparedMusicList() {
        log("preparedMusic...");
        ArrayList<Music> ret = new ArrayList<>();

        String deleteList = prefs.getString("delete",null);

        String[] proj = new String[]{MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Artists.ARTIST,
                MediaStore.Audio.AudioColumns.DURATION
                , MediaStore.Audio.AudioColumns.DATA
                , MediaStore.Audio.AlbumColumns.ALBUM_ID};

        String selection = null;
        String[] selectionArgs = null;

        //todo 추가한 음악  표기
        if(deleteList == null){
            selection = MediaStore.Files.FileColumns.MIME_TYPE + "=?";
            selectionArgs = new String[]{MimeTypeMap.getSingleton().getMimeTypeFromExtension("mp3")};
        }else{
            String[] split = deleteList.split(" ");
            selection = MediaStore.Files.FileColumns.MIME_TYPE + "=? and " + MediaStore.Audio.Media._ID + " not in (" +  TextUtils.join(", ",split) + ")";
            selectionArgs = new String[]{MimeTypeMap.getSingleton().getMimeTypeFromExtension("mp3")};
        }


        Cursor cursor = getContentResolver().query(uri, proj, selection, selectionArgs, null);

        while (cursor.moveToNext()) {
            String id = cursor.getString(0);
            String title = cursor.getString(1);
            String artist = cursor.getString(2);
            int duration = cursor.getInt(3);
            String path = cursor.getString(4);
            int albumid = cursor.getInt(5);

            ret.add(new Music(id, title, artist, duration, albumid, path));

        }

        return ret;
    }
    @Override
    public void actionSetting(Music music, int position) {
        if (playList.isEmpty() || !music.getId().equals(playMusic.getId())) {
            //처음에 선택한 음악이 없거나 다른 음악을 선택한 경우
            makePlayList(position);
            beforeStartMusic();
        } else {
            if (mp.isPlaying()) {
                pauseMusic();
            } else {
                restartMusic();
            }
        }
    }

    private void makePlayList(int start) {
        if (playList != null)
            playList.clear();

        if (!isRand) {
            //순차 재생 리스트
            if (start < musics.size()) {
                playList.addAll(musics.subList(start, musics.size()));
                playList.addAll(musics.subList(0, start));
            }
        }else{
            List<Music> temp = new ArrayList<>(musics);
            temp.remove(start);

            Collections.shuffle(temp);
            playList.addAll(temp);
            playList.addFirst(musics.get(start));
        }
    }

    private void restartMusic() {
        log("restart music...");
        mp.start();
        playMusicSetting(true);
    }

    private void pauseMusic() {
        log("pause music...");
        handler.sendEmptyMessage(SEND_STOP);
        mp.pause();

        playMusicSetting(false);
    }

    @Override
    public void forwardMusic() {
        playList.addLast(playMusic);
        beforeStartMusic();
    }

    @Override
    public void rewindMusic() {
        playList.addFirst(playMusic);
        playList.addFirst(playList.pollLast());
        beforeStartMusic();

    }

    @Override
    public void onItemMove(int fromPos, int targetPos) {
        log("from: " + fromPos + " to: " + targetPos);
        Collections.swap(musics, fromPos, targetPos);
        adapter.notifyItemMoved(fromPos, targetPos);
    }

    @Override
    public void onItemDismiss(int pos) {
        log("onItemDismiss..");

        if (playMusic != null && playMusic.getId().equals(musics.get(pos).getId())) {
            //현재 진행하는 음악과 동일한 경우
            if (mp.isPlaying())
                mp.pause();
            musicViewModel.setOncePlay(false);
            stopForeground(true);
        }

        saveDeleteList(musics.get(pos).getId());
        musics.remove(pos);
        adapter.notifyItemRemoved(pos);
    }

    //전에 지웠던 음악 id들들
    private void saveDeleteList(String id){
        StringBuilder sb = null;
        String temp = prefs.getString("delete",null);

        if(temp == null)
            sb = new StringBuilder();
        else{
            sb = new StringBuilder(temp);

            if (temp.contains(id)) {
                return;
            }
        }

        SharedPreferences.Editor editor = prefs.edit();
        sb.append(" " + id);
        editor.putString("delete",sb.toString().trim());
        editor.apply();
    }

    @Override
    public void setRandom(boolean random) {
        isRand = random;
        musicViewModel.setRandom(isRand);

        int start = musics.indexOf(playMusic);
        makePlayList(start);

        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("rand", isRand);
        editor.apply();
    }

    @Override
    public void setLoop(boolean loop) {
        musicViewModel.setLoop(loop);
        isLoop = loop;

        if (isLoop)
            Toast.makeText(this, "반복 재생", Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(this, "한번 재생", Toast.LENGTH_SHORT).show();

        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("loop", isLoop);
        editor.apply();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        log("onError....");
        return true;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        log("onPrepared...");
        mp.start();
        musicViewModel.updateMusicView(playMusic);
        playMusicSetting(true);
    }

    //음악을 실행한 후, 수행되는 메소드
    private void playMusicSetting(boolean b) {
        if (b) {
            pt = new ProgressThread(mp, handler);
            handler.setProgressThread(pt);
            pt.start();
        } else
            handler.sendEmptyMessage(SEND_STOP);

        musicViewModel.updatePlayButton(b);
        startForeground(FORESERVICE, buildNotification(playMusic));
    }

    //음악 준비
    private void beforeStartMusic() {
        if (pt != null)
            handler.sendEmptyMessage(SEND_STOP);

        if (mp.isPlaying())
            mp.pause();

        if (!playList.isEmpty()) {
            playMusic = playList.pollFirst();

            try {
                mp.reset();
                mp.setDataSource(this, Uri.parse(playMusic.getPath()));
                mp.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            log("Play List End...");
            pt.lastMusic();
            startForeground(FORESERVICE, buildNotification(playMusic));
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        log("onCompletion...");
        //노래가 완료된 경우
        if (isLoop) {
            mp.seekTo(0);
            mp.start();
        } else {
            playList.addLast(playMusic);
            beforeStartMusic();
        }
    }

    private Notification buildNotification(Music music) {
        NotificationCompat.Builder builder = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new NotificationCompat.Builder(this, channelId);
        } else
            builder = new NotificationCompat.Builder(this);

        builder.setWhen(System.currentTimeMillis());
        builder.setContent(buildRemoveViews(music));
        builder.setContentText("Play");
        builder.setSmallIcon(R.drawable.circle_play);
        builder.setAutoCancel(false);

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pIntent = PendingIntent.getActivity(this, 101, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pIntent);

        return builder.build();
    }

    private RemoteViews buildRemoveViews(Music music) {
        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.remote_music_view);

        remoteViews.setTextViewText(R.id.remote_view_titie, music.getTitle());
        remoteViews.setTextViewText(R.id.remote_view_artist, music.getArtist());

        Bitmap bitmap = Util.getAlbumart(this, music.getImage());

        if (bitmap != null)
            remoteViews.setImageViewBitmap(R.id.remote_view_image, bitmap);
        else
            remoteViews.setImageViewResource(R.id.remote_view_image, R.drawable.album_white);

        if (mp.isPlaying()) {
            remoteViews.setImageViewResource(R.id.remote_view_play, R.drawable.pause_white);
        } else
            remoteViews.setImageViewResource(R.id.remote_view_play, R.drawable.play_white);


        Intent intent = new Intent(receiverName);
        intent.putExtra("mode", PLAY);
        PendingIntent pIntent = PendingIntent.getBroadcast(this, PLAY, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.remote_view_play, pIntent);

        intent = new Intent(receiverName);
        intent.putExtra("mode", FORWARD);
        pIntent = PendingIntent.getBroadcast(this, FORWARD, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.remote_view_forward, pIntent);

        intent = new Intent(receiverName);
        intent.putExtra("mode", REWIND);
        pIntent = PendingIntent.getBroadcast(this, REWIND, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.remote_view_rewind, pIntent);

        intent = new Intent(receiverName);
        intent.putExtra("mode", CLOSE);
        pIntent = PendingIntent.getBroadcast(this, CLOSE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.remote_view_close, pIntent);

        return remoteViews;
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void initialChannelSetting() {
        manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        channelId = "Music-Channel";
        String channelName = "Music player Test";
        String channelDescription = "First music player";

        channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(channelDescription);
        channel.enableLights(false);
        channel.enableVibration(false);
        channel.setVibrationPattern(new long[]{0});

        manager.createNotificationChannel(channel);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new MusicBinder();
    }

    private void log(String s) {
        Log.d(TAG, "[Service] " + s);
    }

    public void seekTo(int progress) {
        mp.seekTo(progress);
    }

    public class MusicBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

}
