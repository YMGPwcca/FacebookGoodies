package ymg.pwcca.facebookgoodies;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

public final class SettingsActivity extends Activity {
    private SharedPreferences prefs;
    private LinearLayout content;
    private TextView status;
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        prefs = getSharedPreferences(SettingsProvider.PREFS, 0);
        content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        content.setPadding(pad, pad, pad, pad);
        ScrollView scroll = new ScrollView(this); scroll.addView(content); setContentView(scroll);
        text("FacebookGoodies", 26);
        text("Sponsored-feed filtering and privacy controls for Facebook. Enable FacebookGoodies for Facebook in LSPosed, then restart Facebook.", 16);
        toggle("ads", "Block sponsored feed posts", "Filters sponsored units in the feed's list, cache and rendering paths. Other ad surfaces are not yet verified.");
        toggle("suggestions", "Hide Facebook suggestions", "Hides suggested posts and people recommendations. Keeps ordinary posts and friends' shares.");
        toggle("receipts", "Suppress in-app Messenger read receipts", "Experimental: blocks the thread-mark-read transport call. Chats may remain unread. Separate Messenger is outside this module's scope.");
        toggle("capture", "Block screen-capture detection", "Suppresses Android capture callbacks and confirmed Facebook capture-observer paths. Does not remove DRM or unlock protected screenshots.");
        status = text("", 15);
        Button refresh = new Button(this); refresh.setText("Refresh status"); refresh.setOnClickListener(v -> refreshStatus()); content.addView(refresh);
        text("Status contains hook counts and interception counts only. No messages, account data, cookies or URLs are collected. Switches refresh while Facebook runs.", 14);
        refreshStatus();
    }
    private TextView text(String value, int size) {
        TextView view = new TextView(this); view.setText(value); view.setTextSize(size); view.setPadding(0, 12, 0, 12); content.addView(view); return view;
    }
    private void toggle(String key, String title, String description) {
        Switch view = new Switch(this); view.setText(title); view.setTextSize(18); view.setMinHeight(64);
        view.setChecked(prefs.getBoolean(key, true));
        view.setOnCheckedChangeListener((button, enabled) -> prefs.edit().putBoolean(key, enabled).apply());
        content.addView(view); text(description, 14);
    }
    private void refreshStatus() {
        Bundle data = getContentResolver().call(SettingsProvider.URI, "status", null, null);
        if (data == null || data.getLong("updated") == 0) { status.setText("Waiting for Facebook. Enable the module in LSPosed and restart Facebook."); return; }
        status.setText("Supported build: " + data.getBoolean("supported") + "\nHooks installed: " + data.getInt("hooks") + "\nAd filter actions: " + data.getInt("ads") + "\nSuggestion filter actions: " + data.getInt("suggestions") + "\nCache checks / removals: " + data.getInt("cacheCalls") + " / " + data.getInt("cacheRemoved") + "\nRender checks / removals: " + data.getInt("renderCalls") + " / " + data.getInt("renderRemoved") + "\nRead calls suppressed: " + data.getInt("reads") + "\nCapture callbacks suppressed: " + data.getInt("captures") + "\nHook errors: " + data.getInt("errors") + "\nFacebook process: " + data.getInt("pid") + "\nLast report: " + new java.util.Date(data.getLong("updated")));
    }
}
