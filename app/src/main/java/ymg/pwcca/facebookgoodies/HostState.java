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
    final AtomicInteger downloadHooks = new AtomicInteger();
    final AtomicInteger downloads = new AtomicInteger();
    final AtomicInteger downloadFailures = new AtomicInteger();
    final AtomicInteger downloadStage = new AtomicInteger();
    final AtomicInteger downloadError = new AtomicInteger();
    final AtomicInteger downloadSidebarRows = new AtomicInteger();
    final AtomicInteger muxStage = new AtomicInteger();
    final AtomicInteger muxError = new AtomicInteger();
    final AtomicInteger videoCodec = new AtomicInteger();
    final AtomicInteger videoWidth = new AtomicInteger();
    final AtomicInteger videoHeight = new AtomicInteger();
    final AtomicInteger audioCodec = new AtomicInteger();
    final AtomicInteger lastMuxFailureStage = new AtomicInteger();
    final AtomicInteger lastMuxFailureError = new AtomicInteger();
    final AtomicInteger lastMuxFailureVideoCodec = new AtomicInteger();
    final AtomicInteger lastMuxFailureAudioCodec = new AtomicInteger();
    final AtomicInteger videoContainer = new AtomicInteger();
    final AtomicInteger audioContainer = new AtomicInteger();
    final AtomicInteger portableStage = new AtomicInteger();
    final AtomicInteger portableError = new AtomicInteger();
    final AtomicInteger ads = new AtomicInteger();
    final AtomicInteger videoHooks = new AtomicInteger();
    final AtomicInteger reelsAds = new AtomicInteger();
    final AtomicInteger videoAds = new AtomicInteger();
    final AtomicInteger videoResponses = new AtomicInteger();
    final AtomicInteger videoUnmatched = new AtomicInteger();
    final AtomicInteger videoSourceCode = new AtomicInteger();
    final AtomicInteger reelsLaunchFlags = new AtomicInteger();
    final AtomicInteger affiliateCards = new AtomicInteger();
    final AtomicInteger videoMetadata = new AtomicInteger();
    final AtomicInteger videoCards = new AtomicInteger();
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
        out.putInt("downloadHooks", downloadHooks.get()); out.putInt("downloads", downloads.get()); out.putInt("downloadFailures", downloadFailures.get());
        out.putInt("downloadStage", downloadStage.get()); out.putInt("downloadError", downloadError.get());
        out.putInt("downloadSidebarRows", downloadSidebarRows.get());
        out.putInt("muxStage", muxStage.get()); out.putInt("muxError", muxError.get());
        out.putInt("videoCodec", videoCodec.get()); out.putInt("videoWidth", videoWidth.get());
        out.putInt("videoHeight", videoHeight.get()); out.putInt("audioCodec", audioCodec.get());
        out.putInt("lastMuxFailureStage", lastMuxFailureStage.get()); out.putInt("lastMuxFailureError", lastMuxFailureError.get());
        out.putInt("lastMuxFailureVideoCodec", lastMuxFailureVideoCodec.get()); out.putInt("lastMuxFailureAudioCodec", lastMuxFailureAudioCodec.get());
        out.putInt("videoContainer", videoContainer.get()); out.putInt("audioContainer", audioContainer.get());
        out.putInt("portableStage", portableStage.get()); out.putInt("portableError", portableError.get());
        out.putInt("videoHooks", videoHooks.get()); out.putInt("reelsAds", reelsAds.get()); out.putInt("videoAds", videoAds.get());
        out.putInt("videoResponses", videoResponses.get()); out.putInt("videoUnmatched", videoUnmatched.get());
        out.putInt("videoSourceCode", videoSourceCode.get());
        out.putInt("reelsLaunchFlags", reelsLaunchFlags.get()); out.putInt("affiliateCards", affiliateCards.get());
        out.putInt("videoMetadata", videoMetadata.get()); out.putInt("videoCards", videoCards.get());
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
