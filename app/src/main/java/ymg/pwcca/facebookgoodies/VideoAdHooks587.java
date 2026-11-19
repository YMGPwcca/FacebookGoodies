package ymg.pwcca.facebookgoodies;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/** Ad-only pool, completion and banner hooks for Facebook 587 and 588. */
final class VideoAdHooks587 {
    private static final String[] SELECT = {"X.05z2", "X.03Wk", "java.util.List", "int"};
    private static final String[] SELECT_VALIDATED = {
            "com.facebook.auth.usersession.FbUserSession", "X.03RY", "X.05z2", "X.03Wk",
            "java.util.List", "java.util.List", "int", "boolean"};

    static void install(FacebookHostBindings loader, HostState state) {
        attempt("reels-pool", state, () -> pool(loader, "X.04wh", "A0V", SELECT, null, VideoAdPolicy.Surface.REELS, state));
        attempt("reels-pool-validated", state, () -> pool(loader, "X.03HA", "A0T", SELECT_VALIDATED,
                "X.04wh", VideoAdPolicy.Surface.REELS, state));
        for (String owner : new String[]{"X.06I5", "X.06I4"}) {
            attempt("video-pool", state, () -> pool(loader, owner, "A0V", SELECT, null, VideoAdPolicy.Surface.VIDEO, state));
            attempt("video-pool-validated", state, () -> pool(loader, owner, "A0T", SELECT_VALIDATED,
                    null, VideoAdPolicy.Surface.VIDEO, state));
        }
        VideoAdResponses587.install(loader, state);
        attempt("reels-completion", state, () -> reelsCompletion(loader, "X.0Uk0", state));
        // This sibling used the inherited callback implementation in 585; 586 declares the same
        // callback pair on the concrete class, so restrict the hook to that receiver.
        attempt("reels-completion-inherited", state, () -> reelsCompletion(loader, "X.0Uk1", state));
        VideoAdMetadataHooks587.install(loader, state);
        attempt("reels-banner", state, () -> reelsBanner(loader, state));
        ReelsOverlayHooks587.install(loader, state);
        XposedBridge.log("FacebookGoodies: video hooks=" + state.videoHooks.get());
    }

    private static void pool(FacebookHostBindings loader, String owner, String name, String[] parameters,
                             String receiverType, VideoAdPolicy.Surface surface, HostState state) throws ReflectiveOperationException {
        Method method = method(loader, owner, name, "X.065q", parameters);
        Class<?> receiver = receiverType == null ? null : loader.type(receiverType);
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

    private static void reelsBanner(FacebookHostBindings loader, HostState state) throws ReflectiveOperationException {
        Method render = method(loader, "X.0UaS", "A19", "X.01Kp", new String[]{"X.01Mw"});
        Constructor<?> empty = loader.type("X.02Cy").getDeclaredConstructor();
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

    private static void reelsCompletion(FacebookHostBindings loader, String owner, HostState state)
            throws ReflectiveOperationException {
        Method success = method(loader, owner, "A02", "void", new String[]{"X.0Wcj"});
        Method noAd = method(loader, owner, "A01", "void", new String[]{});
        Class<?> receiver = loader.type(owner);
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

    private static Method method(FacebookHostBindings loader, String owner, String name, String result,
                                 String[] parameterNames) throws ReflectiveOperationException {
        Class<?>[] parameters = new Class<?>[parameterNames.length];
        for (int i = 0; i < parameters.length; i++) parameters[i] = type(loader, parameterNames[i]);
        Method method = loader.method(loader.type(owner), name, parameters);
        if (method.getReturnType() != type(loader, result)) throw new NoSuchMethodException("Video return contract mismatch");
        method.setAccessible(true);
        return method;
    }

    private static Class<?> type(FacebookHostBindings loader, String name) throws ClassNotFoundException {
        if ("void".equals(name)) return void.class;
        if ("int".equals(name)) return int.class;
        if ("boolean".equals(name)) return boolean.class;
        return loader.type(name);
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
