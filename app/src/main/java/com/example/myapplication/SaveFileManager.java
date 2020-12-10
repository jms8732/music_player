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

    public ArrayList<String> loadShowList() {
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

    public String loadId(){
        return prefs.getString("id",null);
    }

    public void saveLastId(String id){
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("id",id);
        editor.apply();
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

    public void saveMusicList(ArrayList<String> showList) {
        SharedPreferences.Editor editor = prefs.edit();
        StringBuilder sb = new StringBuilder();

        for(String id : showList)
            sb.append(id + " ");

        editor.putString("order", sb.toString().trim());
        editor.apply();
    }
}
