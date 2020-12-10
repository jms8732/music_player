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
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.FileDescriptor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class MusicRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements myPositionListener, ItemMoveCallback.ItemTouchHelperAdapter {
    private ArrayList<String> list;
    private Context context;
    private final int TYPE_MUSIC = 1, TYPE_LAST = 0;
    private RecyclerView recyclerView;
    private onStartDragListener dragListener;
    private boolean drag;

    public MusicRecyclerAdapter(Context context, RecyclerView recyclerView, onStartDragListener dragListener) {
        this.context = context;
        this.recyclerView = recyclerView;
        this.dragListener = dragListener;
    }

    public void setList(ArrayList<String> list) {
        this.list = list;
    }


    @Override
    public int getItemViewType(int position) {
        return list.get(position) != null ? TYPE_MUSIC : TYPE_LAST;

    }

    @Override
    public void onItemMove(int fromPos, int targetPos) {
        Collections.swap(list, fromPos, targetPos);
        notifyItemMoved(fromPos, targetPos);
    }

    @Override
    public void onItemDismiss(int pos) {
        Log.d("jms8732","dismiss.....");
        String tar = list.get(pos);
        list.remove(pos);
        notifyItemRemoved(pos);

        Intent intent = new Intent("com.example.service");
        intent.putStringArrayListExtra("list", list);
        intent.putExtra("code",2);
        intent.putExtra("target",tar);

        context.sendBroadcast(intent);
    }

    @Override
    public void onFinishDrag(int actionState) {
        if(actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
            drag = true;
        }else if(actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            drag = false;
        } else if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
            Log.d("jms8732","finish..........");
            if(drag){
                Log.d("jms8732","drag.....");
                //이동이 끝난 후
                Intent intent = new Intent("com.example.service");
                intent.putStringArrayListExtra("list", list);
                intent.putExtra("code",1);

                context.sendBroadcast(intent);
            }
        }
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
    public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof MusicHolder) {
            String id = list.get(position);

            String title = MusicSearcher.findDisplayName(context, id);
            String artist = MusicSearcher.findArtist(context, id);
            String duration = convertDuration(MusicSearcher.findDuration(context, id));
            int albumId = MusicSearcher.findAlbumId(context, id);

            ((MusicHolder) holder).title.setText(title);
            ((MusicHolder) holder).artist.setText(artist);
            ((MusicHolder) holder).duration.setText(duration);
            ((MusicHolder) holder).moveButton.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) {
                        Log.d("jms8732", "Action down.........");
                        dragListener.onStartDrag(holder);

                    } else if (event.getAction() == MotionEvent.ACTION_UP) {
                        Log.d("jms8732", "Action up.........");
                    }
                    return true;
                }
            });

            Glide.with(context)
                    .load(getAlbumart(albumId))
                    .fitCenter()
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .into(((MusicHolder) holder).image);
        }
    }

    @Override
    public void getPosition(int position) {
        if (list.get(position) == null) {
            //마지막인 경우
            recyclerView.getLayoutManager().scrollToPosition(0);
        } else {
            Intent sIntent = new Intent(context, MusicService.class);
            sIntent.putExtra("data", list.get(position));
            sIntent.putExtra("code", 1);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                context.startForegroundService(sIntent);
            else
                context.startService(sIntent);
        }
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

        if (bm == null)
            bm = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher_foreground);

        return bm;
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}
