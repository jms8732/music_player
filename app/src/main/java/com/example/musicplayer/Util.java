package com.example.musicplayer;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.view.View;
import android.widget.ImageView;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;
import androidx.databinding.BindingAdapter;

import com.squareup.picasso.Picasso;

public class Util {
    private static Util instance;
    private String channelId = "JChannel";
    private final String receiverName = "com.example.service";

    public static synchronized Util getInstance(){
        if(instance == null)
            instance = new Util();

        return instance;
    }


    //1000 millisec = 1sec;
    //todo total Duration 올림 표기
    public static String convertDuration(int duration) {
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

    public Notification buildNotification(Context context, Music music, boolean isPlay) {
        NotificationCompat.Builder builder = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new NotificationCompat.Builder(context, channelId);
        } else
            builder = new NotificationCompat.Builder(context);

        builder.setWhen(System.currentTimeMillis());
        builder.setContent(buildRemoveViews(context,music,isPlay));
        builder.setContentText("Play");
        builder.setSmallIcon(R.drawable.music);
        builder.setAutoCancel(false);

        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pIntent = PendingIntent.getActivity(context, 101, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pIntent);

        return builder.build();
    }

    private RemoteViews buildRemoveViews(Context context, Music music, boolean isPlay) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.remote_view);

        remoteViews.setTextViewText(R.id.remote_view_titie, music.getTitle());
        remoteViews.setTextViewText(R.id.remote_view_artist, music.getArtist());

        Uri image =ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"),music.getAlbum());

        if (image != null) {
            remoteViews.setImageViewUri(R.id.remote_view_image,image);
        } else
            remoteViews.setImageViewResource(R.id.remote_view_image, R.drawable.ic_launcher_foreground);

        if (isPlay) {
            remoteViews.setImageViewResource(R.id.remote_view_play, R.drawable.pause);
        } else
            remoteViews.setImageViewResource(R.id.remote_view_play, R.drawable.play);


        Intent intent = new Intent(receiverName);
        intent.putExtra("mode", 1);
        PendingIntent pIntent = PendingIntent.getBroadcast(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.remote_view_play, pIntent);

        intent = new Intent(receiverName);
        intent.putExtra("mode", -1);
        pIntent = PendingIntent.getBroadcast(context, -1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.remote_view_forward, pIntent);

        intent = new Intent(receiverName);
        intent.putExtra("mode", -2);
        pIntent = PendingIntent.getBroadcast(context, -2, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.remote_view_rewind, pIntent);

        intent = new Intent(receiverName);
        intent.putExtra("mode", -3);
        pIntent = PendingIntent.getBroadcast(context, -3, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.remote_view_close, pIntent);

        return remoteViews;
    }

}
