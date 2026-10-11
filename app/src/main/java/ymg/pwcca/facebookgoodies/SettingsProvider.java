package ymg.pwcca.facebookgoodies;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Process;
import java.util.Arrays;

public final class SettingsProvider extends ContentProvider {
    static final Uri URI = Uri.parse("content://ymg.pwcca.facebookgoodies.settings");
    static final String PREFS = "controls";
    private static final String[] COUNTERS = {"hooks", "ads", "suggestions", "reads", "captures", "errors", "starts", "pid", "cacheCalls", "renderCalls", "cacheRemoved", "renderRemoved", "videoHooks", "reelsAds", "videoAds", "videoResponses", "videoUnmatched", "videoSourceCode", "reelsLaunchFlags", "affiliateCards", "videoMetadata", "videoCards", "contentHooks", "threads", "storiesTop", "storiesMiddle", "reels"};
    @Override public boolean onCreate() { return true; }
    private boolean isFacebook(int uid) {
        String[] names = getContext().getPackageManager().getPackagesForUid(uid);
        return names != null && Arrays.asList(names).contains("com.facebook.katana");
    }
    private boolean mayRead(int uid) { return uid == Process.myUid() || uid == 2000 || isFacebook(uid); }
    @Override public Bundle call(String method, String arg, Bundle extras) {
        int uid = Binder.getCallingUid();
        if (!mayRead(uid)) throw new SecurityException("Caller not allowed");
        if ("report".equals(method)) return acceptReport(uid, extras);
        if ("config".equals(method)) return configuration();
        if ("status".equals(method)) return status();
        throw new IllegalArgumentException("Unknown operation");
    }
    private Bundle configuration() {
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS, 0);
        Bundle out = new Bundle();
        out.putBoolean("ads", prefs.getBoolean("ads", true));
        out.putBoolean("suggestions", prefs.getBoolean("suggestions", true));
        out.putBoolean("receipts", prefs.getBoolean("receipts", true));
        out.putBoolean("capture", prefs.getBoolean("capture", true));
        out.putBoolean("downloads", prefs.getBoolean("downloads", true));
        for (String key : new String[]{"hide_threads", "hide_stories_top", "hide_stories_middle", "hide_reels"})
            out.putBoolean(key, prefs.getBoolean(key, false));
        return out;
    }
    private Bundle acceptReport(int uid, Bundle data) {
        if (!isFacebook(uid) || data == null) throw new SecurityException("Only Facebook can report");
        SharedPreferences.Editor edit = getContext().getSharedPreferences("diagnostics", 0).edit();
        for (String name : COUNTERS)
            edit.putInt(name, Math.max(0, data.getInt(name)));
        edit.putBoolean("supported", data.getBoolean("supported"));
        for (String name : new String[]{"downloadHooks", "downloads", "downloadFailures", "downloadStage", "downloadError", "downloadSidebarRows", "muxStage", "muxError", "videoCodec", "videoWidth", "videoHeight", "audioCodec", "lastMuxFailureStage", "lastMuxFailureError", "lastMuxFailureVideoCodec", "lastMuxFailureAudioCodec", "videoContainer", "audioContainer", "portableStage", "portableError"})
            edit.putInt(name, Math.max(0, data.getInt(name)));
        edit.putLong("updated", System.currentTimeMillis());
        edit.apply();
        return new Bundle();
    }
    private Bundle status() {
        SharedPreferences prefs = getContext().getSharedPreferences("diagnostics", 0);
        Bundle out = new Bundle();
        for (String name : COUNTERS)
            out.putInt(name, prefs.getInt(name, 0));
        out.putBoolean("supported", prefs.getBoolean("supported", false));
        for (String name : new String[]{"downloadHooks", "downloads", "downloadFailures", "downloadStage", "downloadError", "downloadSidebarRows", "muxStage", "muxError", "videoCodec", "videoWidth", "videoHeight", "audioCodec", "lastMuxFailureStage", "lastMuxFailureError", "lastMuxFailureVideoCodec", "lastMuxFailureAudioCodec", "videoContainer", "audioContainer", "portableStage", "portableError"})
            out.putInt(name, prefs.getInt(name, 0));
        out.putLong("updated", prefs.getLong("updated", 0));
        return out;
    }
    @Override public Cursor query(Uri u, String[] p, String s, String[] a, String o) { return null; }
    @Override public String getType(Uri u) { return null; }
    @Override public Uri insert(Uri u, ContentValues v) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri u, String s, String[] a) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri u, ContentValues v, String s, String[] a) { throw new UnsupportedOperationException(); }
}
