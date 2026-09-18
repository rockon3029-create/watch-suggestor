
package com.example.watchsuggester;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.SharedPreferences;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.*;

public class ContentScannerService extends AccessibilityService {

    private long lastCaptureTime = 0;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED | AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS | AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
        info.packageNames = new String[] {
            "com.google.android.youtube",
            "com.netflix.ninja",
            "com.netflix.mediaclient",
            "com.amazon.avod.thirdpartyclient",
            "in.startv.hotstar"
        };
        info.notificationTimeout = 250;
        setServiceInfo(info);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        long now = System.currentTimeMillis();
        // Throttle captures to once every 2 seconds to avoid excessive battery/disk writes
        if (now - lastCaptureTime < 2000) return;

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;

        List<String> visibleTexts = new ArrayList<>();
        extractText(root, visibleTexts);

        for (String text : visibleTexts) {
            String clean = text.trim();
            if (clean.length() > 3 && clean.length() < 60) {
                // Save discovered show / video title
                recordObservedContent(clean);
                lastCaptureTime = now;
                break;
            }
        }
    }

    private void extractText(AccessibilityNodeInfo node, List<String> list) {
        if (node == null || list.size() > 15) return;

        CharSequence cs = node.getText();
        if (cs != null && cs.length() > 0) {
            list.add(cs.toString());
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            extractText(node.getChild(i), list);
        }
    }

    private void recordObservedContent(String title) {
        SharedPreferences prefs = getSharedPreferences("WatchProfile", MODE_PRIVATE);
        String history = prefs.getString("titles_seen", "");
        
        // Don't log duplicates repeatedly
        if (!history.contains(title)) {
            String updated = title + "\n" + history;
            // Retain top 30 observed entries
            String[] lines = updated.split("\n");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Math.min(lines.length, 30); i++) {
                sb.append(lines[i]).append("\n");
            }
            prefs.edit().putString("titles_seen", sb.toString()).apply();
        }
    }

    @Override
    public void onInterrupt() {}
}
