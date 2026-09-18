package com.example.watchsuggester;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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

    private final List<MediaItem> catalog = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        seedCatalog();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 50, 40, 40);

        Button btnAccess = new Button(this);
        btnAccess.setText("1. Enable Accessibility Reader");
        btnAccess.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        root.addView(btnAccess);

        TextView tvInstructions = new TextView(this);
        tvInstructions.setText("Turn on 'WatchSuggester Screen Reader' inside Accessibility settings.\n");
        root.addView(tvInstructions);

        Button btnAnalyze = new Button(this);
        btnAnalyze.setText("2. Analyze Watched Content & Suggest Next");
        root.addView(btnAnalyze);

        TextView tvProfile = new TextView(this);
        tvProfile.setTextSize(14);
        tvProfile.setPadding(0, 20, 0, 20);
        root.addView(tvProfile);

        TextView tvSuggestion = new TextView(this);
        tvSuggestion.setTextSize(18);
        root.addView(tvSuggestion);

        btnAnalyze.setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences("WatchProfile", MODE_PRIVATE);
            String rawHistory = prefs.getString("titles_seen", "");

            if (rawHistory.trim().isEmpty()) {
                tvProfile.setText("No watched content detected yet.\nOpen YouTube, Netflix, Hotstar, or Prime Video and watch something first!");
                tvSuggestion.setText("");
                return;
            }

            // Keyword inference engine
            Map<String, Integer> genreWeights = computeGenreInterests(rawHistory);
            String topGenre = "Action";
            int maxScore = -1;
            for (Map.Entry<String, Integer> e : genreWeights.entrySet()) {
                if (e.getValue() > maxScore) {
                    maxScore = e.getValue();
                    topGenre = e.getKey();
                }
            }

            tvProfile.setText("Detected Recent Titles:\n" + rawHistory + "\nInferred Preferred Genre: " + topGenre);

            // Filter catalog matching inferred taste
            List<MediaItem> matches = new ArrayList<>();
            for (MediaItem item : catalog) {
                if (item.genre.equalsIgnoreCase(topGenre)) {
                    matches.add(item);
                }
            }

            if (!matches.isEmpty()) {
                MediaItem pick = matches.get(new Random().nextInt(matches.size()));
                tvSuggestion.setText("Recommended Next:\n" + pick.title + "\nAvailable on: " + pick.platform + " [" + pick.genre + "]");
            } else {
                MediaItem fallback = catalog.get(new Random().nextInt(catalog.size()));
                tvSuggestion.setText("Recommended Next:\n" + fallback.title + "\nAvailable on: " + fallback.platform + " [" + fallback.genre + "]");
            }
        });

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);
    }

    private Map<String, Integer> computeGenreInterests(String history) {
        String lower = history.toLowerCase();
        Map<String, Integer> scores = new HashMap<>();
        scores.put("Crime", 0);
        scores.put("Sci-Fi", 0);
        scores.put("Action", 0);
        scores.put("Documentary", 0);
        scores.put("Comedy", 0);

        if (lower.contains("police") || lower.contains("gang") || lower.contains("crime") || lower.contains("murder") || lower.contains("mirzapur") || lower.contains("bad")) {
            scores.put("Crime", scores.get("Crime") + 3);
        }
        if (lower.contains("space") || lower.contains("tech") || lower.contains("future") || lower.contains("alien") || lower.contains("stranger")) {
            scores.put("Sci-Fi", scores.get("Sci-Fi") + 3);
        }
        if (lower.contains("fight") || lower.contains("super") || lower.contains("war") || lower.contains("boys") || lower.contains("action")) {
            scores.put("Action", scores.get("Action") + 3);
        }
        if (lower.contains("how") || lower.contains("why") || lower.contains("science") || lower.contains("history") || lower.contains("veritasium") || lower.contains("kurzgesagt")) {
            scores.put("Documentary", scores.get("Documentary") + 3);
        }

        return scores;
    }

    private void seedCatalog() {
        catalog.add(new MediaItem("Stranger Things", "Netflix", "Sci-Fi"));
        catalog.add(new MediaItem("Dark", "Netflix", "Sci-Fi"));
        catalog.add(new MediaItem("Breaking Bad", "Netflix", "Crime"));
        catalog.add(new MediaItem("Narcos", "Netflix", "Crime"));
        catalog.add(new MediaItem("The Boys", "Prime Video", "Action"));
        catalog.add(new MediaItem("Reacher", "Prime Video", "Action"));
        catalog.add(new MediaItem("Mirzapur", "Prime Video", "Crime"));
        catalog.add(new MediaItem("Panchayat", "Prime Video", "Comedy"));
        catalog.add(new MediaItem("Special OPS", "Hotstar", "Crime"));
        catalog.add(new MediaItem("Loki", "Hotstar", "Sci-Fi"));
        catalog.add(new MediaItem("Veritasium", "YouTube", "Documentary"));
        catalog.add(new MediaItem("Kurzgesagt - In a Nutshell", "YouTube", "Documentary"));
    }
}
