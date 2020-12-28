package com.example.myapplication;

import android.app.Service;
import android.content.ContentUris;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;
import androidx.recyclerview.widget.RecyclerView;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class MusicService extends Service implements MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, MediaPlayer.OnPreparedListener, Status
        , HandleAdpater {
    private static final String TAG = "jms8732";
    private static final Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
    private ArrayList<Music> musics;
    private MediaPlayer mp;
    private SharedPreferences prefs;
    private int position = 0;
    private boolean isSeq, isLoop;
    private int[] order;
    private InnerListener innerListener;
    private Handler handler;
    private ProgressThread pt;
    private String current_id;

    @Override
    public void onCreate() {
        super.onCreate();
        log("create...");

        //서비스가 연결되면 음악 목록을 불러온다.
        musics = preparedMusicList();

        if (mp == null) {
            mp = new MediaPlayer();
            mp.setOnCompletionListener(this);
            mp.setOnErrorListener(this);
            mp.setOnPreparedListener(this);
        }

        prefs = getSharedPreferences("Music", MODE_PRIVATE);
        isSeq = prefs.getBoolean("sequential", true);
        isLoop = prefs.getBoolean("loop", false);
        position = prefs.getInt("current", 0);

        if (handler == null) {
            handler = new Handler() {
                @Override
                public void handleMessage(@NonNull Message msg) {
                    switch (msg.what) {
                        case SEND_MSG:
                            innerListener.reviseProgressbar(msg.arg1);
                            break;
                        case SEND_STOP:
                            if (pt != null) pt.preparedStop();
                            break;
                    }
                }
            };
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        log("onStartCommand...");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        log("destroy");
        super.onDestroy();
    }

    private void makeMusicOrder(int pos) {
        log("makeMusicOrder...");

        if (order == null)
            order = new int[musics.size()];

        position = 0;
        order[position] = pos;

        if (isSeq) {
            //순차일 경우
            makeSequentialOrder(pos);
        } else {
            boolean[] visited = new boolean[musics.size()];
            visited[pos] = true;
            makeRandomOrder(visited);
        }
    }

    private void makeRandomOrder(boolean[] visited) {
        log("make Random Order...");
        Random rand = new Random();

        for (int i = 1; i < musics.size(); i++) {
            order[i] = getIdx(visited, rand);
        }

        StringBuilder sb = new StringBuilder();

        for (int i : order) {
            sb.append(i + " ");
        }

        log("order: " + sb.toString());
    }

    private int getIdx(boolean[] visited, Random rand) {
        while (true) {
            int idx = rand.nextInt(visited.length);
            if (!visited[idx]) {
                visited[idx] = true;
                return idx;
            }
        }
    }

    private void makeSequentialOrder(int start) {
        log("make Sequential Order...");
        for (int i = 1; i < musics.size(); i++) {
            order[i] = (i + start) % musics.size();
        }

        StringBuilder sb = new StringBuilder();

        for (int i : order) {
            sb.append(i + " ");
        }
        log("order: " + sb.toString());
    }

    public void setInnerListener(InnerListener mListener) {
        this.innerListener = mListener;
    }

    //음악을 준비하는 메소드
    private ArrayList<Music> preparedMusicList() {
        log("preparedMusic...");
        ArrayList<Music> ret;
        DBHelper helper = new DBHelper(getApplicationContext());
        SQLiteDatabase db = helper.getReadableDatabase();

        Cursor cursor = db.rawQuery("select * from tb_m", null);
        if (cursor.getCount() == 0) {
            //DB에 데이터가 존재하지 않을 경우
            return initialMusicList();
        } else {
            //DB에 데이터가 존재한 경우
            ret = new ArrayList<>();
            while (cursor.moveToNext()) {
                String id = cursor.getString(0);
                String title = cursor.getString(1);
                String artist = cursor.getString(2);
                int duration = cursor.getInt(3);
                int albumid = cursor.getInt(4);
                String path = cursor.getString(5);

                ret.add(new Music(id, title, artist, duration, albumid, path));
            }
        }

        return ret;
    }

    //초기 음악 리스트
    private ArrayList<Music> initialMusicList() {
        ArrayList<Music> ret = new ArrayList<>();

        String[] proj = new String[]{MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Artists.ARTIST,
                MediaStore.Audio.AudioColumns.DURATION
                , MediaStore.Audio.AudioColumns.DATA
                , MediaStore.Audio.AlbumColumns.ALBUM_ID};

        String selection = MediaStore.Files.FileColumns.MIME_TYPE + "=?";
        String[] selectionArgs = new String[]{MimeTypeMap.getSingleton().getMimeTypeFromExtension("mp3")};

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
    public void judgeAction(Music music, int pos) {
        String current = music.getId();
        if (current_id == null || !current_id.equals(current)) {
            //새로운 음악
            log("start music..");
            makeMusicOrder(pos); //음악 재생 순서를 만든다.
            preparedMusic();
        } else {
            if (mp.isPlaying()) {
                log("pause music...");
                innerListener.pauseMusic();
                mp.pause();
                handler.sendEmptyMessage(SEND_STOP);
            } else {
                log("restart music...");
                innerListener.restartMusic();
                mp.start();

                pt = new ProgressThread();
                pt.start();
            }
        }
    }

    @Override
    public void forwardMusic() {
        log("forward music...");
        //다음 곡
        position += 1 % musics.size();
        preparedMusic();
    }

    @Override
    public void rewindMusic() {
        log("rewind music...");
        if(position - 1 < 0)
            position = musics.size()-1;
        else
            position -= 1;

        preparedMusic();
    }

    private void preparedMusic(){
        if(mp.isPlaying()) {
            mp.pause();
            handler.sendEmptyMessage(SEND_STOP);
        }

        try {
            mp.reset();
            mp.setDataSource(this,Uri.parse(musics.get(order[position]).getPath()));
            mp.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<Music> getMusicList() {
        return this.musics;
    }


    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        log("onError....");
        return true;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        log("onPrepared...");

        current_id = musics.get(order[position]).getId();

        mp.start();
        innerListener.startMusic(musics.get(order[position]));
        pt = new ProgressThread();
        pt.start();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        log("onCompletion...");
        //노래가 완료된 경우

        position = (position + 1) % musics.size();

        try {
            mp.reset();
            mp.setDataSource(this, Uri.parse(musics.get(order[position]).getPath()));
            mp.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }

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

    private class ProgressThread extends Thread {
        private boolean stop = false;

        public void preparedStop() {
            stop = true;
        }

        @Override
        public void run() {
            log("===== Thread start =====");
            while (!stop) {
                Message msg = new Message();
                msg.what = SEND_MSG;
                msg.arg1 = mp.getCurrentPosition();
                handler.sendMessage(msg);
                SystemClock.sleep(1000);
            }
            log("===== Thread stop =====");
        }
    }

}
