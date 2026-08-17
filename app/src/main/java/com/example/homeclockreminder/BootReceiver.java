package com.example.homeclockreminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Prefs.get(context).getBoolean(Prefs.KEY_ACTIVE, false)) {
            GeofenceManager.registerSaved(context, null, null);
        }
    }
}
