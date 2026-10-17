package ymg.pwcca.facebookgoodies;

import android.app.Activity;
import android.app.Application;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

final class VideoDownloadHooks583 {
    private final HostState state;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final CurrentVideo current = new CurrentVideo();
    private final VideoDownloader downloader;
    private final AtomicBoolean pending = new AtomicBoolean();
    private final AtomicReference<WeakReference<Object>> candidate = new AtomicReference<>(new WeakReference<>(null));
    private final Method params;
    private final Field view, id, live, sponsored, source, manifest, hd, sd;

    static void install(Application app, ClassLoader loader, HostState state) {
        try { new VideoDownloadHooks583(app, loader, state).install(app, loader); }
        catch (ReflectiveOperationException | RuntimeException | LinkageError failure) { state.failure("download-install"); }
    }
    private VideoDownloadHooks583(Application app, ClassLoader loader, HostState state) throws ReflectiveOperationException {
        this.state = state; downloader = new VideoDownloader(app, state);
        Class<?> player = loader.loadClass("X.5B3");
        Class<?> playerParams = loader.loadClass("com.facebook.video.engine.api.VideoPlayerParams");
        Class<?> dataSource = loader.loadClass("com.facebook.video.engine.api.VideoDataSource");
        params = player.getDeclaredMethod("CS6"); params.setAccessible(true);
        if (params.getReturnType() != playerParams) throw new NoSuchMethodException("CS6 return type");
        view = field(player, "A04", android.view.ViewGroup.class);
        id = field(playerParams, "A0x", String.class);
        live = field(playerParams, "A1m", boolean.class);
        sponsored = field(playerParams, "A1v", boolean.class);
        source = field(playerParams, "A0d", dataSource);
        manifest = field(dataSource, "A0C", String.class);
        hd = field(dataSource, "A07", android.net.Uri.class);
        sd = field(dataSource, "A08", android.net.Uri.class);
    }
    private static Field field(Class<?> owner, String name, Class<?> type) throws ReflectiveOperationException {
        Field field = owner.getDeclaredField(name);
        if (field.getType() != type) throw new NoSuchFieldException(name + " type");
        field.setAccessible(true); return field;
    }
    private void install(Application app, ClassLoader loader) throws ReflectiveOperationException {
        new DownloadSidebarHooks583(loader, state, () -> main.post(this::download)).install(loader);
        Class<?> player = loader.loadClass("X.5B3");
        hook(player.getDeclaredMethod("isPlaying"), true);
        hook(player.getDeclaredMethod("ESY", loader.loadClass("X.2Pf")), false);
        hook(player.getDeclaredMethod("maybeTrackVideoStart", loader.loadClass("X.2Pf")), false);
        app.registerActivityLifecycleCallbacks(new Lifecycle());
        main.post(this::tick);
    }
    private void hook(Method method, boolean playingResult) {
        XposedBridge.hookMethod(method, new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                if (param.hasThrowable() || !state.options.downloads) return;
                if (playingResult && !Boolean.TRUE.equals(param.getResult())) return;
                offer(param.thisObject);
            }
        });
        state.hooks.incrementAndGet(); state.downloadHooks.incrementAndGet();
    }
    private void offer(Object player) {
        candidate.set(new WeakReference<>(player));
        if (!pending.compareAndSet(false, true)) return;
        main.post(() -> {
            pending.set(false);
            Object next = candidate.getAndSet(new WeakReference<>(null)).get();
            if (next != null && state.options.downloads) select(next);
        });
    }
    private void select(Object player) {
        try {
            View playerView = (View) view.get(player);
            if (!visible(playerView)) return;
            DownloadMedia media = read(player);
            if (media == null) { clear(); return; }
            current.replace(player, media);
        } catch (ReflectiveOperationException | RuntimeException failure) { clear(); state.failure("download-player"); }
    }
    private DownloadMedia read(Object player) throws ReflectiveOperationException {
        Object value = params.invoke(player);
        if (value == null || live.getBoolean(value) || sponsored.getBoolean(value)) return null;
        String videoId = (String) id.get(value);
        Object data = source.get(value);
        if (!DownloadMedia.present(videoId) || data == null) return null;
        DownloadMedia media = new DownloadMedia(videoId, (String) manifest.get(data), string(hd.get(data)), string(sd.get(data)));
        return media.hasSource() ? media : null;
    }
    private static String string(Object value) { return value == null ? null : value.toString(); }
    private boolean visible(View view) {
        if (view == null || !view.isAttachedToWindow() || !view.isShown()) return false;
        if (!view.hasWindowFocus() && !downloader.busy()) return false;
        Rect rect = new Rect();
        if (!view.getGlobalVisibleRect(rect)) return false;
        long total = (long) view.getWidth() * view.getHeight();
        return total > 0 && (long) rect.width() * rect.height() >= total / 2;
    }
    private void tick() {
        if (!state.options.downloads) clear();
        Object player = current.player();
        if (player != null) refresh(player);
        main.postDelayed(this::tick, 500);
    }
    private void refresh(Object player) {
        try {
            if (!visible((View) view.get(player))) { clear(); return; }
            select(player);
        } catch (IllegalAccessException | RuntimeException failure) { clear(); }
    }
    private void download() {
        Object player = current.player();
        Activity owner = owner(player);
        if (player == null || owner == null || downloader.busy()) return;
        try {
            if (!state.options.downloads || !visible((View) view.get(player))) { clear(); return; }
            DownloadMedia snapshot = read(player);
            if (snapshot == null) { clear(); return; }
            downloader.start(snapshot, DownloadButton.progress(owner, downloader, state));
        } catch (ReflectiveOperationException | RuntimeException failure) { clear(); state.failure("download-start"); }
    }
    private Activity owner(Object player) {
        if (player == null) return null;
        try { View playerView = (View) view.get(player); return playerView == null ? null : DownloadButton.activity(playerView.getContext()); }
        catch (IllegalAccessException failure) { return null; }
    }
    private void clear() { current.clear(); candidate.set(new WeakReference<>(null)); }
    private final class Lifecycle implements Application.ActivityLifecycleCallbacks {
        @Override public void onActivityPaused(Activity activity) { if (owner(current.player()) == activity) clear(); }
        @Override public void onActivityDestroyed(Activity activity) { if (owner(current.player()) == activity) clear(); }
        @Override public void onActivityCreated(Activity activity, Bundle state) {}
        @Override public void onActivityStarted(Activity activity) {}
        @Override public void onActivityResumed(Activity activity) {}
        @Override public void onActivityStopped(Activity activity) {}
        @Override public void onActivitySaveInstanceState(Activity activity, Bundle state) {}
    }
}
