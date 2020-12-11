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

    public String loadRemovedMusic() {
        return prefs.getString("removed", null);
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

    public void saveRemovedMusic(String removed) {
        SharedPreferences.Editor editor = prefs.edit();
        String basic = loadRemovedMusic(); //기존에 있는 값을 가져옴
        editor.putString("removed", removeDuplicate(basic,removed));
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
