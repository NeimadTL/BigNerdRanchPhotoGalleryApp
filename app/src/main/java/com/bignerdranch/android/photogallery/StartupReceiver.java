package com.bignerdranch.android.photogallery;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by Neimad on 18/04/2017.
 */

public class StartupReceiver extends BroadcastReceiver
{



    public static final String TAG = "StartupReceiver";



    @Override
    public void onReceive(Context context, Intent intent)
    {
        Log.i(TAG, "Received braodcast intent: " + intent.getAction());

        boolean isOn = QueryPreferences.isAlarmOn(context);
        PollService.setServiceAlarm(context, isOn);
    }
}
