package ymg.pwcca.facebookgoodies;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/** Ad-only pool, completion and banner hooks for Facebook 586.0.0.52.53 / 476217123. */
final class VideoAdHooks586 {
    private static final String[] SELECT = {"X.060V", "X.03Vb", "java.util.List", "int"};
    private static final String[] SELECT_VALIDATED = {
            "com.facebook.auth.usersession.FbUserSession", "X.03QL", "X.060V", "X.03Vb",
            "java.util.List", "java.util.List", "int", "boolean"};

    static void install(ClassLoader loader, HostState state) {
        attempt("reels-pool", state, () -> pool(loader, "X.05U4", "A0X", SELECT, null, VideoAdPolicy.Surface.REELS, state));
        attempt("reels-pool-validated", state, () -> pool(loader, "X.03En", "A0V", SELECT_VALIDATED,
                "X.05U4", VideoAdPolicy.Surface.REELS, state));
        for (String owner : new String[]{"X.06I0", "X.06Hz"}) {
            attempt("video-pool", state, () -> pool(loader, owner, "A0X", SELECT, null, VideoAdPolicy.Surface.VIDEO, state));
            attempt("video-pool-validated", state, () -> pool(loader, owner, "A0V", SELECT_VALIDATED,
                    null, VideoAdPolicy.Surface.VIDEO, state));
        }
        VideoAdResponses586.install(loader, state);
        attempt("reels-completion", state, () -> reelsCompletion(loader, "X.0ReI", state));
        // This sibling used the inherited callback implementation in 585; 586 declares the same
        // callback pair on the concrete class, so restrict the hook to that receiver.
        attempt("reels-completion-inherited", state, () -> reelsCompletion(loader, "X.0ReJ", state));
        VideoAdMetadataHooks586.install(loader, state);
        attempt("reels-banner", state, () -> reelsBanner(loader, state));
        ReelsOverlayHooks586.install(loader, state);
        XposedBridge.log("FacebookGoodies: video hooks=" + state.videoHooks.get());
    }

    private static void pool(ClassLoader loader, String owner, String name, String[] parameters,
                             String receiverType, VideoAdPolicy.Surface surface, HostState state) throws ReflectiveOperationException {
        Method method = method(loader, owner, name, "X.067I", parameters);
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

    private static void reelsBanner(ClassLoader loader, HostState state) throws ReflectiveOperationException {
        Method render = method(loader, "X.0RUD", "A1E", "X.01I3", new String[]{"X.01S3"});
        Constructor<?> empty = Class.forName("X.035j", false, loader).getDeclaredConstructor();
        empty.setAccessible(true);
        if (!render.getReturnType().isAssignableFrom(empty.getDeclaringClass()))
            throw new NoSuchMethodException("Reels banner empty component contract mismatch");
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

    private static void reelsCompletion(ClassLoader loader, String owner, HostState state)
            throws ReflectiveOperationException {
        Method success = method(loader, owner, "A02", "void", new String[]{"X.0ARi"});
        Method noAd = method(loader, owner, "A01", "void", new String[]{});
        Class<?> receiver = Class.forName(owner, false, loader);
        register(success, state, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam param) {
                if (!state.options.ads || !receiver.isInstance(param.thisObject)) return;
                try {
                    // The native no-ad path clears fetch state and completes the request.
                    noAd.invoke(param.thisObject);
                    param.setResult(null);
                    state.videoAdFiltered(VideoAdPolicy.Surface.REELS);
                } catch (ReflectiveOperationException | RuntimeException failed) {
                    state.failure("reels-completion");
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
