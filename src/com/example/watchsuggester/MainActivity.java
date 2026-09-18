package com.example.watchsuggester;

import android.app.Activity;
import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Process;
import android.provider.Settings;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {

    static class MediaItem {
        String title, platform, genre;
        MediaItem(String t, String p, String g) {
            title = t; platform = p; genre = g;
        }
    }

    private List<MediaItem> catalog = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        seedCatalog();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 60, 40, 40);

        Button btnPerm = new Button(this);
        btnPerm.setText("1. Grant Usage Access (Screen Time)");
        btnPerm.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)));
        root.addView(btnPerm);

        Button btnRefresh = new Button(this);
        btnRefresh.setText("2. Check Screen Time & Suggest");
        root.addView(btnRefresh);

        TextView tvUsage = new TextView(this);
        tvUsage.setTextSize(14);
        tvUsage.setPadding(0, 30, 0, 20);
        root.addView(tvUsage);

        TextView tvResult = new TextView(this);
        tvResult.setTextSize(18);
        root.addView(tvResult);

        btnRefresh.setOnClickListener(v -> {
            if (!hasUsageStatsPermission()) {
                tvUsage.setText("Usage access permission is NOT granted.\nTap the top button first.");
                return;
            }

            Map<String, Long> timeMap = getAppScreenTime();
            StringBuilder sb = new StringBuilder("Today's App Usage:\n");
            String topApp = "None";
            long maxTime = 0;

            for (Map.Entry<String, Long> entry : timeMap.entrySet()) {
                long minutes = entry.getValue() / (1000 * 60);
                sb.append("• ").append(entry.getKey()).append(": ").append(minutes).append(" mins\n");
                if (entry.getValue() > maxTime) {
                    maxTime = entry.getValue();
                    topApp = entry.getKey();
                }
            }
            tvUsage.setText(sb.toString());

            // Suggest based on the app used the most today
            List<MediaItem> matches = new ArrayList<>();
            for (MediaItem item : catalog) {
                if (item.platform.equalsIgnoreCase(topApp)) {
                    matches.add(item);
                }
            }

            if (!matches.isEmpty()) {
                MediaItem pick = matches.get(new Random().nextInt(matches.size()));
                tvResult.setText("Based on your most-used app (" + topApp + "):\nRecommended: " + pick.title + " [" + pick.genre + "]");
            } else {
                MediaItem fallback = catalog.get(new Random().nextInt(catalog.size()));
                tvResult.setText("General Recommendation:\n" + fallback.title + " [" + fallback.platform + " | " + fallback.genre + "]");
            }
        });

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);
    }

    private boolean hasUsageStatsPermission() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private Map<String, Long> getAppScreenTime() {
        Map<String, Long> results = new HashMap<>();
        results.put("YouTube", 0L);
        results.put("Netflix", 0L);
        results.put("Prime Video", 0L);
        results.put("Hotstar", 0L);

        UsageStatsManager usm = (UsageStatsManager) getSystemService(Context.USAGE_STATS_SERVICE);
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        long startTime = calendar.getTimeInMillis();
        long endTime = System.currentTimeMillis();

        List<UsageStats> stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime);
        if (stats != null) {
            for (UsageStats u : stats) {
                String pkg = u.getPackageName().toLowerCase();
                long total = u.getTotalTimeInForeground();
                if (pkg.contains("youtube")) {
                    results.put("YouTube", results.get("YouTube") + total);
                } else if (pkg.contains("netflix")) {
                    results.put("Netflix", results.get("Netflix") + total);
                } else if (pkg.contains("amazon.avod")) {
                    results.put("Prime Video", results.get("Prime Video") + total);
                } else if (pkg.contains("hotstar")) {
                    results.put("Hotstar", results.get("Hotstar") + total);
                }
            }
        }
        return results;
    }

    private void seedCatalog() {
        catalog.add(new MediaItem("Stranger Things", "Netflix", "Sci-Fi"));
        catalog.add(new MediaItem("Breaking Bad", "Netflix", "Crime"));
        catalog.add(new MediaItem("The Boys", "Prime Video", "Action"));
        catalog.add(new MediaItem("Mirzapur", "Prime Video", "Drama"));
        catalog.add(new MediaItem("Special OPS", "Hotstar", "Thriller"));
        catalog.add(new MediaItem("Loki", "Hotstar", "Superhero"));
        catalog.add(new MediaItem("Veritasium", "YouTube", "Science"));
        catalog.add(new MediaItem("Kurzgesagt", "YouTube", "Documentary"));
    }
}
