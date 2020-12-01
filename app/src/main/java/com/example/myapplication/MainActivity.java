package com.example.myapplication;

import android.Manifest;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Intent;
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
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.skydoves.transformationlayout.TransformationLayout;

import java.io.FileDescriptor;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements OnSelectedItemListener, View.OnClickListener, onFinishListener {
    private ArrayList<MusicVO> musics = new ArrayList<>();
    private RecyclerView recyclerView;
    private TextView status_show, thumbnail_title, thumbnail_artist, music_title, music_artist, current_duration, total_duration;
    private ImageView thumbnail_play, play, music_image;
    private MusicRecyclerAdapter adapter;
    private LinearLayout belowMusicMenu;
    private TransformationLayout transformationLayout;
    private int position;
    private SeekBar music_progress, speaker;
    private AudioManager manager;
    private View music_detail;
    private SharedPreferences prefs;
    private long pressedTime;
    private MusicService mService;
    private boolean isService, isDestroy;


    private ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            mService = binder.getService();
            isService = true;

            mService.setFinishListener(MainActivity.this);
            log("connected...");


            if (!mService.isMpNull()){
                changePlayImage(true);
            }
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

        prefs = getSharedPreferences("Status", MODE_PRIVATE);
        position = prefs.getInt("pos", -1);

        manager = (AudioManager) getSystemService(AUDIO_SERVICE);

        music_detail = findViewById(R.id.targetView);
        music_detail.setOnTouchListener(new OnSwipeTouchListener(this) {
            @Override
            public void onSwipeBottom() {
                if (transformationLayout.isTransformed())
                    finishTransform();
            }
        });

        setLayoutHeight();

        recyclerView = (RecyclerView) findViewById(R.id.recycle_view);

        thumbnail_play = (ImageView) findViewById(R.id.play);
        thumbnail_play.setOnClickListener(this);

        status_show = (TextView) findViewById(R.id.status_show);
        thumbnail_title = (TextView) findViewById(R.id.thumbnail_title);
        thumbnail_artist = (TextView) findViewById(R.id.thumbnail_artist);
        belowMusicMenu = (LinearLayout) findViewById(R.id.belowMusicMenu);
        transformationLayout = (TransformationLayout) findViewById(R.id.transformation_layout);
        belowMusicMenu.setOnClickListener(this);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            init();
        } else
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 101);

        log("onCreate...");
    }

    @Override
    protected void onResume() {
        super.onResume();
        log("onResume...");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        log("onDestroy...");

        isDestroy = true;
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("pos", position);
        editor.apply();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 101) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                init();
            }
        }
    }



    //확장한 회면 높이 조절
    private void setLayoutHeight() {
        ViewGroup.LayoutParams params = music_detail.getLayoutParams();
        Point size = new Point();
        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();

        if (Build.VERSION.SDK_INT >= 17)
            display.getRealSize(size);
        else
            display.getSize(size);

        params.height = size.y;
    }

    //안드로이드 내에 존재하는 파일들을 탐색하여 확장자 mp3를 가진 파일들을 찾는 메소드
    private void searchMusicPath() {
        final Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        final String[] projection = new String[]{MediaStore.Audio.Media.DISPLAY_NAME
                , MediaStore.Audio.Artists.ARTIST
                , MediaStore.Audio.AlbumColumns.ALBUM_ID
                , MediaStore.Audio.AudioColumns.DURATION
                , MediaStore.Audio.AudioColumns.DATA};

        String selection = MediaStore.Files.FileColumns.MIME_TYPE + "=?";
        String[] selectionArgs = new String[]{MimeTypeMap.getSingleton().getMimeTypeFromExtension("mp3")};

        Cursor cursor = this.getContentResolver().query(uri, projection, selection, selectionArgs, null);

        while (cursor != null && cursor.moveToNext()) {
            String title = cursor.getString(0);
            String artist = cursor.getString(1);
            int album_id = cursor.getInt(2);
            int duration = cursor.getInt(3);
            String data = cursor.getString(4);

            musics.add(new MusicVO(title, duration, artist, album_id, data));
        }

        //마지막에 '맨 위로' 처리
        musics.add(null);
    }

    @Override
    public void finish(boolean fin) {
        //노래가 끝난 경우
        current_duration.setText(convertDuration(0));
        music_progress.setProgress(0);

        mService.musicPause();
    }

    //뷰 세팅
    private void init() {
        adapter = new MusicRecyclerAdapter(this, recyclerView);

        searchMusicPath();
        if (!musics.isEmpty()) {
            status_show.setVisibility(View.GONE);
            adapter.setList(musics);
            adapter.setItemListener(this);

            recyclerView.setAdapter(adapter);
            recyclerView.setLayoutManager(new CenterLayoutManager(this));
        }

        setLayout();
        setPermission();

    }

    //확장된 레이아웃 세팅
    private void setLayout() {
        play = (ImageButton) music_detail.findViewById(R.id.detail_play);
        play.setOnClickListener(this);
        music_title = (TextView) music_detail.findViewById(R.id.music_title);
        music_title.setSelected(true);
        music_artist = (TextView) music_detail.findViewById(R.id.artist);
        music_artist.setSelected(true);

        current_duration = (TextView) music_detail.findViewById(R.id.current_duration);
        current_duration.setText(convertDuration(0));

        total_duration = (TextView) music_detail.findViewById(R.id.total_duration);
        music_image = (ImageView) music_detail.findViewById(R.id.music_image);

        speaker = (SeekBar) music_detail.findViewById(R.id.speaker);
        music_progress = (SeekBar) music_detail.findViewById(R.id.music_progress);

        int vol = manager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int max = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        speaker.setProgress(vol);
        speaker.setMax(max);

        if (position != -1) { //이전에 한번이라도 튼 경우
            MusicVO musicVO = musics.get(position);

            recyclerView.smoothScrollToPosition(position);

            belowMusicMenu.setVisibility(View.VISIBLE);
            thumbnail_title.setText(musicVO.getTitle());
            thumbnail_title.setSelected(true); //marquee 진행
            thumbnail_artist.setText(musicVO.getArtist());
            music_title.setText(musicVO.getTitle());
            music_artist.setText(musicVO.getArtist());
            total_duration.setText(convertDuration(musicVO.getDuration()));

            music_progress.setMax(musicVO.getDuration());

            Glide.with(this)
                    .load(getAlbumart(musicVO.getAlbum_id()))
                    .centerInside()
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .into(music_image);
            transformationLayout.bindTargetView(music_detail);

        }

        log("setLayout...");

    }

    private void setPermission() {
        //화면 전환을 permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //시스템에 쓰기 위한
            if (!Settings.System.canWrite(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + this.getPackageName()));
                startActivity(intent);
            }
        } else {
            //시스템에 쓰기 위한
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + this.getPackageName()));
            startActivity(intent);
        }
    }

    //선택된 음악 화면
    @Override
    public void ChangeLayout(int position) {
        MusicVO musicVO = musics.get(position);
        belowMusicMenu.setVisibility(View.VISIBLE);
        thumbnail_title.setText(musicVO.getTitle());
        thumbnail_title.setSelected(true); //marquee 진행
        thumbnail_artist.setText(musicVO.getArtist());

        music_artist.setText(musicVO.getArtist());
        music_title.setText(musicVO.getTitle());
        total_duration.setText(convertDuration(musicVO.getDuration()));
        music_progress.setMax(musics.get(position).getDuration());

        mService.setMusic(musics.get(position).getPath());
        changePlayImage(true);

        Glide.with(this)
                .load(getAlbumart(musicVO.getAlbum_id()))
                .centerInside()
                .placeholder(R.drawable.ic_launcher_foreground)
                .into(music_image);

        this.position = position;

        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("pos", position);
        editor.apply();
    }

    //이미지 변경 메소드
    private void changePlayImage(boolean b) {
        if (b) {
            play.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.pause, null));
            thumbnail_play.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.pause, null));
            ProgressAsync async = new ProgressAsync();
            async.execute();

        } else {
            play.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.play, null));
            thumbnail_play.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.play, null));
        }
    }


    @Override
    public void onClick(View v) {
        if (v == play || v == thumbnail_play) {
            if (isService) {
                //서비스 중이라면
                if (!mService.isMpNull()) {
                    //MediaPlayer가 있는 경우
                    if (mService.isPlaying()) {
                        changePlayImage(false);
                        mService.musicPause();
                    } else {
                        changePlayImage(true);
                        mService.musicStart();
                    }
                } else {
                    mService.setMusic(musics.get(position).getPath());
                    changePlayImage(true);
                }
            } else {
                //서비스 중이 아니면
            }
        } else if (v == belowMusicMenu) {
            if (!transformationLayout.isTransformed()) {
                transformationLayout.startTransform();
            } else {
                transformationLayout.finishTransform();
            }
        }
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

    private void finishTransform() {
        transformationLayout.finishTransform();
    }

    private void log(String s) {
        Log.d("jms8732", s);
    }

    //1000 millisec = 1sec;
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

    class ProgressAsync extends AsyncTask<Void, Integer, Void> {
        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            int current = values[0];
            current_duration.setText(convertDuration(current));
            music_progress.setProgress(current);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            log("donInbackground start...");
            while (mService.isPlaying() && !isDestroy) {
                publishProgress(mService.getCurrentDuration() + 1000);
                SystemClock.sleep(1000);
            }
            log("donInbackground end...");
            return null;
        }
    }
}