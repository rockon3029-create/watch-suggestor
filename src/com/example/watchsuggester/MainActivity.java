package com.example.watchsuggester;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.util.JsonReader;
import android.widget.*;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MainActivity extends Activity {
    static class MediaItem {
        String title, platform, genre;
        MediaItem(String t, String p, String g) { title = t; platform = p; genre = g; }
    }

    private List<MediaItem> catalog = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadCatalogFromAssets("catalog.json");

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 40, 40, 40);

        Button btnPerm = new Button(this);
        btnPerm.setText("1. Enable Notification Access");
        btnPerm.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        root.addView(btnPerm);

        TextView lbl = new TextView(this);
        lbl.setText("\nFilter by Platform (" + catalog.size() + " loaded):");
        root.addView(lbl);

        Spinner platformSpinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"All", "Netflix", "Prime Video", "Hotstar", "YouTube"});
        platformSpinner.setAdapter(adapter);
        root.addView(platformSpinner);

        Button btnSuggest = new Button(this);
        btnSuggest.setText("Suggest What to Watch Next");
        root.addView(btnSuggest);

        TextView tvResult = new TextView(this);
        tvResult.setTextSize(18);
        tvResult.setPadding(0, 30, 0, 20);
        root.addView(tvResult);

        TextView tvHistory = new TextView(this);
        tvHistory.setTextSize(14);
        root.addView(tvHistory);

        btnSuggest.setOnClickListener(v -> {
            String selectedPlatform = platformSpinner.getSelectedItem().toString();
            List<MediaItem> pool = new ArrayList<>();
            for (MediaItem item : catalog) {
                if (selectedPlatform.equals("All") || item.platform.equalsIgnoreCase(selectedPlatform)) {
                    pool.add(item);
                }
            }
            if (!pool.isEmpty()) {
                MediaItem pick = pool.get(new Random().nextInt(pool.size()));
                tvResult.setText("Recommended: " + pick.title + "\n[" + pick.platform + " | " + pick.genre + "]");
            }

            SharedPreferences prefs = getSharedPreferences("MediaHistory", MODE_PRIVATE);
            String history = prefs.getString("recent_items", "No recent activity detected yet.");
            tvHistory.setText("Recent Detected Activity:\n" + history);
        });

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);
    }

    private void loadCatalogFromAssets(String fileName) {
        try (InputStream is = getAssets().open(fileName);
             InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
             JsonReader reader = new JsonReader(isr)) {
            reader.beginArray();
            while (reader.hasNext()) {
                reader.beginObject();
                String title = "", platform = "", genre = "";
                while (reader.hasNext()) {
                    String key = reader.nextName();
                    if (key.equals("title")) title = reader.nextString();
                    else if (key.equals("platform")) platform = reader.nextString();
                    else if (key.equals("genre")) genre = reader.nextString();
                    else reader.skipValue();
                }
                reader.endObject();
                if (!title.isEmpty()) catalog.add(new MediaItem(title, platform, genre));
            }
            reader.endArray();
        } catch (Exception e) {}
    }
}
