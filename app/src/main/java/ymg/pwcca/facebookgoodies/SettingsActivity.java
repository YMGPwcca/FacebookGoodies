package ymg.pwcca.facebookgoodies;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SettingsActivity extends Activity {
    private SharedPreferences prefs;
    private LinearLayout content;
    private LinearLayout diagnostics;
    private TextView status;
    private TextView statusHint;
    private TextView reportTime;
    private View statusDot;
    private Button diagnosticsButton;
    private final Map<String, TextView> counters = new LinkedHashMap<>();
    private int textColor;
    private int secondaryColor;
    private int surfaceColor;
    private int accentColor;

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        prefs = getSharedPreferences(SettingsProvider.PREFS, MODE_PRIVATE);
        textColor = getColor(R.color.text_primary);
        secondaryColor = getColor(R.color.text_secondary);
        surfaceColor = getColor(R.color.surface);
        accentColor = getColor(R.color.accent);
        int pageColor = getColor(R.color.page_background);
        getWindow().setStatusBarColor(pageColor);
        getWindow().setNavigationBarColor(pageColor);
        boolean night = (getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        getWindow().getDecorView().setSystemUiVisibility(night ? 0
                : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(pageColor);
        scroll.setFillViewport(true);
        scroll.setPadding(dp(20), dp(12), dp(20), dp(20));
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            scroll.setPadding(dp(20) + insets.getSystemWindowInsetLeft(),
                    dp(12) + insets.getSystemWindowInsetTop(),
                    dp(20) + insets.getSystemWindowInsetRight(),
                    dp(20) + insets.getSystemWindowInsetBottom());
            return insets;
        });
        content = column();
        FrameLayout frame = new FrameLayout(this);
        frame.addView(content, new FrameLayout.LayoutParams(-1, -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL));
        scroll.addView(frame, new ScrollView.LayoutParams(-1, -2));
        scroll.addOnLayoutChangeListener((view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            int width = Math.min(dp(600), Math.max(0, right - left - scroll.getPaddingLeft() - scroll.getPaddingRight()));
            if (width > 0 && content.getLayoutParams().width != width) {
                content.getLayoutParams().width = width;
                content.requestLayout();
            }
        });
        setContentView(scroll);

        addHeader();
        addStatus();
        LinearLayout feed = addGroup(R.string.section_feed);
        addSetting(feed, "ads", R.string.block_sponsored_posts, R.string.block_sponsored_posts_description);
        addDivider(feed);
        addSetting(feed, "suggestions", R.string.hide_facebook_suggestions, R.string.hide_facebook_suggestions_description);
        LinearLayout privacy = addGroup(R.string.section_privacy);
        addSetting(privacy, "receipts", R.string.suppress_read_receipts, R.string.suppress_read_receipts_description);
        addDivider(privacy);
        addSetting(privacy, "capture", R.string.block_capture_detection, R.string.block_capture_detection_description);
        addDiagnostics();
        if (saved != null && saved.getBoolean("diagnostics_open")) setDiagnosticsOpen(true);
    }

    @Override protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putBoolean("diagnostics_open", diagnostics.getVisibility() == View.VISIBLE);
    }

    private void addHeader() {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        ImageView icon = new ImageView(this);
        icon.setImageResource(R.mipmap.ic_launcher);
        icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        row.addView(icon, size(36, 36));
        TextView title = label(getString(R.string.app_name), 22, textColor, true);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, -2, 1);
        titleParams.setMarginStart(dp(12));
        row.addView(title, titleParams);
        row.setPadding(0, dp(8), 0, dp(16));
        content.addView(row, size(-1, -2));
    }

    private void addStatus() {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        statusDot = new View(this);
        row.addView(statusDot, size(7, 7));
        status = label("", 13, secondaryColor, false);
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        LinearLayout.LayoutParams stateParams = new LinearLayout.LayoutParams(0, -2, 1);
        stateParams.setMarginStart(dp(8));
        row.addView(status, stateParams);
        content.addView(row, size(-1, -2));
        statusHint = label("", 13, secondaryColor, false);
        LinearLayout.LayoutParams hintParams = size(-1, -2);
        hintParams.topMargin = dp(5);
        content.addView(statusHint, hintParams);
    }

    private LinearLayout addGroup(int headingResource) {
        TextView heading = label(getString(headingResource), 14, accentColor, true);
        heading.setAccessibilityHeading(true);
        LinearLayout.LayoutParams headingParams = size(-1, -2);
        headingParams.topMargin = dp(24);
        headingParams.bottomMargin = dp(8);
        headingParams.setMarginStart(dp(2));
        content.addView(heading, headingParams);
        LinearLayout group = column();
        group.setBackground(rounded(surfaceColor, 12));
        content.addView(group, size(-1, -2));
        return group;
    }

    private void addSetting(LinearLayout group, String key, int titleResource, int descriptionResource) {
        String title = getString(titleResource);
        String description = getString(descriptionResource);
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(14), dp(12), dp(14));
        row.setMinimumHeight(dp(76));
        row.setBackground(new RippleDrawable(ColorStateList.valueOf((accentColor & 0x00ffffff) | 0x22000000),
                null, rounded(0xffffffff, 12)));
        row.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout copy = column();
        copy.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        copy.addView(label(title, 16, textColor, true));
        TextView descriptionView = label(description, 14, secondaryColor, false);
        LinearLayout.LayoutParams descriptionParams = size(-1, -2);
        descriptionParams.topMargin = dp(4);
        copy.addView(descriptionView, descriptionParams);
        row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));
        Switch toggle = new Switch(this);
        toggle.setShowText(false);
        toggle.setMinimumWidth(dp(48));
        toggle.setMinimumHeight(dp(48));
        toggle.setContentDescription(getString(R.string.setting_accessibility, title, description));
        int[][] states = { { android.R.attr.state_checked }, {} };
        toggle.setThumbTintList(new ColorStateList(states, new int[] { accentColor, getColor(R.color.control_off_thumb) }));
        toggle.setTrackTintList(new ColorStateList(states, new int[] { accentColor, getColor(R.color.control_off_track) }));
        toggle.setChecked(prefs.getBoolean(key, true));
        toggle.setOnCheckedChangeListener((button, enabled) -> prefs.edit().putBoolean(key, enabled).apply());
        LinearLayout.LayoutParams toggleParams = size(-2, 48);
        toggleParams.setMarginStart(dp(12));
        row.addView(toggle, toggleParams);
        row.setOnClickListener(view -> toggle.setChecked(!toggle.isChecked()));
        group.addView(row, size(-1, -2));
    }

    private void addDivider(LinearLayout group) {
        View divider = new View(this);
        divider.setBackgroundColor(getColor(R.color.outline));
        LinearLayout.LayoutParams params = size(-1, 1);
        params.setMarginStart(dp(16));
        params.setMarginEnd(dp(16));
        group.addView(divider, params);
    }

    private void addDiagnostics() {
        diagnosticsButton = textButton(R.string.show_diagnostics);
        diagnosticsButton.setOnClickListener(view -> {
            setDiagnosticsOpen(diagnostics.getVisibility() != View.VISIBLE);
            refreshStatus();
        });
        LinearLayout.LayoutParams disclosureParams = size(-1, 48);
        disclosureParams.topMargin = dp(14);
        content.addView(diagnosticsButton, disclosureParams);
        diagnostics = column();
        diagnostics.setPadding(dp(16), dp(8), dp(16), dp(12));
        diagnostics.setBackground(rounded(surfaceColor, 12));
        diagnostics.setVisibility(View.GONE);
        reportTime = label("", 13, secondaryColor, false);
        diagnostics.addView(reportTime, size(-1, -2));
        addCounter("hooks", R.string.diagnostic_hooks);
        addCounter("ads", R.string.diagnostic_ads);
        addCounter("suggestions", R.string.diagnostic_suggestions);
        addCounter("reads", R.string.diagnostic_reads);
        addCounter("captures", R.string.diagnostic_captures);
        addCounter("errors", R.string.diagnostic_errors);
        Button refresh = textButton(R.string.refresh);
        refresh.setOnClickListener(view -> refreshStatus());
        diagnostics.addView(refresh, size(-1, 48));
        content.addView(diagnostics, size(-1, -2));
    }

    private void setDiagnosticsOpen(boolean open) {
        diagnostics.setVisibility(open ? View.VISIBLE : View.GONE);
        diagnosticsButton.setText(open ? R.string.hide_diagnostics : R.string.show_diagnostics);
    }

    private void addCounter(String key, int labelResource) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(7), 0, dp(7));
        row.addView(label(getString(labelResource), 14, secondaryColor, false), new LinearLayout.LayoutParams(0, -2, 1));
        TextView value = label(getString(R.string.no_count), 14, textColor, true);
        row.addView(value);
        diagnostics.addView(row, size(-1, -2));
        counters.put(key, value);
    }

    private void refreshStatus() {
        Bundle data = getContentResolver().call(SettingsProvider.URI, "status", null, null);
        long updated = data == null ? 0 : data.getLong("updated");
        int stateColor = secondaryColor;
        if (updated == 0) {
            status.setText(R.string.waiting_for_facebook);
            statusHint.setText(R.string.enable_in_lsposed);
        } else if (!data.getBoolean("supported")) {
            status.setText(R.string.unsupported_build);
            statusHint.setText(R.string.unsupported_build_details);
            stateColor = getColor(R.color.status_error);
        } else if (data.getInt("hooks") == 0) {
            status.setText(R.string.no_hooks_status);
            statusHint.setText(R.string.no_hooks_details);
            stateColor = getColor(R.color.status_warning);
        } else if (System.currentTimeMillis() - updated > 15000) {
            status.setText(R.string.facebook_idle);
            statusHint.setText(R.string.facebook_idle_hint);
        } else {
            status.setText(R.string.module_active);
            statusHint.setText(R.string.settings_apply);
            stateColor = getColor(R.color.status_success);
        }
        statusDot.setBackground(rounded(stateColor, 4));
        reportTime.setText(updated == 0 ? getString(R.string.no_report_yet)
                : getString(R.string.last_report, DateFormat.getTimeFormat(this).format(updated)));
        NumberFormat format = NumberFormat.getIntegerInstance();
        for (Map.Entry<String, TextView> counter : counters.entrySet()) {
            counter.getValue().setText(updated == 0 ? getString(R.string.no_count)
                    : format.format(data.getInt(counter.getKey())));
        }
    }

    private Button textButton(int labelResource) {
        Button button = new Button(this);
        button.setText(labelResource);
        button.setAllCaps(false);
        button.setTextSize(14);
        button.setTextColor(accentColor);
        button.setMinHeight(dp(48));
        button.setMinimumHeight(dp(48));
        button.setElevation(0);
        button.setBackground(new RippleDrawable(ColorStateList.valueOf((accentColor & 0x00ffffff) | 0x22000000),
                null, rounded(0xffffffff, 8)));
        return button;
    }

    private TextView label(String text, int sizeSp, int color, boolean medium) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        view.setTypeface(Typeface.create(medium ? "sans-serif-medium" : "sans-serif", Typeface.NORMAL));
        view.setIncludeFontPadding(false);
        return view;
    }

    private LinearLayout column() {
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        return column;
    }

    private GradientDrawable rounded(int color, int radiusDp) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(radiusDp));
        return shape;
    }

    private LinearLayout.LayoutParams size(int widthDp, int heightDp) {
        return new LinearLayout.LayoutParams(widthDp < 0 ? widthDp : dp(widthDp), heightDp < 0 ? heightDp : dp(heightDp));
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
