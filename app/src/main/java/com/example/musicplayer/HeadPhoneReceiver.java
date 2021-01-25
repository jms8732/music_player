package com.example.musicplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class HeadPhoneReceiver extends BroadcastReceiver {
    private final String TAG = "jms8732", SERVICE = "com.example.service";
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive: " + intent.getAction());

        int state = intent.getIntExtra("state",-1);

        if(intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
            switch (state) {
                case 0:
                    Intent sIntent = new Intent(SERVICE);
                    sIntent.putExtra("mode", 0);
                    context.sendBroadcast(sIntent);
                    break;
            }
        }
    }
}
