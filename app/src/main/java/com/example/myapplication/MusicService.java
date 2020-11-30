package com.example.myapplication;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentUris;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
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

public class MusicService extends Service {
    private MediaPlayer mp = null;
    private MusicBinder binder = new MusicBinder();

    @Override
    public void onCreate() {
        super.onCreate();
        log("Service onCreate...");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        log("Service onDestroy....");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        log("Service onStartCommand.....");

        Notification noti = makeNotification(intent);
        startForeground(2, noti);

        setMusic(intent);
        return START_STICKY;
    }

    //Mediaplayer 설정
    private void setMusic(Intent intent){
        MusicVO musicVO = intent.getParcelableExtra("data");

        //이전에 존재한 Mediaplayer 멈춤
        if(mp != null && mp.isPlaying())
            mp.stop();

        mp = MediaPlayer.create(this,Uri.parse(musicVO.getPath()));
    }

    public MediaPlayer getMp(){
        return mp;
    }

    private Notification makeNotification(Intent intent) {
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

        Intent aIntent = new Intent(this, MusicActivity.class);
        aIntent.putExtra("data", intent.getParcelableExtra("data"));

        PendingIntent pIntent = PendingIntent.getActivity(this, 101, aIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        MusicVO musicVO = intent.getParcelableExtra("data");

        //todo RemoteView add
        builder.setContentTitle(musicVO.getTitle());
        builder.setContentText(musicVO.getArtist());
        builder.setSmallIcon(R.drawable.play);
        builder.setContentIntent(pIntent);
        builder.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_LIGHTS);
        builder.setVibrate(new long[]{0});

        RemoteViews contentView = new RemoteViews(getPackageName(),R.layout.custom_remoteview);
        settingRemoteView(contentView, intent);
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
    private void settingRemoteView(RemoteViews contentView,Intent intent){
        MusicVO musicVO = intent.getParcelableExtra("data");
        contentView.setTextViewText(R.id.remoteView_title,musicVO.getTitle());
        contentView.setTextViewText(R.id.remoteView_artist,musicVO.getArtist());

        Bitmap bitmap = getAlbumart(musicVO.getAlbum_id());

        if(bitmap == null)
            bitmap = BitmapFactory.decodeResource(getResources(),R.drawable.volume);
        contentView.setImageViewBitmap(R.id.remoteView_image,bitmap);
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
