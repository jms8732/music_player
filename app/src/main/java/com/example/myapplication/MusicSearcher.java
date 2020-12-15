package com.example.myapplication;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

public class MusicSearcher {
    private static final Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

    private MusicSearcher() {

    }

    public static Cursor findId(Context context) {
        String[] proj = new String[]{MediaStore.Audio.Media._ID};

        String selection = null;
        String[] selectionArgs = null;
        selection = MediaStore.Files.FileColumns.MIME_TYPE + "=?";
        selectionArgs = new String[]{MimeTypeMap.getSingleton().getMimeTypeFromExtension("mp3")};


        Cursor cursor = context.getContentResolver().query(uri, proj, selection, selectionArgs, null);

        return cursor;
    }

    public static String findDisplayName(Context context, String id) {
        String[] proj = new String[]{MediaStore.Audio.Media.DISPLAY_NAME};
        String selection = MediaStore.Audio.Media._ID + "=?";
        String[] selectionArgs = new String[]{id};

        Cursor cursor = context.getContentResolver().query(uri, proj, selection, selectionArgs, null);

        while (cursor.moveToNext()) {
            return cursor.getString(0);
        }
        return null;
    }

    public static String findArtist(Context context, String id) {
        String[] proj = new String[]{MediaStore.Audio.Artists.ARTIST};
        String selection = MediaStore.Audio.Media._ID + "=?";
        String[] selectionArgs = new String[]{id};

        Cursor cursor = context.getContentResolver().query(uri, proj, selection, selectionArgs, null);

        while (cursor.moveToNext()) {
            return cursor.getString(0);
        }
        return null;
    }

    public static int findAlbumId(Context context, String id) {
        String[] proj = new String[]{MediaStore.Audio.AlbumColumns.ALBUM_ID};
        String selection = MediaStore.Audio.Media._ID + "=?";
        String[] selectionArgs = new String[]{id};

        Cursor cursor = context.getContentResolver().query(uri, proj, selection, selectionArgs, null);

        while (cursor.moveToNext()) {
            return cursor.getInt(0);
        }
        return -1;
    }

    public static int findDuration(Context context, String id) {
        String[] proj = new String[]{MediaStore.Audio.AudioColumns.DURATION};
        String selection = MediaStore.Audio.Media._ID + "=?";
        String[] selectionArgs = new String[]{id};

        Cursor cursor = context.getContentResolver().query(uri, proj, selection, selectionArgs, null);

        while (cursor.moveToNext()) {
            return cursor.getInt(0);
        }
        return -1;
    }

    public static String findPath(Context context, String id) {
        String[] proj = new String[]{MediaStore.Audio.AudioColumns.DATA};
        String selection = MediaStore.Audio.Media._ID + "=?";
        String[] selectionArgs = new String[]{id};

        Cursor cursor = context.getContentResolver().query(uri, proj, selection, selectionArgs, null);

        while (cursor.moveToNext()) {
            return cursor.getString(0);
        }
        return null;
    }
}
