package com.example.homeclockreminder;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

public final class GeofenceManager {
    public static final String GEOFENCE_ID = "home_geofence";

    private GeofenceManager() {}

    private static PendingIntent pendingIntent(Context context) {
        Intent intent = new Intent(context, GeofenceBroadcastReceiver.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_MUTABLE;
        }
        return PendingIntent.getBroadcast(context, 1001, intent, flags);
    }

    public static boolean hasRequiredLocationPermissions(Context context) {
        boolean fine = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean background = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                context.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
        return fine && background;
    }

    public static void registerSaved(Context context, Runnable onSuccess, java.util.function.Consumer<Exception> onError) {
        SharedPreferences p = Prefs.get(context);
        if (!p.getBoolean(Prefs.KEY_ACTIVE, false) || !hasRequiredLocationPermissions(context)) {
            return;
        }

        double lat = Double.longBitsToDouble(p.getLong(Prefs.KEY_LAT, Double.doubleToRawLongBits(0.0)));
        double lon = Double.longBitsToDouble(p.getLong(Prefs.KEY_LON, Double.doubleToRawLongBits(0.0)));
        float radius = p.getFloat(Prefs.KEY_RADIUS, 200f);
        boolean enter = p.getBoolean(Prefs.KEY_ENTER, true);
        boolean exit = p.getBoolean(Prefs.KEY_EXIT, true);

        int transitions = 0;
        if (enter) transitions |= Geofence.GEOFENCE_TRANSITION_ENTER;
        if (exit) transitions |= Geofence.GEOFENCE_TRANSITION_EXIT;
        if (transitions == 0) transitions = Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT;

        Geofence geofence = new Geofence.Builder()
                .setRequestId(GEOFENCE_ID)
                .setCircularRegion(lat, lon, radius)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(transitions)
                .setNotificationResponsiveness(60_000)
                .build();

        GeofencingRequest request = new GeofencingRequest.Builder()
                .setInitialTrigger(0)
                .addGeofence(geofence)
                .build();

        GeofencingClient client = LocationServices.getGeofencingClient(context);
        try {
            client.removeGeofences(pendingIntent(context)).addOnCompleteListener(task -> {
                try {
                    client.addGeofences(request, pendingIntent(context))
                            .addOnSuccessListener(v -> { if (onSuccess != null) onSuccess.run(); })
                            .addOnFailureListener(e -> { if (onError != null) onError.accept(e); });
                } catch (SecurityException se) {
                    if (onError != null) onError.accept(se);
                }
            });
        } catch (SecurityException se) {
            if (onError != null) onError.accept(se);
        }
    }

    public static void unregister(Context context) {
        LocationServices.getGeofencingClient(context).removeGeofences(pendingIntent(context));
    }
}
