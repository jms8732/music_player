package com.example.myapplication;

import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.FileDescriptor;

//연산에 필요한 메소드
public class Util {

    private Util(){

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


    //mp3의 섬네일
    public static Bitmap getAlbumart(Context context, long album_id) {
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
}
