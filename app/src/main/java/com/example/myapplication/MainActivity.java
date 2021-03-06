package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.example.myapplication.databinding.ActivityMainBinding;
import com.skydoves.transformationlayout.TransformationCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class MainActivity extends AppCompatActivity implements clickAdapter, View.OnClickListener, SwipeAdapter {
    private static final String TAG = "jms8732";
    private ActivityMainBinding binding;
    private MusicService mService;
    private SharedPreferences pref;
    private MusicAdapter adapter;
    private long pressedTime = 0L;
    private InputMethodManager manager;


    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String mode = intent.getStringExtra("mode");

            switch (mode) {
                case "start":
                    int pos = intent.getIntExtra("pos", 0);
                    binding.recycler.scrollToPosition(pos);
                    invalidateStartView();
                    break;
                case "restart":
                    invalidateRestartView();
                    break;
                case "pause":
                    invalidatePauseView();
                    break;

                case "complete":
                    invalidateCompleteView();
                    break;
            }
        }
    };

    private ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "Service connected...");
            MusicService.MyBinder binder = (MusicService.MyBinder) service;
            mService = binder.getInstance();

            initSettings();
            List<Music> temp = loadMusicList();
            invalidate(temp);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "Service disconnected...");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        TransformationCompat.onTransformationStartContainer(this);
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        checkingPermission();
        registerReceiver(receiver, new IntentFilter("com.example.activity"));
    }

    //뷰 갱신
    private void invalidate(List<Music> musics) {
        MusicClickListener musicClickListener = new MusicClickListener(this);

        binding.setItem(musicClickListener);
        binding.recycler.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        adapter = new MusicAdapter(musics, musicClickListener);
        binding.recycler.setAdapter(adapter);
        binding.recycler.setHasFixedSize(true);
        binding.recycler.setNestedScrollingEnabled(false);

        ItemTouchHelper helper = new ItemTouchHelper(new SwipeHelper(getApplicationContext(), this));
        helper.attachToRecyclerView(binding.recycler);

        int previous = pref.getInt("previous", 0);
        binding.recycler.scrollToPosition(previous);

        binding.getMusic().setIsplaying(true);
        if (mService.isPlaying()) {
            invalidatePlay();
            binding.getMusic().setActivate(true);
        } else {
            invalidatePause();
        }
    }


    //음악 실행 뷰 갱신
    private void invalidateStartView() {
        binding.setMusic(mService.getMusic());
        binding.getMusic().setActivate(true);
        binding.getMusic().setIsplaying(true);

        invalidatePlay();
    }

    //음악 재실행 뷰 갱신
    private void invalidateRestartView() {
        binding.getMusic().setActivate(true);

        invalidatePlay();
    }

    //음악 일시정지 뷰 갱신
    private void invalidatePauseView() {
        binding.getMusic().setActivate(false);

        invalidatePause();
    }

    //음악 재생완료 뷰 갱신
    private void invalidateCompleteView() {
        binding.getMusic().setActivate(false);
        binding.getMusic().setIsplaying(false);
        binding.getMusic().setCurrentDuration(0);

        invalidatePause();
    }

    @Override
    public void rawClick(Music next) {
        String cur_id = binding.getMusic().getId();
        String next_id = next.getId();

        if (cur_id.equals(next_id)) {
            //현재 선택한 음악이 동일 한 경우

            if (mService.isPlaying()) {
                //노래가 플레이 중이라면
                rawPause();
            } else {
                if (mService.isPrepared()) {
                    //노래가 이전에 준비가 되었더라면
                    rawRestart();
                } else {
                    rawStart(next);
                    if (binding.search.hasFocus())
                        editTextClearFocus();
                }
            }
        } else {
            //다른 노래인 경우
            binding.getMusic().setIsplaying(false);
            binding.getMusic().setActivate(false);
            rawStart(next);

            if (binding.search.hasFocus())
                editTextClearFocus();
        }
    }


    //노래 재생
    private void rawStart(Music music) {
        mService.start(music);
    }

    //노래 다시 재생
    private void rawRestart() {
        mService.restart();
    }

    //노래 일시 중지
    private void rawPause() {
        mService.pause();
    }

    //뷰 갱신
    private void invalidatePlay() {
        binding.menuPlay.setImageResource(R.drawable.pause);
        binding.playButton.setImageResource(R.drawable.pause_circle_out);
    }

    //뷰 갱신
    private void invalidatePause() {
        binding.menuPlay.setImageResource(R.drawable.play);
        binding.playButton.setImageResource(R.drawable.play_circle_out);
    }

    //음악 리스트를 호출
    private List<Music> loadMusicList() {
        List<Music> ret = new ArrayList<>();
        String deletes = pref.getString("delete", null);

        String[] proj = new String[]{MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Artists.ARTIST,
                MediaStore.Audio.AudioColumns.DURATION,
                MediaStore.Audio.AudioColumns.DATA,
                MediaStore.Audio.Albums.ALBUM_ID};

        String selection = null;
        String[] selectionArgs = null;

        if (deletes != null) {
            String[] split = deletes.split(" ");
            selection = MediaStore.Audio.Media._ID + " not in (" + TextUtils.join(", ", split) + ")";
        }

        Cursor cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, proj, selection, selectionArgs, null);

        int index = 0;
        while (cursor.moveToNext()) {
            String id = cursor.getString(0);
            String title = cursor.getString(1);

            String[] split = title.split("\\.", 2);
            title = split[0];
            String extension = split[1];

            String artist = cursor.getString(2);
            int duration = cursor.getInt(3);
            String path = cursor.getString(4);
            long album = cursor.getLong(5);

            ret.add(new Music(title, artist, path, duration, album, id, false, index++, extension));
        }

        int previous = pref.getInt("previous", 0);

        if (mService.getMusic() != null) {
            previous = IntStream
                    .range(0, ret.size())
                    .filter(i -> ret.get(i).getId().equals(mService.getMusic().getId()))
                    .findAny()
                    .orElse(0);
        }

        //초기 설정
        binding.setMusic(ret.get(previous));


        //서비스에 음악 등록
        mService.setMusic(ret.get(previous));
        mService.setMusicList(ret);

        return ret;
    }

    //초기 설정
    private void initSettings() {
        pref = getSharedPreferences("music", MODE_PRIVATE);
        binding.transformationLayout.setOnClickListener(this);

        binding.search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        binding.search.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    binding.transformationLayout.setVisibility(View.GONE);
                } else
                    binding.transformationLayout.setVisibility(View.VISIBLE);

            }
        });

        binding.search.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    //엔터키를 누른 경우
                    editTextClearFocus();
                    return true;
                }
                return false;
            }
        });

        manager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        binding.menuForward.setOnClickListener(this);
        binding.menuRewind.setOnClickListener(this);
        binding.menuSort.setOnClickListener(this);

        binding.swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Toast.makeText(getApplicationContext(), "Refresh...", Toast.LENGTH_SHORT).show();
                SharedPreferences.Editor editor = pref.edit();
                editor.putString("delete",null);
                editor.apply();

                List<Music> newList = loadMusicList();
                adapter.refreshMusicList(newList,binding.swipeRefreshLayout,binding.recycler);
            }
        });
    }

    //권한 채크 메소드
    private void checkingPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            //권한 설정이 된 경우
            Log.d(TAG, "permission ok...");
            connectService();
        } else
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 101);
    }

    //서비스 연결
    private void connectService() {
        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, conn, 0);
        startService(intent);
    }

    @Override
    public void onClick(View v) {
        if (v == binding.transformationLayout) {
            Intent intent = new Intent(getApplicationContext(), DetailActivity.class);
            TransformationCompat.startActivity(binding.transformationLayout, intent);
        } else if (v == binding.menuRewind) {
            mService.rewind();
        } else if (v == binding.menuForward) {
            mService.forward();
        } else if (v == binding.menuSort) {
            final PopupMenu menu = new PopupMenu(getApplicationContext(), v);
            getMenuInflater().inflate(R.menu.menu_context, menu.getMenu());
            menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.artist:
                            Toast.makeText(getApplicationContext(), "아티스트", Toast.LENGTH_SHORT).show();
                            return true;
                    }
                    return false;
                }
            });

            menu.show();
        }
    }


    @Override
    public void swipeDelete(RecyclerView.ViewHolder viewHolder, int direction) {
        mService.removeMusic(viewHolder, direction, adapter, binding.recycler);
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "[Activity] onDestroy.....");
        super.onDestroy();
        unregisterReceiver(receiver);

        if (!mService.isPlaying()) {
            stopService(new Intent(this, MusicService.class));
            unbindService(conn);
        }

    }

    private void editTextClearFocus() {
        adapter.getFilter().filter("");
        binding.search.setText("");
        manager.hideSoftInputFromWindow(binding.search.getWindowToken(), 0);
        binding.transformationLayout.setVisibility(View.VISIBLE);
        binding.search.clearFocus();
    }

    @Override
    public void onBackPressed() {
        if (binding.search.hasFocus()) {
            //EditText가 포커싱 되 있으면서 뒤로가기 버튼을 누른 경우
            editTextClearFocus();
        } else {
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "permission ok...");
                connectService();
            } else
                ActivityCompat.requestPermissions(this, new String[]{permissions[0]}, 101);
        }
    }
}