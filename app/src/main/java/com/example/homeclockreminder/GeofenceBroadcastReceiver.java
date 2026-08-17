package com.example.homeclockreminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        GeofencingEvent event = GeofencingEvent.fromIntent(intent);
        if (event == null || event.hasError()) return;

        SharedPreferences p = Prefs.get(context);
        if (!p.getBoolean(Prefs.KEY_ACTIVE, false)) return;

        int transition = event.getGeofenceTransition();
        if (transition == Geofence.GEOFENCE_TRANSITION_ENTER && p.getBoolean(Prefs.KEY_ENTER, true)) {
            NotificationHelper.show(context, true);
        } else if (transition == Geofence.GEOFENCE_TRANSITION_EXIT && p.getBoolean(Prefs.KEY_EXIT, true)) {
            NotificationHelper.show(context, false);
        }
    }
}
