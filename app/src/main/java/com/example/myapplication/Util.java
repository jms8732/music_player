package com.example.myapplication;

public class Util {
    private static Util instance;

    public synchronized static Util getInstance(){
        if(instance == null){
            instance = new Util();
        }

        return instance;
    }

    //변경
    public static String convertTime(long duration){
        return String.valueOf(duration);
    }
}
