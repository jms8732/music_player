package com.example.musicplayer;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class MusicService extends Service implements MacroAdapter, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener
        , ItemMoveCallback.ItemTouchHelperAdapter {
    private final String TAG = "jms8732";
    private final String receiverName = "com.example.service";
    private List<Music> musicList;
    private Deque<Music> playList;
    private SharedPreferences prefs;
    private MusicAdapter mAdapter;
    private MediaPlayer mPlayer;
    private MusicViewModel model;
    private DurationThread thread;
    private DurationHandler handler;
    private Music playingMusic;
    private int count = 0;
    private NotificationManager manager;
    private NotificationChannel channel;
    private boolean loop, seq;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int mode = intent.getIntExtra("mode", 0);

            switch (mode) {
                case 1:
                    if (mPlayer.isPlaying())
                        rawPause();
                    else
                        rawReStart();
                    break;
                case -1:
                    rawForward();
                    break;
                case -2:
                    rawRewind();
                    break;
                case -3:
                    rawPause();
                    stopForeground(true);
                    if (!checkActivityAlive()) { //액티비티가 살아있지 않을 경우
                        stopSelf();
                        mPlayer.release();
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
    public void onCreate() {
        Log.d(TAG, "[Service] onCreate:");
        super.onCreate();

        initialSettings();
    }

    private void initialSettings() {
        if (prefs == null)
            prefs = getSharedPreferences("music", MODE_PRIVATE);

        loadInstance();
        loadMusicList();
        initialChannelSetting();
        registerReceiver(receiver, new IntentFilter(receiverName));

        setModel();
    }

    private void setModel() {
        model.setCurrentDuration(0);
        model.setTitle(playingMusic.getTitle());
        model.setArtist(playingMusic.getArtist());
        model.setAlbum(playingMusic.getAlbum());
        model.setTotalDuration(playingMusic.getDuration());
        model.setSequential(seq);
        model.setLoop(loop);
    }

    private void loadInstance() {
        if (model == null)
            model = new MusicViewModel();

        if (mPlayer == null) {
            mPlayer = new MediaPlayer();
            mPlayer.setOnPreparedListener(this);
            mPlayer.setOnCompletionListener(this);
            mPlayer.setOnErrorListener(this);
        }

        if (handler == null)
            handler = new DurationHandler(model);

        if (playList == null)
            playList = new LinkedList<>();

        loop = prefs.getBoolean("loop", false);
        seq = prefs.getBoolean("seq", true);
    }

    private void loadMusicList() {
        if (musicList == null)
            musicList = new ArrayList<>();

        String deletes = prefs.getString("delete", null);

        String[] proj = new String[]{MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Artists.ARTIST,
                MediaStore.Audio.AudioColumns.DURATION
                , MediaStore.Audio.AudioColumns.DATA
                , MediaStore.Audio.Albums.ALBUM_ID};

        String selection = null;
        String[] selectionArgs = null;

        if (deletes == null) {
            selection = MediaStore.Files.FileColumns.MIME_TYPE + "=?";
            selectionArgs = new String[]{MimeTypeMap.getSingleton().getMimeTypeFromExtension("mp3")};
        } else {
            String[] split = deletes.split(" ");
            selection = MediaStore.Files.FileColumns.MIME_TYPE + "=? and " + MediaStore.Audio.Media._ID + " not in (" + TextUtils.join(", ", split) + ")";
            selectionArgs = new String[]{MimeTypeMap.getSingleton().getMimeTypeFromExtension("mp3")};
        }

        Cursor cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, proj, selection, selectionArgs, null);

        while (cursor.moveToNext()) {
            String id = cursor.getString(0);
            String title = cursor.getString(1);
            String artist = cursor.getString(2);
            int duration = cursor.getInt(3);
            String path = cursor.getString(4);
            long album_id = cursor.getLong(5);

            musicList.add(new Music(id, duration, album_id, title, artist, path));

        }

        int played = prefs.getInt("played", 0);
        createPlayList(played);

        cursor.close();
    }

    private void createPlayList(int start) {
        Log.d(TAG, "createPlayList");
        if (!playList.isEmpty())
            playList.clear();

        playingMusic = musicList.get(start);

        if (seq) {
            count = start;
            playList.addAll(musicList.subList(start + 1, musicList.size()));
            playList.addAll(musicList.subList(0, start));
        } else {
            count = 0;
            List<Music> temp = new ArrayList<>(musicList);
            temp.remove(start); //시작 음악을 제외한 나머지 음악들

            Collections.shuffle(temp);
            playList.addAll(temp);
        }
    }

    public MusicViewModel getModel() {
        return model;
    }

    public void setAdapter(MusicAdapter adapter) {
        this.mAdapter = adapter;
        mAdapter.setMusicList(musicList);
    }

    @Override
    public void onItemClick(int pos) {
        int playMusicNum = musicList.indexOf(playingMusic);
 
        if (pos >= 0) {
            if (pos != playMusicNum) {
                //서로 다른 item을 클릭하거나 처음 음악을 실행할 경우,
                model.setStatus(false);
                musicList.get(playMusicNum).setVisible(false);
                mAdapter.notifyItemChanged(playMusicNum);

                createPlayList(pos);
                rawStart();
            } else {
                try {
                    int id = mPlayer.getSelectedTrack(MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_AUDIO); //에러가 발생하면 현재 음악을 한번도 실행하지 않았다는 의미
                    if (mPlayer.isPlaying()) {
                        rawPause();
                    } else {
                        rawReStart();
                    }
                } catch (Exception e) {
                    rawStart();
                }
            }
        } else if (pos == -2 || pos == -3) {
            //rewind or forward를 클릭했을 시
            if (pos == -2)
                rawRewind();
            else
                rawForward();
        } else if (pos == -4 || pos == -5) {
            if (pos == -4) {
                loop = !loop;
                model.setLoop(loop);
            } else {
                seq = !seq;
                model.setSequential(seq);
                createPlayList(playMusicNum);
            }

            saveStatus();
        } else {
            try {
                int id = mPlayer.getSelectedTrack(MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_AUDIO); //에러가 발생하면 현재 음악을 한번도 실행하지 않았다는 의미
                if (mPlayer.isPlaying()) {
                    rawPause();
                } else {
                    rawReStart();
                }
            } catch (Exception e) {
                //맨 처음 Recycler view item을 클릭하지 않고 플레이 버튼을 클릭한 경우
                rawStart();
            }
        }
    }

    private void rawPause() {
        Log.d(TAG, "rawPause");
        mPlayer.pause();

        int playMusicNum = musicList.indexOf(playingMusic);
        mAdapter.notifyItemChanged(playMusicNum);
        handler.setThread(thread);
        handler.sendEmptyMessage(0);
        model.setStatus(false);

        Notification notification = Util.getInstance().buildNotification(getApplicationContext(), playingMusic, false);
        startForeground(1, notification);
    }

    private void rawStart() {
        Log.d(TAG, "rawStart");
        if (thread != null) {
            handler.setThread(thread);
            handler.sendEmptyMessage(0);
        }

        try {
            mPlayer.reset();
            mPlayer.setDataSource(getApplicationContext(), Uri.parse(playingMusic.getPath()));
            mPlayer.prepareAsync();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void rawReStart() {
        Log.d(TAG, "rawRestart");

        thread = new DurationThread(handler);
        thread.setPlayer(mPlayer);

        mPlayer.start();
        thread.start();

        int playMusicNum = musicList.indexOf(playingMusic);

        if(playMusicNum != -1) {
            musicList.get(playMusicNum).setVisible(true);
            mAdapter.notifyItemChanged(playMusicNum);
            model.setStatus(true);
        }

        Notification notification = Util.getInstance().buildNotification(getApplicationContext(), playingMusic, true);
        startForeground(1, notification);
    }

    private void rawRewind() {
        if (count - 1 < 0)
            Toast.makeText(this, "첫 곡입니다.", Toast.LENGTH_SHORT).show();
        else {
            count -= 1;

            //이전에 실행한 음악 뷰 갱신
            model.setStatus(false);
            int playMusicNum = musicList.indexOf(playingMusic);

            if (playMusicNum != -1) {
                musicList.get(playMusicNum).setVisible(false);
                mAdapter.notifyItemChanged(playMusicNum);
            }

            playList.addFirst(playingMusic);
            playingMusic = playList.pollLast();
            rawStart();
        }
    }

    private void rawForward() {
        if (count + 1 >= musicList.size()) {
            Toast.makeText(this, "마지막 곡 입니다.", Toast.LENGTH_SHORT).show();
        } else {
            count += 1;

            //이전에 실행한 음악 뷰 갱신
            model.setStatus(false);
            int playMusicNum = musicList.indexOf(playingMusic);

            if (playMusicNum != -1) {
                musicList.get(playMusicNum).setVisible(false);
                mAdapter.notifyItemChanged(playMusicNum);
            }

            playList.addLast(playingMusic);
            playingMusic = playList.poll();
            rawStart();
        }

    }

    public void seekTo(int progress, boolean stopTrack) {
        if (stopTrack) {
            mPlayer.seekTo(progress);
        }
        model.setCurrentDuration(progress);
    }


    @Override
    public void onItemDismiss(int pos) {
        Log.d(TAG, "onItemDismiss");

        if (playingMusic.getId().equals(musicList.get(pos).getId())) {
            //현재 진행하고 있는 음악과 동일한 경우
            if (mPlayer.isPlaying()) {
                model.setStatus(false);
                musicList.get(pos).setVisible(false);
                mAdapter.notifyItemChanged(pos);

                mPlayer.pause();
            }

            //다음 곡으로 화면 대체
            playingMusic = playList.pollFirst();
            rawStart();
            setModel();


            saveDeleteList(musicList.get(pos).getId());

            musicList.remove(pos);
            mAdapter.notifyItemRemoved(pos);
        } else {
            //실행 중인 음악이 아닌 경우, 플레이 리스트 재설정
            saveDeleteList(musicList.get(pos).getId());

            musicList.remove(pos);
            mAdapter.notifyItemRemoved(pos);

            int start = musicList.indexOf(playingMusic);
            createPlayList(start);
        }
    }

    //전에 지웠던 음악 id들
    private void saveDeleteList(String id) {
        StringBuilder sb = null;
        String temp = prefs.getString("delete", null);

        if (temp == null)
            sb = new StringBuilder();
        else {
            if (temp.contains(id)) {
                return;
            }

            sb = new StringBuilder(temp);
        }

        SharedPreferences.Editor editor = prefs.edit();
        sb.append(" " + id);
        editor.putString("delete", sb.toString().trim());
        editor.apply();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }


    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.d(TAG, "onCompletion");

        if (loop) {
            //현재 상황이 루프가 켜져있을 경우
            mPlayer.seekTo(0);
            mPlayer.start();
        } else {
            handler.sendEmptyMessage(0);
            rawForward();
        }
    }


    @TargetApi(Build.VERSION_CODES.O)
    private void initialChannelSetting() {
        manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        String channelId = "JChannel";
        String channelName = "Music player Test";
        String channelDescription = "First music player";

        channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(channelDescription);
        channel.enableLights(false);
        channel.enableVibration(false);
        channel.setVibrationPattern(new long[]{0});

        manager.createNotificationChannel(channel);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "[Service] onDestroy");
        unregisterReceiver(receiver);
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return true;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.d(TAG, "onPrepared");

        rawReStart();
        saveMusic();

        Notification notification = Util.getInstance().buildNotification(getApplicationContext(), playingMusic, true);
        startForeground(1, notification);
        setModel();
    }

    private void saveStatus(){
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("loop",loop);
        editor.putBoolean("seq",seq);
        editor.apply();
    }

    private void saveMusic(){
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("played",musicList.indexOf(playingMusic));
        editor.apply();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new MyBinder();
    }

    public class MyBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }
}
