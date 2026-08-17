package com.example.homeclockreminder;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;

public final class NotificationHelper {
    private static final String CHANNEL_ID = "attendance_reminders";

    private NotificationHelper() {}

    public static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "תזכורות נוכחות",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("תזכורת בעת כניסה או יציאה מאזור הבית");
            manager.createNotificationChannel(channel);
        }
    }

    public static void show(Context context, boolean entering) {
        if (Build.VERSION.SDK_INT >= 33 &&
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        ensureChannel(context);
        String title = entering ? "הגעת הביתה 🏠" : "יצאת מהבית 🚗";
        String text = entering
                ? "אל תשכח להחתים כניסה בשעון הנוכחות."
                : "אל תשכח להחתים יציאה בשעון הנוכחות.";

        String url = Prefs.get(context).getString(Prefs.KEY_URL, "").trim();
        Intent openIntent;
        if (!url.isEmpty()) {
            try {
                openIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            } catch (Exception ex) {
                openIntent = new Intent(context, MainActivity.class);
            }
        } else {
            openIntent = new Intent(context, MainActivity.class);
        }

        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                entering ? 2001 : 2002,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(context, CHANNEL_ID)
                : new Notification.Builder(context);

        builder.setSmallIcon(com.example.homeclockreminder.R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .setPriority(Notification.PRIORITY_HIGH)
                .setCategory(Notification.CATEGORY_REMINDER);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(entering ? 301 : 302, builder.build());
    }
}
