package ymg.pwcca.facebookgoodies;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/** Stops Messenger from reporting that the user is active. */
final class MessengerPresenceHooks {
    private MessengerPresenceHooks() { }

    static int install(ClassLoader loader, BooleanSupplier enabled, AtomicInteger blocked)
            throws ReflectiveOperationException {
        int installed = 0;
        RuntimeException last = null;
        for (String name : new String[]{
                "com.facebook.presence.DefaultPresenceManager",
                "com.facebook.presence.PresenceManagerV2"}) {
            try {
                Class<?> manager = Class.forName(name, false, loader);
                for (String method : new String[]{
                        "onAppActive", "onAppPaused", "onAppStopped",
                        "onDeviceActive", "onDeviceStopped"})
                    installed += hookLifecycle(manager, method, enabled, blocked);
            } catch (ClassNotFoundException | RuntimeException failure) {
                last = failure instanceof RuntimeException ? (RuntimeException) failure : null;
            }
        }
        for (String name : new String[]{
                "com.facebook.distribgw.client.presence.binding.DgwPresenceClient",
                "com.facebook.distribgw.client.presence.DgwPresenceClientImpl"}) {
            try {
                Class<?> client = Class.forName(name, false, loader);
                Method setting = client.getDeclaredMethod("setPresenceUserSettingEnabled", boolean.class, String.class);
                XposedBridge.hookMethod(setting, new XC_MethodHook() {
                    @Override protected void beforeHookedMethod(MethodHookParam param) {
                        if (enabled.getAsBoolean() && Boolean.TRUE.equals(param.args[0])) {
                            param.args[0] = false;
                            blocked.incrementAndGet();
                        }
                    }
                });
                installed++;
            } catch (ClassNotFoundException | NoSuchMethodException | RuntimeException ignored) {
                // This build does not use the Distributed Gateway presence client.
            }
        }
        if (installed == 0 && last != null) throw last;
        return installed;
    }

    private static int hookLifecycle(Class<?> manager, String name, BooleanSupplier enabled,
                                     AtomicInteger blocked) {
        int installed = 0;
        for (Method method : manager.getDeclaredMethods()) {
            if (!method.getName().equals(name) || method.getReturnType() != void.class) continue;
            XposedBridge.hookMethod(method, new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) {
                    if (!enabled.getAsBoolean()) return;
                    param.setResult(null);
                    blocked.incrementAndGet();
                }
            });
            installed++;
        }
        return installed;
    }
}
