package com.example.watchsuggester;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.content.SharedPreferences;
import android.os.Bundle;

public class MediaNotificationListener extends NotificationListenerService {
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String pkg = sbn.getPackageName();
        if (pkg.contains("youtube") || pkg.contains("netflix") || 
            pkg.contains("amazon.avod") || pkg.contains("hotstar")) {

            Bundle extras = sbn.getNotification().extras;
            if (extras != null) {
                String title = extras.getString("android.title", "");
                String text = extras.getString("android.text", "");
                if (!title.isEmpty()) {
                    SharedPreferences prefs = getSharedPreferences("MediaHistory", MODE_PRIVATE);
                    String current = prefs.getString("recent_items", "");
                    prefs.edit().putString("recent_items", title + " - " + text + "\n" + current).apply();
                }
            }
        }
    }
}
