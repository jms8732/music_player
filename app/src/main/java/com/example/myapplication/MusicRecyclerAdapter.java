package com.example.myapplication;

import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.icu.text.DecimalFormat;
import android.icu.text.NumberFormat;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.FileDescriptor;
import java.util.ArrayList;
import java.util.Random;

public class MusicRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements myPositionListener {
    private ArrayList<MusicVO> list;
    private Context context;
    private final int TYPE_MUSIC = 1, TYPE_LAST = 0;
    private RecyclerView recyclerView;
    private OnSelectedItemListener itemListener;
    public static String id;
    private MusicService mService;

    private ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((MusicService.MusicBinder) service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    public MusicRecyclerAdapter(Context context, RecyclerView recyclerView) {
        this.context = context;
        this.recyclerView = recyclerView;

        Intent intent = new Intent(context, MusicService.class);
        context.bindService(intent, conn, Context.BIND_AUTO_CREATE);
    }

    public void setList(ArrayList<MusicVO> list) {
        this.list = list;
    }


    @Override
    public int getItemViewType(int position) {
        return list.get(position) != null ? TYPE_MUSIC : TYPE_LAST;
    }

    public void setItemListener(OnSelectedItemListener listener) {
        this.itemListener = listener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_MUSIC) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.music_view, parent, false);
            MusicHolder holder = new MusicHolder(view);
            holder.setListener(this);

            return holder;
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.music_last_view, parent, false);
            LastHolder holder = new LastHolder(view);
            holder.setListener(this);

            return holder;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof MusicHolder) {
            String title = list.get(position).getTitle();
            String artist = list.get(position).getArtist();
            String duration = convertDuration(list.get(position).getDuration());

            ((MusicHolder) holder).title.setText(title);
            ((MusicHolder) holder).artist.setText(artist);
            ((MusicHolder) holder).duration.setText(duration);

            Glide.with(context)
                    .load(getAlbumart(list.get(position).getAlbum_id()))
                    .fitCenter()
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .into(((MusicHolder) holder).image);
        } else {

        }
    }

    @Override
    public void getPosition(int position) {
        if (list.get(position) == null) {
            //마지막인 경우
            recyclerView.getLayoutManager().scrollToPosition(0);
        } else {
            log("id : " + id);
            if (id == null) {
                //맨 처음 터치
                id = list.get(position).getId();

                this.itemListener.ChangeLayout(position);

                Intent intent = new Intent(context, MusicService.class);
                intent.putExtra("data", list.get(position));
                intent.putExtra("array", makeOrder(position));

                if (Build.VERSION_CODES.O <= Build.VERSION.SDK_INT) {
                    context.startForegroundService(intent);
                } else
                    context.startService(intent);
            } else {
                if (isSameMusicPlaying(id, list.get(position).getId())) {
                    //현재 동일한 음악을 누른 경우
                    if (mService.isPlaying()) {
                        mService.musicPause();
                    } else
                        mService.musicStart();
                    this.itemListener.ChangeStatus(mService.isPlaying());
                } else {
                    this.itemListener.ChangeLayout(position);

                    Intent intent = new Intent(context, MusicService.class);
                    intent.putExtra("data", list.get(position));
                    intent.putExtra("array", makeOrder(position));

                    if (Build.VERSION_CODES.O <= Build.VERSION.SDK_INT) {
                        context.startForegroundService(intent);
                    } else
                        context.startService(intent);
                }
                id = list.get(position).getId();
            }
        }
    }


    public void setId(String id) {
        this.id = id;
    }

    //순서를 만드는 메소드
    private String makeOrder(int first) {
        StringBuilder sb = new StringBuilder();
        boolean[] visited = new boolean[list.size() - 1];
        visited[first] = true;

        sb.append(list.size() - 1 + " ");
        sb.append(first + " ");

        for (int i = 1; i < visited.length; i++) {
            sb.append(getIdx(visited) + " ");
        }

        return sb.toString().trim();
    }

    private int getIdx(boolean visited[]) {
        Random rand = new Random();
        while (true) {
            int idx = rand.nextInt(visited.length);
            if (!visited[idx]) {
                visited[idx] = true;
                return idx;
            }
        }
    }

    private boolean isSameMusicPlaying(String previous_id, String current_id) {
        if (previous_id.equals(current_id))
            return true;

        return false;
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
    public Bitmap getAlbumart(long album_id) {
        Bitmap bm = null;
        try {
            final Uri sArtworkUri = Uri
                    .parse("content://media/external/audio/albumart");

            Uri uri = ContentUris.withAppendedId(sArtworkUri, album_id);

            ParcelFileDescriptor pfd = context.getContentResolver()
                    .openFileDescriptor(uri, "r");

            if (pfd != null) {
                FileDescriptor fd = pfd.getFileDescriptor();
                bm = BitmapFactory.decodeFileDescriptor(fd);
            }
        } catch (Exception e) {
        }
        return bm;
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}
