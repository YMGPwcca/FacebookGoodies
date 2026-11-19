package ymg.pwcca.facebookgoodies;

import java.lang.reflect.Method;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

final class FeedCacheHooks587 {
    static void install(FacebookHostBindings loader, FeedCollections587 collections, HostState state) {
        for (String name : new String[]{"X.027i", "X.0Vrn", "X.0Vro"}) installCache(loader, name, collections, state);
        installStorage(loader, collections, state);
    }
    private static void installCache(FacebookHostBindings loader, String name, FeedCollections587 collections, HostState state) {
        try {
            Class<?> type = loader.type(name);
            for (Method method : type.getDeclaredMethods()) {
                if (!loader.methodName(type, "Bkr").equals(method.getName()) || !method.getReturnType().getName().equals(loader.typeName("X.02Xb"))) continue;
                int index = immutableListIndex(method);
                if (index >= 0) hook(method, index, collections, state);
            }
        } catch (ClassNotFoundException | RuntimeException | LinkageError failed) { state.failure("csr-cache-" + name); }
    }
    private static void installStorage(FacebookHostBindings loader, FeedCollections587 collections, HostState state) {
        try {
            Class<?> type = loader.type("X.027e");
            for (Method method : type.getDeclaredMethods()) {
                if (!loader.methodName(type, "AAb").equals(method.getName())) continue;
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
    private static void hook(Method method, int index, FeedCollections587 collections, HostState state) {
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
