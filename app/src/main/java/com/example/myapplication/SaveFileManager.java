package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.StringTokenizer;

import static android.content.Context.MODE_PRIVATE;

public class SaveFileManager {
    private static SharedPreferences prefs;

    public SaveFileManager(Context context){
        if(prefs == null)
            prefs = context.getSharedPreferences("music", MODE_PRIVATE);
    }

    public boolean loadLoopStatus(){
        return prefs.getBoolean("loopStatus",false);
    }

    public boolean loadOrderStatus(){
        return prefs.getBoolean("orderStatus",true);
    }

    public int loadPosition() {
        return prefs.getInt("pos", -1);
    }

    public int[] loadOrder() {
        String order = prefs.getString("order", null);
        int[] ret = null;

        if (order != null) {
            StringTokenizer st = new StringTokenizer(order);
            ret = new int[Integer.parseInt(st.nextToken())];

            for (int i = 0; i < ret.length; i++) {
                ret[i] = Integer.parseInt(st.nextToken());
            }
        }

        return ret;
    }

    public void saveLoopStatus(boolean loopStatus){
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("loopStatus",loopStatus);
        editor.apply();
    }

    public void saveOrderStatus(boolean orderStatus){
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("orderStatus",orderStatus);
        editor.apply();
    }

    public void savePoint(int position) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt("pos", position);
        editor.apply();
    }

    public void saveOrder(int[] music_order) {
        SharedPreferences.Editor editor = prefs.edit();
        StringBuilder sb = new StringBuilder();
        sb.append(music_order.length + " ");

        for (int i : music_order) {
            sb.append(i + " ");
        }

        editor.putString("order", sb.toString().trim());
        editor.apply();
    }
}
