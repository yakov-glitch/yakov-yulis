package com.example.homeclockreminder;

import android.content.Context;
import android.content.SharedPreferences;

public final class Prefs {
    private static final String FILE = "home_clock_prefs";
    public static final String KEY_LAT = "lat";
    public static final String KEY_LON = "lon";
    public static final String KEY_RADIUS = "radius";
    public static final String KEY_ENTER = "enter";
    public static final String KEY_EXIT = "exit";
    public static final String KEY_ACTIVE = "active";
    public static final String KEY_URL = "clock_url";

    private Prefs() {}

    public static SharedPreferences get(Context context) {
        return context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }
}
