package com.example.musicplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;

public class HeadPhoneReceiver extends BroadcastReceiver {
    private final String TAG = "jms8732", SERVICE = "com.example.service";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive: " + intent.getAction());

        int state = intent.getIntExtra("state", -1);

        if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
            switch (state) {
                case 0:
                    Intent sIntent = new Intent(SERVICE);
                    sIntent.putExtra("mode", 0);
                    context.sendBroadcast(sIntent);
                    break;
            }
        } else if (intent.getAction().equals(Intent.ACTION_MEDIA_BUTTON)) {
            KeyEvent event = (KeyEvent) intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (event == null) {
                return;
            }

            int keycode = event.getKeyCode();
            int action = event.getAction();
            long eventtime = event.getEventTime();

            if (action == KeyEvent.ACTION_DOWN) {
                Log.d(TAG, "onReceive : " + action);
            }
        }
    }
}
