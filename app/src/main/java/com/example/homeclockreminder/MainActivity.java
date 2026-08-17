package com.example.homeclockreminder;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

import java.util.Locale;

public class MainActivity extends Activity {
    private static final int REQ_LOCATION = 10;
    private static final int REQ_NOTIFICATION = 11;

    private EditText latInput;
    private EditText lonInput;
    private EditText urlInput;
    private SeekBar radiusSeek;
    private TextView radiusLabel;
    private CheckBox enterCheck;
    private CheckBox exitCheck;
    private TextView status;
    private FusedLocationProviderClient fusedLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("תזכורת נוכחות");
        NotificationHelper.ensureChannel(this);
        fusedLocation = LocationServices.getFusedLocationProviderClient(this);
        setContentView(buildUi());
        loadPrefs();
        requestNotificationPermissionIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePermissionStatus();
        if (Prefs.get(this).getBoolean(Prefs.KEY_ACTIVE, false) && GeofenceManager.hasRequiredLocationPermissions(this)) {
            GeofenceManager.registerSaved(this, this::showActive, e -> showStatus("נשמר, אך רישום האזור נכשל: " + e.getMessage(), true));
        }
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(32));
        root.setGravity(Gravity.RIGHT);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setBackgroundColor(Color.rgb(247, 248, 247));
        scroll.addView(root);

        TextView title = text("תזכורת נוכחות", 28, true);
        root.addView(title);
        TextView subtitle = text("קבל התראה אוטומטית כשאתה נכנס או יוצא מאזור הבית.", 16, false);
        subtitle.setTextColor(Color.rgb(82, 96, 88));
        root.addView(subtitle, marginTop(8));

        root.addView(section("מיקום הבית"), marginTop(28));
        Button locate = button("📍 השתמש במיקום הנוכחי");
        locate.setOnClickListener(v -> useCurrentLocation());
        root.addView(locate, marginTop(8));

        latInput = input("קו רוחב (Latitude)");
        lonInput = input("קו אורך (Longitude)");
        latInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
        lonInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
        root.addView(latInput, marginTop(10));
        root.addView(lonInput, marginTop(8));

        root.addView(section("רדיוס"), marginTop(24));
        radiusLabel = text("200 מטר", 18, true);
        root.addView(radiusLabel);
        radiusSeek = new SeekBar(this);
        radiusSeek.setMax(19); // 100m..2000m in 100m increments
        radiusSeek.setProgress(1);
        radiusSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { radiusLabel.setText(radiusFromProgress(progress) + " מטר"); }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        root.addView(radiusSeek);

        root.addView(section("התראות"), marginTop(20));
        enterCheck = new CheckBox(this);
        enterCheck.setText("התראה בכניסה הביתה");
        enterCheck.setTextSize(17);
        enterCheck.setChecked(true);
        root.addView(enterCheck);
        exitCheck = new CheckBox(this);
        exitCheck.setText("התראה ביציאה מהבית");
        exitCheck.setTextSize(17);
        exitCheck.setChecked(true);
        root.addView(exitCheck);

        root.addView(section("שעון נוכחות — אופציונלי"), marginTop(20));
        TextView urlHelp = text("אם תדביק קישור לאתר שעון הנוכחות, לחיצה על ההתראה תפתח אותו ישירות.", 14, false);
        urlHelp.setTextColor(Color.rgb(82, 96, 88));
        root.addView(urlHelp);
        urlInput = input("https://...");
        urlInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        root.addView(urlInput, marginTop(8));

        Button save = button("הפעל תזכורות");
        save.setOnClickListener(v -> saveAndRegister());
        root.addView(save, marginTop(24));

        Button permissions = button("⚙️ הרשאות מיקום ברקע");
        permissions.setOnClickListener(v -> openAppSettings());
        root.addView(permissions, marginTop(10));

        Button test = button("🔔 שלח התראת בדיקה");
        test.setOnClickListener(v -> NotificationHelper.show(this, false));
        root.addView(test, marginTop(10));

        Button disable = button("כבה תזכורות");
        disable.setOnClickListener(v -> disableReminders());
        root.addView(disable, marginTop(10));

        status = text("", 15, true);
        status.setPadding(dp(12), dp(16), dp(12), dp(16));
        root.addView(status, marginTop(18));

        TextView note = text("חשוב: כדי שהזיהוי יעבוד כשהאפליקציה סגורה, יש לבחור בהרשאת המיקום ‘אפשר כל הזמן’. מומלץ להשתמש במיקום מדויק וברדיוס של 150–300 מטר לפחות.", 13, false);
        note.setTextColor(Color.rgb(82, 96, 88));
        root.addView(note, marginTop(14));

        return scroll;
    }

    private void loadPrefs() {
        SharedPreferences p = Prefs.get(this);
        if (p.contains(Prefs.KEY_LAT)) {
            double lat = Double.longBitsToDouble(p.getLong(Prefs.KEY_LAT, Double.doubleToRawLongBits(0)));
            double lon = Double.longBitsToDouble(p.getLong(Prefs.KEY_LON, Double.doubleToRawLongBits(0)));
            latInput.setText(String.format(Locale.US, "%.7f", lat));
            lonInput.setText(String.format(Locale.US, "%.7f", lon));
        }
        int radius = Math.round(p.getFloat(Prefs.KEY_RADIUS, 200f));
        radiusSeek.setProgress(progressFromRadius(radius));
        radiusLabel.setText(radiusFromProgress(radiusSeek.getProgress()) + " מטר");
        enterCheck.setChecked(p.getBoolean(Prefs.KEY_ENTER, true));
        exitCheck.setChecked(p.getBoolean(Prefs.KEY_EXIT, true));
        urlInput.setText(p.getString(Prefs.KEY_URL, ""));
        updatePermissionStatus();
    }

    private void useCurrentLocation() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_LOCATION);
            return;
        }
        showStatus("מאתר את המיקום הנוכחי…", false);
        CancellationTokenSource cts = new CancellationTokenSource();
        fusedLocation.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.getToken())
                .addOnSuccessListener(location -> {
                    if (location == null) {
                        showStatus("לא התקבל מיקום. ודא ששירותי המיקום פעילים ונסה שוב.", true);
                        return;
                    }
                    setLocation(location);
                })
                .addOnFailureListener(e -> showStatus("לא הצלחתי לקבל מיקום: " + e.getMessage(), true));
    }

    private void setLocation(Location location) {
        latInput.setText(String.format(Locale.US, "%.7f", location.getLatitude()));
        lonInput.setText(String.format(Locale.US, "%.7f", location.getLongitude()));
        showStatus("המיקום הנוכחי הוזן. כעת לחץ ‘הפעל תזכורות’.", false);
    }

    private void saveAndRegister() {
        try {
            double lat = Double.parseDouble(latInput.getText().toString().trim());
            double lon = Double.parseDouble(lonInput.getText().toString().trim());
            if (lat < -90 || lat > 90 || lon < -180 || lon > 180) throw new IllegalArgumentException();
            if (!enterCheck.isChecked() && !exitCheck.isChecked()) {
                Toast.makeText(this, "בחר לפחות התראה אחת", Toast.LENGTH_LONG).show();
                return;
            }
            int radius = radiusFromProgress(radiusSeek.getProgress());
            Prefs.get(this).edit()
                    .putLong(Prefs.KEY_LAT, Double.doubleToRawLongBits(lat))
                    .putLong(Prefs.KEY_LON, Double.doubleToRawLongBits(lon))
                    .putFloat(Prefs.KEY_RADIUS, radius)
                    .putBoolean(Prefs.KEY_ENTER, enterCheck.isChecked())
                    .putBoolean(Prefs.KEY_EXIT, exitCheck.isChecked())
                    .putString(Prefs.KEY_URL, urlInput.getText().toString().trim())
                    .putBoolean(Prefs.KEY_ACTIVE, true)
                    .apply();

            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_LOCATION);
                return;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                    checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                showStatus("ההגדרה נשמרה. עכשיו צריך לאפשר מיקום ‘כל הזמן’ במסך ההרשאות.", true);
                openAppSettings();
                return;
            }
            registerNow();
        } catch (Exception e) {
            showStatus("יש להזין מיקום תקין או להשתמש בכפתור ‘המיקום הנוכחי’.", true);
        }
    }

    private void registerNow() {
        GeofenceManager.registerSaved(this, this::showActive, e -> showStatus("לא הצלחתי להפעיל את אזור הבית: " + e.getMessage(), true));
    }

    private void disableReminders() {
        Prefs.get(this).edit().putBoolean(Prefs.KEY_ACTIVE, false).apply();
        GeofenceManager.unregister(this);
        showStatus("התזכורות כבויות.", false);
    }

    private void showActive() {
        runOnUiThread(() -> showStatus("✓ התזכורות פעילות. אפשר לסגור את האפליקציה.", false));
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFICATION);
        }
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", getPackageName(), null));
        startActivity(intent);
        Toast.makeText(this, "פתח ‘הרשאות’ → ‘מיקום’ → בחר ‘אפשר כל הזמן’ ומיקום מדויק.", Toast.LENGTH_LONG).show();
    }

    private void updatePermissionStatus() {
        if (status == null) return;
        boolean fine = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean bg = Build.VERSION.SDK_INT < 29 || checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean notif = Build.VERSION.SDK_INT < 33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        boolean active = Prefs.get(this).getBoolean(Prefs.KEY_ACTIVE, false);
        String s = "מיקום מדויק: " + (fine ? "✓" : "✗") + "   מיקום ברקע: " + (bg ? "✓" : "✗") + "   התראות: " + (notif ? "✓" : "✗");
        if (active && fine && bg) s += "\nהתזכורות מוגדרות כפעילות.";
        status.setText(s);
        status.setTextColor((fine && bg && notif) ? Color.rgb(20, 83, 45) : Color.rgb(145, 64, 14));
    }

    private void showStatus(String message, boolean warning) {
        status.setText(message);
        status.setTextColor(warning ? Color.rgb(145, 64, 14) : Color.rgb(20, 83, 45));
    }

    private TextView text(String value, int sp, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(value);
        tv.setTextSize(sp);
        tv.setTextColor(Color.rgb(23, 33, 26));
        tv.setGravity(Gravity.RIGHT);
        if (bold) tv.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);
        return tv;
    }

    private TextView section(String value) { return text(value, 19, true); }

    private EditText input(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextSize(16);
        e.setGravity(Gravity.RIGHT);
        e.setSingleLine(true);
        e.setPadding(dp(12), dp(12), dp(12), dp(12));
        return e;
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(16);
        b.setAllCaps(false);
        return b;
    }

    private LinearLayout.LayoutParams marginTop(int top) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(top);
        return lp;
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private int radiusFromProgress(int progress) { return (progress + 1) * 100; }
    private int progressFromRadius(int radius) { return Math.max(0, Math.min(19, Math.round(radius / 100f) - 1)); }
}
