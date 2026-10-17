package ymg.pwcca.facebookgoodies;

import java.lang.reflect.Method;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

final class FeedCacheHooks583 {
    static void install(ClassLoader loader, FeedCollections583 collections, HostState state) {
        for (String name : new String[]{"X.25I", "X.kep", "X.keq"}) installCache(loader, name, collections, state);
        installStorage(loader, collections, state);
    }
    private static void installCache(ClassLoader loader, String name, FeedCollections583 collections, HostState state) {
        try {
            Class<?> type = Class.forName(name, false, loader);
            for (Method method : type.getDeclaredMethods()) {
                if (!"AsB".equals(method.getName()) || !method.getReturnType().getName().equals("X.2Sj")) continue;
                int index = immutableListIndex(method);
                if (index >= 0) hook(method, index, collections, state);
            }
        } catch (ClassNotFoundException | RuntimeException | LinkageError failed) { state.failure("csr-cache-" + name); }
    }
    private static void installStorage(ClassLoader loader, FeedCollections583 collections, HostState state) {
        try {
            Class<?> type = Class.forName("X.252", false, loader);
            for (Method method : type.getDeclaredMethods()) {
                if (!"AB8".equals(method.getName())) continue;
                int index = immutableListIndex(method);
                if (index >= 0) hook(method, index, collections, state);
            }
        } catch (ClassNotFoundException | RuntimeException | LinkageError failed) { state.failure("csr-storage"); }
    }
    private static int immutableListIndex(Method method) {
        Class<?>[] types = method.getParameterTypes();
        for (int i = 0; i < types.length; i++)
            if (types[i].getName().equals("com.google.common.collect.ImmutableList")) return i;
        return -1;
    }
    private static void hook(Method method, int index, FeedCollections583 collections, HostState state) {
        XposedBridge.hookMethod(method, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam param) {
                state.cacheCalls.incrementAndGet();
                Options options = state.options;
                if (!options.hasFeedFilters()) return;
                param.args[index] = collections.filter(param.args[index], method.getParameterTypes()[index], options, true);
            }
        });
        state.hooks.incrementAndGet();
    }
}
