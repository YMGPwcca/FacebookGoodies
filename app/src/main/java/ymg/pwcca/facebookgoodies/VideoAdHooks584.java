package ymg.pwcca.facebookgoodies;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/** Ad-only contracts for Facebook 584.0.0.51.56 / 475817510. */
final class VideoAdHooks584 {
    private static final String[] SELECT = {"X.3iB", "X.3nh", "java.util.List", "int"};
    private static final String[] SELECT_VALIDATED = {
            "com.facebook.auth.usersession.FbUserSession", "X.3i9", "X.3iB", "X.3nh",
            "java.util.List", "java.util.List", "int", "boolean"};

    static void install(ClassLoader loader, HostState state) {
        attempt("reels-pool", state, () -> pool(loader, "X.5RW", "A0V", SELECT, null, VideoAdPolicy.Surface.REELS, state));
        attempt("reels-pool-validated", state, () -> pool(loader, "X.3T6", "A0T", SELECT_VALIDATED, "X.5RW", VideoAdPolicy.Surface.REELS, state));
        for (String owner : new String[]{"X.5pp", "X.5po"}) {
            attempt("video-pool", state, () -> pool(loader, owner, "A0V", SELECT, null, VideoAdPolicy.Surface.VIDEO, state));
            attempt("video-pool-validated", state, () -> pool(loader, owner, "A0T", SELECT_VALIDATED, null, VideoAdPolicy.Surface.VIDEO, state));
        }
        for (String owner : new String[]{"X.Smq", "X.Smr"})
            attempt("reels-completion", state, () -> reelsCompletion(loader, owner, state));
        VideoAdResponses584.install(loader, state);
        VideoAdMetadataHooks584.install(loader, state);
        attempt("reels-banner", state, () -> reelsBanner(loader, state));
        ReelsOverlayHooks584.install(loader, state);
        XposedBridge.log("FacebookGoodies: video hooks=" + state.videoHooks.get());
    }

    private static void pool(ClassLoader loader, String owner, String name, String[] parameters,
                             String receiverType, VideoAdPolicy.Surface surface, HostState state) throws ReflectiveOperationException {
        Method method = method(loader, owner, name, "X.710", parameters);
        Class<?> receiver = receiverType == null ? null : Class.forName(receiverType, false, loader);
        // Keep the pool's normal accounting and callbacks; null is its native no-selection result.
        register(method, state, new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                if (!state.options.ads || param.hasThrowable() || param.getResult() == null) return;
                if (receiver != null && !receiver.isInstance(param.thisObject)) return;
                param.setResult(null);
                state.videoAdFiltered(surface);
            }
        });
    }

    private static void reelsCompletion(ClassLoader loader, String owner, HostState state) throws ReflectiveOperationException {
        Method success = method(loader, owner, "A02", "void", new String[]{"X.7gJ"});
        Method noAd = method(loader, owner, "A01", "void", new String[]{});
        // A01 removes the player's fetch marker and completes the ad request with NO_AD_RETURN.
        register(success, state, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam param) {
                if (!state.options.ads) return;
                try {
                    noAd.invoke(param.thisObject);
                    param.setResult(null);
                    state.videoAdFiltered(VideoAdPolicy.Surface.REELS);
                } catch (ReflectiveOperationException | RuntimeException failed) {
                    state.failure("reels-completion");
                }
            }
        });
    }

    private static void reelsBanner(ClassLoader loader, HostState state) throws ReflectiveOperationException {
        Method render = method(loader, "X.SaE", "A0l", "X.3SE", new String[]{"X.3tD"});
        Constructor<?> empty = Class.forName("X.26x", false, loader).getDeclaredConstructor();
        empty.setAccessible(true);
        if (!render.getReturnType().isAssignableFrom(empty.getDeclaringClass()))
            throw new NoSuchMethodException("Reels empty component contract mismatch");
        register(render, state, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam param) {
                if (!state.options.ads) return;
                try {
                    param.setResult(empty.newInstance());
                    state.videoAdFiltered(VideoAdPolicy.Surface.REELS);
                } catch (ReflectiveOperationException | RuntimeException failed) {
                    state.failure("reels-banner");
                }
            }
        });
    }

    private static Method method(ClassLoader loader, String owner, String name, String result,
                                 String[] parameterNames) throws ReflectiveOperationException {
        Class<?>[] parameters = new Class<?>[parameterNames.length];
        for (int i = 0; i < parameters.length; i++) parameters[i] = type(loader, parameterNames[i]);
        Method method = Class.forName(owner, false, loader).getDeclaredMethod(name, parameters);
        if (method.getReturnType() != type(loader, result)) throw new NoSuchMethodException("Video return contract mismatch");
        method.setAccessible(true);
        return method;
    }

    private static Class<?> type(ClassLoader loader, String name) throws ClassNotFoundException {
        if ("void".equals(name)) return void.class;
        if ("int".equals(name)) return int.class;
        if ("boolean".equals(name)) return boolean.class;
        return Class.forName(name, false, loader);
    }

    private static void register(Method method, HostState state, XC_MethodHook callback) {
        XposedBridge.hookMethod(method, callback);
        state.hooks.incrementAndGet();
        state.videoHooks.incrementAndGet();
    }

    private static void attempt(String point, HostState state, Install install) {
        try { install.run(); }
        catch (ReflectiveOperationException | RuntimeException | LinkageError failed) { state.failure(point); }
    }

    @FunctionalInterface private interface Install { void run() throws ReflectiveOperationException; }
}
