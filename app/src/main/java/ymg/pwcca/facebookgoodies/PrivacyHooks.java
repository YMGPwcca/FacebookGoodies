package ymg.pwcca.facebookgoodies;

import android.app.Activity;
import android.database.ContentObserver;
import java.lang.reflect.Method;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

final class PrivacyHooks {
    static void install(ClassLoader loader, HostState state) {
        installReadReceipts(loader, state);
        installCaptureCallbacks(state);
        for (String name : new String[]{"com.facebook.screenshot.ScreenshotContentObserver"}) installObserver(loader, name, state);
    }
    private static void installReadReceipts(ClassLoader loader, HostState state) {
        try {
            Class<?> type = Class.forName("com.facebook.messengershimcqljava.SHIMThreadWriterQuery", false, loader);
            Method method = type.getDeclaredMethod("nativeSHIMTransportHybridThreadMarkThreadRead", long.class, long.class, long.class, String.class);
            state.hooks.addAndGet(XposedBridge.hookAllMethods(type, method.getName(), stop(state, false)).size());
        } catch (ReflectiveOperationException | RuntimeException failure) { state.failure("read-receipts"); }
    }
    private static void installCaptureCallbacks(HostState state) {
        state.hooks.addAndGet(XposedBridge.hookAllMethods(Activity.class, "registerScreenCaptureCallback", stop(state, true)).size());
    }
    private static void installObserver(ClassLoader loader, String name, HostState state) {
        try {
            Class<?> type = Class.forName(name, false, loader);
            if (!ContentObserver.class.isAssignableFrom(type)) return;
            state.hooks.addAndGet(XposedBridge.hookAllMethods(type, "onChange", stop(state, true)).size());
        } catch (ClassNotFoundException | RuntimeException failure) { state.failure("capture-observer"); }
    }
    private static XC_MethodHook stop(HostState state, boolean capture) {
        return new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam param) {
                boolean enabled = capture ? state.options.capture : state.options.receipts;
                if (!enabled || ((Method) param.method).getReturnType() != void.class) return;
                param.setResult(null);
                if (capture) state.captures.incrementAndGet(); else state.reads.incrementAndGet();
            }
        };
    }
}
