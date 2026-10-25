package ymg.pwcca.facebookgoodies;

import android.app.Activity;
import android.animation.ValueAnimator;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
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
    private static final int FACEBOOK = 0;
    private static final int MESSENGER = 1;
    private final Page[] pages = new Page[2];
    private final View[] tabViews = new View[2];
    private final TextView[] tabLabels = new TextView[2];
    private final View[] tabIndicators = new View[2];
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Runnable refresh = new Runnable() {
        @Override public void run() {
            for (Page page : pages) refreshStatus(page);
            main.postDelayed(this, 5000);
        }
    };
    private SharedPreferences prefs;
    private SharedPreferences uiPrefs;
    private int selectedTab;
    private boolean tabsReady;
    private int transitionGeneration;
    private int pageColor;
    private int textColor;
    private int secondaryColor;
    private int surfaceColor;
    private int facebookColor;
    private int messengerColor;

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        prefs = getSharedPreferences(SettingsProvider.PREFS, MODE_PRIVATE);
        uiPrefs = getSharedPreferences("ui", MODE_PRIVATE);
        pageColor = getColor(R.color.page_background);
        textColor = getColor(R.color.text_primary);
        secondaryColor = getColor(R.color.text_secondary);
        surfaceColor = getColor(R.color.surface);
        facebookColor = getColor(R.color.facebook_accent);
        messengerColor = getColor(R.color.messenger_accent);
        getWindow().setStatusBarColor(pageColor);
        getWindow().setNavigationBarColor(pageColor);
        boolean night = (getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        getWindow().getDecorView().setSystemUiVisibility(night ? 0
                : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);

        FrameLayout outer = new FrameLayout(this);
        outer.setBackgroundColor(pageColor);
        outer.setOnApplyWindowInsetsListener((view, insets) -> {
            outer.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });
        LinearLayout shell = column();
        outer.addView(shell, new FrameLayout.LayoutParams(-1, -1, Gravity.TOP | Gravity.CENTER_HORIZONTAL));
        outer.addOnLayoutChangeListener((view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            int width = Math.min(dp(640), right - left - outer.getPaddingLeft() - outer.getPaddingRight());
            if (width > 0 && shell.getLayoutParams().width != width) {
                shell.getLayoutParams().width = width;
                shell.requestLayout();
            }
        });
        setContentView(outer);
        addAppBar(shell);
        addTabs(shell);
        FrameLayout body = new FrameLayout(this);
        shell.addView(body, new LinearLayout.LayoutParams(-1, 0, 1));
        pages[FACEBOOK] = buildPage(FACEBOOK);
        pages[MESSENGER] = buildPage(MESSENGER);
        body.addView(pages[FACEBOOK].scroll, new FrameLayout.LayoutParams(-1, -1));
        body.addView(pages[MESSENGER].scroll, new FrameLayout.LayoutParams(-1, -1));
        if (saved != null) {
            setDiagnosticsOpen(pages[FACEBOOK], saved.getBoolean("facebook_diagnostics"));
            setDiagnosticsOpen(pages[MESSENGER], saved.getBoolean("messenger_diagnostics"));
            pages[FACEBOOK].scroll.post(() -> pages[FACEBOOK].scroll.scrollTo(0, saved.getInt("facebook_scroll")));
            pages[MESSENGER].scroll.post(() -> pages[MESSENGER].scroll.scrollTo(0, saved.getInt("messenger_scroll")));
        }
        showTab(saved == null ? uiPrefs.getInt("selected_tab", FACEBOOK)
                : saved.getInt("selected_tab", FACEBOOK));
    }

    @Override protected void onResume() {
        super.onResume();
        main.removeCallbacks(refresh);
        refresh.run();
    }

    @Override protected void onPause() {
        main.removeCallbacks(refresh);
        if (tabsReady) settleTabs();
        super.onPause();
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt("selected_tab", selectedTab);
        out.putBoolean("facebook_diagnostics", pages[FACEBOOK].diagnostics.getVisibility() == View.VISIBLE);
        out.putBoolean("messenger_diagnostics", pages[MESSENGER].diagnostics.getVisibility() == View.VISIBLE);
        out.putInt("facebook_scroll", pages[FACEBOOK].scroll.getScrollY());
        out.putInt("messenger_scroll", pages[MESSENGER].scroll.getScrollY());
    }

    private void addAppBar(LinearLayout shell) {
        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(20), dp(12), dp(20), dp(12));
        ImageView icon = new ImageView(this);
        icon.setImageResource(R.mipmap.ic_launcher);
        icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        bar.addView(icon, size(38, 38));
        LinearLayout copy = column();
        LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(0, -2, 1);
        copyParams.setMarginStart(dp(12));
        bar.addView(copy, copyParams);
        copy.addView(label(getString(R.string.app_name), 19, textColor, true));
        copy.addView(label(getString(R.string.version_label, moduleVersion()), 12, secondaryColor, false));
        shell.addView(bar, size(-1, -2));
    }

    private void addTabs(LinearLayout shell) {
        LinearLayout tabs = new LinearLayout(this);
        tabs.setPadding(dp(12), 0, dp(12), 0);
        for (int index = 0; index < 2; index++) {
            final int tab = index;
            FrameLayout item = new FrameLayout(this);
            item.setMinimumHeight(dp(56));
            item.setClickable(true);
            item.setFocusable(true);
            item.setBackground(new RippleDrawable(ColorStateList.valueOf(0x22000000),
                    null, new ColorDrawable(0xffffffff)));
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER);
            Drawable icon = hostIcon(index);
            if (icon != null) {
                ImageView image = new ImageView(this);
                image.setImageDrawable(icon);
                image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                row.addView(image, size(21, 21));
            }
            TextView title = label(getString(index == FACEBOOK ? R.string.tab_facebook : R.string.tab_messenger),
                    15, secondaryColor, true);
            LinearLayout.LayoutParams titleParams = size(-2, -2);
            if (icon != null) titleParams.setMarginStart(dp(8));
            row.addView(title, titleParams);
            tabLabels[index] = title;
            item.addView(row, new FrameLayout.LayoutParams(-1, -1));
            View indicator = new View(this);
            FrameLayout.LayoutParams indicatorParams = new FrameLayout.LayoutParams(-1, dp(3), Gravity.BOTTOM);
            indicatorParams.setMarginStart(dp(12));
            indicatorParams.setMarginEnd(dp(12));
            item.addView(indicator, indicatorParams);
            tabIndicators[index] = indicator;
            item.setContentDescription(getString(index == FACEBOOK ? R.string.tab_facebook : R.string.tab_messenger));
            item.setOnClickListener(view -> showTab(tab));
            tabViews[index] = item;
            tabs.addView(item, new LinearLayout.LayoutParams(0, dp(56), 1));
        }
        shell.addView(tabs, size(-1, 56));
        View line = new View(this);
        line.setBackgroundColor(getColor(R.color.outline));
        shell.addView(line, size(-1, 1));
    }

    private Page buildPage(int index) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setPadding(dp(20), dp(20), dp(20), dp(28));
        LinearLayout content = column();
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        Page page = new Page(index, index == FACEBOOK ? facebookColor : messengerColor, scroll, content);
        addSupportRow(page);
        if (index == FACEBOOK) {
            LinearLayout feed = addGroup(page, R.string.section_feed);
            addSetting(page, feed, "ads", R.string.block_sponsored_posts, R.string.block_sponsored_posts_description, true);
            addDivider(feed);
            addSetting(page, feed, "suggestions", R.string.hide_facebook_suggestions, R.string.hide_facebook_suggestions_description, true);
            LinearLayout feedContent = addGroup(page, R.string.section_content);
            addSetting(page, feedContent, "hide_threads", R.string.hide_threads, R.string.hide_threads_description, false);
            addDivider(feedContent);
            addSetting(page, feedContent, "hide_stories_top", R.string.hide_stories_top, R.string.hide_stories_top_description, false);
            addDivider(feedContent);
            addSetting(page, feedContent, "hide_stories_middle", R.string.hide_stories_middle, R.string.hide_stories_middle_description, false);
            addDivider(feedContent);
            addSetting(page, feedContent, "hide_reels", R.string.hide_reels, R.string.hide_reels_description, false);
            LinearLayout privacy = addGroup(page, R.string.section_privacy);
            addSetting(page, privacy, "receipts", R.string.suppress_read_receipts, R.string.suppress_read_receipts_description, true);
            addDivider(privacy);
            addSetting(page, privacy, "capture", R.string.block_capture_detection, R.string.block_capture_detection_description, true);
            LinearLayout downloads = addGroup(page, R.string.section_downloads);
            addSetting(page, downloads, "downloads", R.string.download_current_video, R.string.download_current_video_description, true);
        } else {
            LinearLayout chats = addGroup(page, R.string.section_chats);
            addSetting(page, chats, "messenger_seen", R.string.messenger_seen, R.string.messenger_seen_description, true);
            addDivider(chats);
            addSetting(page, chats, "messenger_typing", R.string.messenger_typing, R.string.messenger_typing_description, true);
            LinearLayout stories = addGroup(page, R.string.section_stories);
            addSetting(page, stories, "messenger_story_seen", R.string.messenger_story_seen, R.string.messenger_story_seen_description, true);
        }
        addDiagnostics(page);
        return page;
    }

    private void addSupportRow(Page page) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(64));
        row.setPadding(dp(16), dp(10), dp(16), dp(10));
        row.setBackground(rounded(surfaceColor, 14));
        page.content.addView(row, size(-1, -2));
        page.supportHeader = row;
        page.version = label("", 14, textColor, true);
        row.addView(page.version, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout support = new LinearLayout(this);
        support.setGravity(Gravity.CENTER_VERTICAL);
        page.supportDot = new View(this);
        support.addView(page.supportDot, size(8, 8));
        page.support = label("", 13, secondaryColor, true);
        LinearLayout.LayoutParams supportParams = size(-2, -2);
        supportParams.setMarginStart(dp(7));
        support.addView(page.support, supportParams);
        LinearLayout.LayoutParams groupParams = size(-2, -2);
        groupParams.setMarginStart(dp(12));
        row.addView(support, groupParams);
    }

    private LinearLayout addGroup(Page page, int headingResource) {
        TextView heading = label(getString(headingResource), 14, page.accent, true);
        heading.setAccessibilityHeading(true);
        LinearLayout.LayoutParams headingParams = size(-1, -2);
        headingParams.topMargin = dp(25);
        headingParams.bottomMargin = dp(9);
        page.content.addView(heading, headingParams);
        LinearLayout group = column();
        group.setBackground(rounded(surfaceColor, 16));
        group.setClipToOutline(true);
        page.content.addView(group, size(-1, -2));
        return group;
    }

    private void addSetting(Page page, LinearLayout group, String key, int titleResource,
            int descriptionResource, boolean defaultValue) {
        String title = getString(titleResource);
        String description = getString(descriptionResource);
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(14), dp(12), dp(14));
        row.setMinimumHeight(dp(76));
        row.setBackground(new RippleDrawable(ColorStateList.valueOf((page.accent & 0x00ffffff) | 0x22000000),
                null, new ColorDrawable(0xffffffff)));
        row.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout copy = column();
        copy.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        copy.addView(label(title, 16, textColor, true));
        TextView detail = label(description, 13, secondaryColor, false);
        LinearLayout.LayoutParams detailParams = size(-1, -2);
        detailParams.topMargin = dp(4);
        copy.addView(detail, detailParams);
        row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));
        Switch toggle = new Switch(this);
        toggle.setShowText(false);
        toggle.setMinimumWidth(dp(48));
        toggle.setMinimumHeight(dp(48));
        toggle.setContentDescription(getString(R.string.setting_accessibility, title, description));
        int[][] states = {{android.R.attr.state_checked}, {}};
        toggle.setThumbTintList(new ColorStateList(states,
                new int[]{page.accent, getColor(R.color.control_off_thumb)}));
        toggle.setTrackTintList(new ColorStateList(states,
                new int[]{page.accent, getColor(R.color.control_off_track)}));
        toggle.setChecked(prefs.getBoolean(key, defaultValue));
        toggle.setOnCheckedChangeListener((button, enabled) -> prefs.edit().putBoolean(key, enabled).apply());
        LinearLayout.LayoutParams toggleParams = size(-2, 48);
        toggleParams.setMarginStart(dp(12));
        row.addView(toggle, toggleParams);
        row.setOnClickListener(view -> toggle.setChecked(!toggle.isChecked()));
        group.addView(row, size(-1, -2));
    }

    private void addDivider(LinearLayout group) {
        View line = new View(this);
        line.setBackgroundColor(getColor(R.color.outline));
        LinearLayout.LayoutParams params = size(-1, 1);
        params.setMarginStart(dp(16));
        params.setMarginEnd(dp(16));
        group.addView(line, params);
    }

    private void addDiagnostics(Page page) {
        LinearLayout card = column();
        card.setBackground(rounded(surfaceColor, 16));
        card.setClipToOutline(true);
        LinearLayout.LayoutParams cardParams = size(-1, -2);
        cardParams.topMargin = dp(25);
        page.content.addView(card, cardParams);
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(16), 0, dp(16), 0);
        header.setMinimumHeight(dp(56));
        header.setClickable(true);
        header.setFocusable(true);
        header.setBackground(new RippleDrawable(ColorStateList.valueOf((page.accent & 0x00ffffff) | 0x22000000),
                null, new ColorDrawable(0xffffffff)));
        header.addView(label(getString(R.string.diagnostics_title), 15, textColor, true),
                new LinearLayout.LayoutParams(0, -2, 1));
        page.diagnosticsAction = label("", 13, page.accent, true);
        header.addView(page.diagnosticsAction);
        header.setOnClickListener(view -> {
            setDiagnosticsOpen(page, page.diagnostics.getVisibility() != View.VISIBLE);
            refreshStatus(page);
        });
        card.addView(header, size(-1, 56));
        page.diagnosticsHeader = header;
        page.diagnostics = column();
        page.diagnostics.setPadding(dp(16), dp(4), dp(16), dp(14));
        page.reportTime = label("", 12, secondaryColor, false);
        LinearLayout.LayoutParams reportParams = size(-1, -2);
        reportParams.bottomMargin = dp(8);
        page.diagnostics.addView(page.reportTime, reportParams);
        if (page.index == FACEBOOK) {
            addCounter(page, "hooks", R.string.diagnostic_hooks);
            addCounter(page, "ads", R.string.diagnostic_ads);
            addCounter(page, "videoHooks", R.string.diagnostic_video_hooks);
            addCounter(page, "reelsAds", R.string.diagnostic_reels_ads);
            addCounter(page, "videoAds", R.string.diagnostic_video_ads);
            addCounter(page, "contentHooks", R.string.diagnostic_content_hooks);
            addCounter(page, "threads", R.string.diagnostic_threads);
            addCounter(page, "storiesTop", R.string.diagnostic_stories_top);
            addCounter(page, "storiesMiddle", R.string.diagnostic_stories_middle);
            addCounter(page, "reels", R.string.diagnostic_reels_content);
            addCounter(page, "suggestions", R.string.diagnostic_suggestions);
            addCounter(page, "reads", R.string.diagnostic_reads);
            addCounter(page, "captures", R.string.diagnostic_captures);
            addCounter(page, "downloadHooks", R.string.diagnostic_download_hooks);
            addCounter(page, "downloads", R.string.diagnostic_downloads);
            addCounter(page, "downloadFailures", R.string.diagnostic_download_failures);
        } else {
            addCounter(page, "hooks", R.string.diagnostic_hooks);
            addCounter(page, "reads", R.string.diagnostic_messenger_reads);
            addCounter(page, "typing", R.string.diagnostic_messenger_typing);
            addCounter(page, "stories", R.string.diagnostic_messenger_stories);
            addCounter(page, "aiFab", R.string.diagnostic_messenger_ai);
        }
        addCounter(page, "errors", R.string.diagnostic_errors);
        Button reload = textButton(R.string.refresh, page.accent);
        reload.setOnClickListener(view -> refreshStatus(page));
        LinearLayout.LayoutParams reloadParams = size(-1, 48);
        reloadParams.topMargin = dp(8);
        page.diagnostics.addView(reload, reloadParams);
        card.addView(page.diagnostics, size(-1, -2));
        setDiagnosticsOpen(page, false);
    }

    private void addCounter(Page page, String key, int labelResource) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(6), 0, dp(6));
        row.addView(label(getString(labelResource), 13, secondaryColor, false),
                new LinearLayout.LayoutParams(0, -2, 1));
        TextView value = label(getString(R.string.no_count), 13, textColor, true);
        LinearLayout.LayoutParams valueParams = size(-2, -2);
        valueParams.setMarginStart(dp(10));
        row.addView(value, valueParams);
        page.diagnostics.addView(row, size(-1, -2));
        page.counters.put(key, value);
    }

    private void setDiagnosticsOpen(Page page, boolean open) {
        page.diagnostics.setVisibility(open ? View.VISIBLE : View.GONE);
        page.diagnosticsAction.setText(open ? R.string.hide : R.string.show);
        page.diagnosticsHeader.setContentDescription(getString(open
                ? R.string.hide_diagnostics : R.string.show_diagnostics));
    }

    private void showTab(int index) {
        int next = index == MESSENGER ? MESSENGER : FACEBOOK;
        if (tabsReady && next == selectedTab) return;
        int previous = selectedTab;
        selectedTab = next;
        if (!tabsReady || !ValueAnimator.areAnimatorsEnabled()
                || pages[previous].scroll.getVisibility() != View.VISIBLE) {
            tabsReady = true;
            settleTabs();
        } else {
            animateTabChange(previous, next);
        }
        uiPrefs.edit().putInt("selected_tab", selectedTab).apply();
        refreshStatus(pages[selectedTab]);
    }

    private void settleTabs() {
        transitionGeneration++;
        for (int tab = 0; tab < 2; tab++) {
            boolean selected = tab == selectedTab;
            Page page = pages[tab];
            page.scroll.animate().cancel();
            page.scroll.setAlpha(1f);
            page.scroll.setTranslationX(0f);
            page.scroll.setVisibility(selected ? View.VISIBLE : View.GONE);
            page.supportHeader.animate().cancel();
            page.supportHeader.setAlpha(1f);
            page.supportHeader.setTranslationY(0f);
            tabIndicators[tab].animate().cancel();
            tabIndicators[tab].setBackgroundColor(page.accent);
            tabIndicators[tab].setAlpha(selected ? 1f : 0f);
            tabIndicators[tab].setScaleX(1f);
            tabViews[tab].setSelected(selected);
            tabLabels[tab].setTextColor(selected ? page.accent : secondaryColor);
        }
    }

    private void animateTabChange(int previous, int next) {
        int generation = ++transitionGeneration;
        int direction = next > previous ? 1 : -1;
        Page outgoing = pages[previous];
        Page incoming = pages[next];
        View oldLine = tabIndicators[previous];
        View newLine = tabIndicators[next];
        outgoing.scroll.animate().cancel();
        incoming.scroll.animate().cancel();
        outgoing.supportHeader.animate().cancel();
        incoming.supportHeader.animate().cancel();
        oldLine.animate().cancel();
        newLine.animate().cancel();
        outgoing.scroll.setVisibility(View.VISIBLE);
        incoming.scroll.setVisibility(View.GONE);
        incoming.scroll.setAlpha(0f);
        incoming.scroll.setTranslationX(direction * dp(18));
        incoming.supportHeader.setAlpha(0.5f);
        incoming.supportHeader.setTranslationY(dp(6));
        for (int tab = 0; tab < 2; tab++) {
            tabViews[tab].setSelected(tab == next);
            tabLabels[tab].setTextColor(tab == next ? pages[tab].accent : secondaryColor);
        }
        oldLine.setPivotX(direction > 0 ? oldLine.getWidth() : 0f);
        newLine.setPivotX(direction > 0 ? 0f : newLine.getWidth());
        newLine.setBackgroundColor(incoming.accent);
        if (newLine.getAlpha() == 0f) newLine.setScaleX(0.6f);
        oldLine.animate().alpha(0f).scaleX(0.6f).setStartDelay(0)
                .setDuration(140).setInterpolator(new AccelerateInterpolator()).start();
        newLine.animate().alpha(1f).scaleX(1f).setStartDelay(0)
                .setDuration(260).setInterpolator(new DecelerateInterpolator()).start();
        outgoing.scroll.animate().alpha(0f).translationX(-direction * dp(16)).setStartDelay(0)
                .setDuration(100).setInterpolator(new AccelerateInterpolator())
                .withEndAction(() -> {
                    if (generation != transitionGeneration) return;
                    outgoing.scroll.setVisibility(View.GONE);
                    outgoing.scroll.setAlpha(1f);
                    outgoing.scroll.setTranslationX(0f);
                    incoming.scroll.setVisibility(View.VISIBLE);
                    incoming.scroll.animate().alpha(1f).translationX(0f).setStartDelay(0)
                            .setDuration(185).setInterpolator(new DecelerateInterpolator()).start();
                    incoming.supportHeader.animate().alpha(1f).translationY(0f).setStartDelay(15)
                            .setDuration(160).setInterpolator(new DecelerateInterpolator()).start();
                }).start();
    }

    private void refreshStatus(Page page) {
        if (page == null) return;
        Bundle data = getContentResolver().call(SettingsProvider.URI, "status",
                page.index == MESSENGER ? "messenger" : "facebook", null);
        PackageInfo host = hostPackage(page.index);
        long updated = data == null ? 0 : data.getLong("updated");
        page.version.setText(host == null ? getString(R.string.host_not_installed)
                : getString(R.string.installed_version, host.versionName));
        boolean supported = host != null && supportedHost(page.index, host);
        int supportColor = host == null ? secondaryColor : getColor(supported
                ? R.color.status_success : R.color.status_error);
        page.support.setText(host == null ? R.string.support_unavailable
                : supported ? R.string.supported : R.string.unsupported);
        page.support.setTextColor(supportColor);
        page.supportDot.setBackground(rounded(supportColor, 4));
        page.reportTime.setText(updated == 0 ? getString(R.string.no_report_yet)
                : getString(R.string.last_report, DateFormat.getTimeFormat(this).format(updated)));
        NumberFormat format = NumberFormat.getIntegerInstance();
        for (Map.Entry<String, TextView> counter : page.counters.entrySet())
            counter.getValue().setText(updated == 0 ? getString(R.string.no_count)
                    : format.format(data.getInt(counter.getKey())));
    }

    private PackageInfo hostPackage(int index) {
        try { return getPackageManager().getPackageInfo(index == FACEBOOK ? "com.facebook.katana" : "com.facebook.orca", 0); }
        catch (PackageManager.NameNotFoundException failure) { return null; }
    }

    private boolean supportedHost(int index, PackageInfo host) {
        if (index == FACEBOOK)
            return FacebookBuild.identify(host.versionName, host.getLongVersionCode()) != FacebookBuild.UNSUPPORTED;
        return "584.1.0.61.98".equals(host.versionName) && host.getLongVersionCode() == 346817021L;
    }

    private Drawable hostIcon(int index) {
        try { return getPackageManager().getApplicationIcon(index == FACEBOOK ? "com.facebook.katana" : "com.facebook.orca"); }
        catch (PackageManager.NameNotFoundException failure) { return null; }
    }

    private String moduleVersion() {
        try { return getPackageManager().getPackageInfo(getPackageName(), 0).versionName; }
        catch (PackageManager.NameNotFoundException failure) { return ""; }
    }

    private Button textButton(int labelResource, int color) {
        Button button = new Button(this);
        button.setText(labelResource);
        button.setAllCaps(false);
        button.setTextSize(14);
        button.setTextColor(color);
        button.setMinHeight(dp(48));
        button.setMinimumHeight(dp(48));
        button.setElevation(0);
        button.setBackground(new RippleDrawable(ColorStateList.valueOf((color & 0x00ffffff) | 0x22000000),
                null, rounded(surfaceColor, 10)));
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
        return new LinearLayout.LayoutParams(widthDp < 0 ? widthDp : dp(widthDp),
                heightDp < 0 ? heightDp : dp(heightDp));
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private static final class Page {
        final int index;
        final int accent;
        final ScrollView scroll;
        final LinearLayout content;
        final Map<String, TextView> counters = new LinkedHashMap<>();
        TextView version;
        TextView support;
        TextView reportTime;
        TextView diagnosticsAction;
        View supportHeader;
        View supportDot;
        View diagnosticsHeader;
        LinearLayout diagnostics;

        Page(int index, int accent, ScrollView scroll, LinearLayout content) {
            this.index = index;
            this.accent = accent;
            this.scroll = scroll;
            this.content = content;
        }
    }
}
