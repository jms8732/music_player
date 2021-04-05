package com.example.myapplication;

import java.text.DecimalFormat;
import java.util.zip.DeflaterInputStream;

public class Util {
    private static Util instance;

    public synchronized static Util getInstance() {
        if (instance == null) {
            instance = new Util();
        }

        return instance;
    }

    public static String convertTime(long duration) {

        long hour = duration / (60 * 60 * 1000);
        long min = (duration %  (60 * 60 * 1000))  / (60 * 1000);
        long sec = ((duration % ( 60 * 60 * 1000) % (60 * 1000))) / 1000;

        StringBuilder sb = new StringBuilder();

        if(hour != 0){
            sb.append(String.format("%02d",hour)).append(String.format("%02d",min)).append(":").append(String.format("%02d",sec));
        }else
            sb.append(String.format("%02d",min)).append(":").append(String.format("%02d",sec));

        return sb.toString();
    }
}
