package ymg.pwcca.facebookgoodies;

import android.content.Context;
import android.os.Bundle;
import android.os.Process;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import de.robv.android.xposed.XposedBridge;

final class HostState {
    volatile Options options = Options.OFF;
    volatile boolean supported;
    final AtomicInteger hooks = new AtomicInteger();
    final AtomicInteger ads = new AtomicInteger();
    final AtomicInteger videoHooks = new AtomicInteger();
    final AtomicInteger reelsAds = new AtomicInteger();
    final AtomicInteger videoAds = new AtomicInteger();
    final AtomicInteger contentHooks = new AtomicInteger();
    final AtomicInteger threads = new AtomicInteger();
    final AtomicInteger storiesTop = new AtomicInteger();
    final AtomicInteger storiesMiddle = new AtomicInteger();
    final AtomicInteger reels = new AtomicInteger();
    final AtomicInteger suggestions = new AtomicInteger();
    final AtomicInteger cacheCalls = new AtomicInteger();
    final AtomicInteger renderCalls = new AtomicInteger();
    final AtomicInteger cacheRemoved = new AtomicInteger();
    final AtomicInteger renderRemoved = new AtomicInteger();
    final AtomicInteger reads = new AtomicInteger();
    final AtomicInteger captures = new AtomicInteger();
    final AtomicInteger errors = new AtomicInteger();
    void start(Context app) {
        Executors.newSingleThreadScheduledExecutor(r -> { Thread t = new Thread(r, "FacebookGoodies-status"); t.setDaemon(true); return t; })
            .scheduleWithFixedDelay(() -> exchange(app), 0, 2, TimeUnit.SECONDS);
    }
    private void exchange(Context app) {
        try {
            Bundle config = app.getContentResolver().call(SettingsProvider.URI, "config", null, null);
            options = supported ? Options.from(config) : Options.OFF;
            app.getContentResolver().call(SettingsProvider.URI, "report", null, snapshot());
        } catch (RuntimeException failed) { options = Options.OFF; }
    }
    private Bundle snapshot() {
        Bundle out = new Bundle(); out.putBoolean("supported", supported);
        out.putInt("videoHooks", videoHooks.get()); out.putInt("reelsAds", reelsAds.get()); out.putInt("videoAds", videoAds.get());
        out.putInt("contentHooks", contentHooks.get()); out.putInt("threads", threads.get());
        out.putInt("storiesTop", storiesTop.get()); out.putInt("storiesMiddle", storiesMiddle.get()); out.putInt("reels", reels.get());
        out.putInt("hooks", hooks.get()); out.putInt("ads", ads.get()); out.putInt("suggestions", suggestions.get()); out.putInt("reads", reads.get());
        out.putInt("cacheCalls", cacheCalls.get()); out.putInt("renderCalls", renderCalls.get());
        out.putInt("cacheRemoved", cacheRemoved.get()); out.putInt("renderRemoved", renderRemoved.get());
        out.putInt("captures", captures.get()); out.putInt("errors", errors.get()); out.putInt("starts", 1); out.putInt("pid", Process.myPid()); return out;
    }
    void removed(FilterPolicy.Action action) {
        switch (action) {
            case AD: ads.incrementAndGet(); break;
            case SUGGESTION: suggestions.incrementAndGet(); break;
            case THREADS: threads.incrementAndGet(); break;
            case STORIES_TOP: storiesTop.incrementAndGet(); break;
            case STORIES_MIDDLE: storiesMiddle.incrementAndGet(); break;
            case REELS: reels.incrementAndGet(); break;
            case KEEP: break;
        }
    }
    void failure(String point) { errors.incrementAndGet(); XposedBridge.log("FacebookGoodies: failed hook " + point); }
    void videoAdFiltered(VideoAdPolicy.Surface surface) {
        if (surface == VideoAdPolicy.Surface.NONE) return;
        ads.incrementAndGet();
        if (surface == VideoAdPolicy.Surface.REELS) reelsAds.incrementAndGet();
        else videoAds.incrementAndGet();
    }
}
