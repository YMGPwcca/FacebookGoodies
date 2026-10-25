package ymg.pwcca.facebookgoodies;

import android.content.Context;
import android.os.Bundle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

final class MessengerPrivacyHooks {
    private volatile boolean seen;
    private volatile boolean typing;
    private volatile boolean storySeen;
    private final AtomicInteger installedHooks = new AtomicInteger();
    private final AtomicInteger readsBlocked = new AtomicInteger();
    private final AtomicInteger typingBlocked = new AtomicInteger();
    private final AtomicInteger storiesBlocked = new AtomicInteger();
    private final AtomicInteger errors = new AtomicInteger();
    private boolean aiFabInstalled;
    private MessengerReadControls readControls;

    static void install(Context app, ClassLoader loader, boolean aiFabInstalled) {
        MessengerPrivacyHooks hooks = new MessengerPrivacyHooks();
        hooks.aiFabInstalled = aiFabInstalled;
        hooks.refresh(app);
        XposedBridge.log("FacebookGoodies: Messenger privacy enabled seen=" + hooks.seen
                + " typing=" + hooks.typing + " story=" + hooks.storySeen);
        hooks.installSeen(loader);
        hooks.readControls = new MessengerReadControls(app, loader, () -> hooks.seen);
        hooks.readControls.install();
        hooks.installTyping(loader);
        hooks.installStorySeen(loader);
        hooks.report(app);
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "FacebookGoodies-Messenger-settings");
            thread.setDaemon(true);
            return thread;
        }).scheduleWithFixedDelay(() -> {
            hooks.refresh(app);
            hooks.report(app);
        }, 2, 2, TimeUnit.SECONDS);
    }

    private void report(Context app) {
        Bundle status = new Bundle();
        status.putBoolean("supported", true);
        status.putInt("hooks", installedHooks.get() + (aiFabInstalled ? 1 : 0)
                + (readControls == null ? 0 : readControls.installedGroups()));
        status.putInt("aiFab", aiFabInstalled ? 1 : 0);
        status.putInt("reads", readsBlocked.get() + (readControls == null ? 0 : readControls.blockedCount()));
        status.putInt("typing", typingBlocked.get());
        status.putInt("stories", storiesBlocked.get());
        status.putInt("errors", errors.get() + (readControls == null ? 0 : readControls.errorCount()));
        try { app.getContentResolver().call(SettingsProvider.URI, "report", null, status); }
        catch (RuntimeException ignored) { }
    }

    private void refresh(Context app) {
        try {
            Bundle config = app.getContentResolver().call(SettingsProvider.URI, "config", null, null);
            seen = config != null && config.getBoolean("messenger_seen");
            typing = config != null && config.getBoolean("messenger_typing");
            storySeen = config != null && config.getBoolean("messenger_story_seen");
        } catch (RuntimeException failure) {
            seen = typing = storySeen = false;
        }
        if (readControls != null) readControls.updateButtonVisibility();
    }

    private void installSeen(ClassLoader loader) {
        try {
            Class<?> callback = Class.forName("X.FZ1", false, loader);
            Field kind = callback.getDeclaredField("$t");
            Field futureField = callback.getDeclaredField("A03");
            kind.setAccessible(true);
            futureField.setAccessible(true);
            hookVoid(callback.getDeclaredMethod("onCompletion", Object.class), new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) throws IllegalAccessException {
                    if (seen && kind.getInt(param.thisObject) == 3
                            && cancelAndSkip(param, futureField.get(param.thisObject)))
                        readsBlocked.incrementAndGet();
                }
            });

            Class<?> shim = Class.forName("com.facebook.messengershimcqljava.SHIMThreadWriterQuery", false, loader);
            hookVoid(shim.getDeclaredMethod("nativeSHIMTransportHybridThreadMarkThreadRead",
                    long.class, long.class, long.class, String.class), new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) {
                    if (seen) {
                        param.setResult(null);
                        readsBlocked.incrementAndGet();
                    }
                }
            });
            XposedBridge.log("FacebookGoodies: Messenger seen hooks installed");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            errors.incrementAndGet();
            XposedBridge.log("FacebookGoodies: Messenger seen hook unavailable: " + failure.getClass().getSimpleName());
        }
    }

    private void installTyping(ClassLoader loader) {
        try {
            Class<?> mqtt = Class.forName("X.3hu", false, loader);
            hookVoid(mqtt.getDeclaredMethod("A00", Integer.class, String.class, byte[].class), new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) {
                    if (typing && "/t_st".equals(param.args[1])) {
                        param.setResult(null);
                        typingBlocked.incrementAndGet();
                    }
                }
            });

            Class<?> callback = Class.forName("X.9qD", false, loader);
            Field futureField = callback.getDeclaredField("A01");
            futureField.setAccessible(true);
            hookVoid(callback.getDeclaredMethod("onCompletion", Object.class), new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) throws IllegalAccessException {
                    if (typing && cancelAndSkip(param, futureField.get(param.thisObject)))
                        typingBlocked.incrementAndGet();
                }
            });
            XposedBridge.log("FacebookGoodies: Messenger typing hooks installed");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            errors.incrementAndGet();
            XposedBridge.log("FacebookGoodies: Messenger typing hook unavailable: " + failure.getClass().getSimpleName());
        }
    }

    private void installStorySeen(ClassLoader loader) {
        try {
            Class<?> card = Class.forName("com.facebook.messaging.montage.model.MontageCard", false, loader);
            XC_MethodHook skip = new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) {
                    if (storySeen) {
                        param.setResult(null);
                        storiesBlocked.incrementAndGet();
                    }
                }
            };
            for (String name : new String[]{"X.Nwi", "X.Nwj"}) {
                Class<?> handler = Class.forName(name, false, loader);
                hookVoid(handler.getDeclaredMethod("C7r", card, boolean.class), skip);
            }
            XposedBridge.log("FacebookGoodies: Messenger story seen hooks installed");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            errors.incrementAndGet();
            XposedBridge.log("FacebookGoodies: Messenger story seen hook unavailable: " + failure.getClass().getSimpleName());
        }
    }

    private static boolean cancelAndSkip(XC_MethodHook.MethodHookParam param, Object value) {
        if (!(value instanceof Future<?>)) return false;
        Future<?> future = (Future<?>) value;
        if (future.cancel(false) || future.isCancelled()) {
            param.setResult(null);
            return true;
        }
        return false;
    }

    private void hookVoid(Method method, XC_MethodHook callback) throws NoSuchMethodException {
        if (method.getReturnType() != void.class)
            throw new NoSuchMethodException("Messenger hook return contract changed: " + method.getName());
        XposedBridge.hookMethod(method, callback);
        installedHooks.incrementAndGet();
    }
}
