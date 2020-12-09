package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
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

    public ArrayList<String> loadOrder() {
        String order = prefs.getString("order", null);

        if(order == null)
            return null;

        ArrayList<String> ret = new ArrayList<>();

        StringTokenizer st = new StringTokenizer(order);

        while(st.hasMoreElements()){
            ret.add(st.nextToken());
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

    public void saveOrder(ArrayList<String> order) {
        SharedPreferences.Editor editor = prefs.edit();
        StringBuilder sb = new StringBuilder();

        for(String id : order)
            sb.append(id + " ");

        editor.putString("order", sb.toString().trim());
        editor.apply();
    }
}
