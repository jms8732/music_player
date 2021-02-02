package com.example.musicplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;

public class HeadMediaReceiver extends BroadcastReceiver {
    private final String TAG = "jms8732", SERVICE = "com.example.service";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_MEDIA_BUTTON)) {
            KeyEvent event = (KeyEvent) intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (event == null) {
                return;
            }

            if(event.getAction() == KeyEvent.ACTION_DOWN){
                int keyCode = event.getKeyCode();

                Log.d(TAG, "KeyCode: " + keyCode);

                Intent sIntent = new Intent(SERVICE);

                if(keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                    sIntent.putExtra("mode",1);
                else if(keyCode == KeyEvent.KEYCODE_MEDIA_NEXT)
                    sIntent.putExtra("mode",-1);

                context.sendBroadcast(sIntent);
            }
        }

    }
}
