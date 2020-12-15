package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.StringTokenizer;

import static android.content.Context.MODE_PRIVATE;

public class SaveFileManager {
    private static SharedPreferences prefs;

    public SaveFileManager(Context context) {
        if (prefs == null)
            prefs = context.getSharedPreferences("music", MODE_PRIVATE);
    }

    public boolean loadLoopStatus() {
        return prefs.getBoolean("loopStatus", false);
    }

    public boolean loadOrderStatus() {
        return prefs.getBoolean("orderStatus", true);
    }

    public String loadMusicList() {
        return prefs.getString("musicList", null);
    }

    public String loadId() {
        return prefs.getString("id", null);
    }

    public void saveLastId(String id) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("id", id);
        editor.apply();
    }


    public void saveLoopStatus(boolean loopStatus) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("loopStatus", loopStatus);
        editor.apply();
    }

    public void saveOrderStatus(boolean orderStatus) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("orderStatus", orderStatus);
        editor.apply();
    }

    public void saveMusicList(ArrayList<String> musicList) {
        SharedPreferences.Editor editor = prefs.edit();
        StringBuilder sb=  new StringBuilder();

        for(int i =0 ; i  < musicList.size()-1 ; i++)
            sb.append(musicList.get(i) + " ");

        editor.putString("musicList",sb.toString().trim());
        editor.apply();
    }

    private String removeDuplicate(String basic, String removed) {
        HashSet<String> set = new HashSet<>();
        StringBuilder sb = new StringBuilder();
        if (basic != null) {
            String[] temp = basic.split(",");

            for (String s : temp)
                set.add(s);
        }

        set.add(removed);
        for (String s : set)
            sb.append(s + ",");


        sb.deleteCharAt(sb.length()-1);
        return sb.toString();
    }
}
