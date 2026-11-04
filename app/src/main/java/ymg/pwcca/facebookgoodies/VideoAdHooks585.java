package ymg.pwcca.facebookgoodies;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/** Ad-only contracts for Facebook 585.0.0.47.54 / 476015694. */
final class VideoAdHooks585 {
    private static final String[] SELECT = {"X.05vI", "X.03WL", "java.util.List", "int"};
    private static final String[] SELECT_VALIDATED = {
            "com.facebook.auth.usersession.FbUserSession", "X.07UP", "X.05vI", "X.03WL",
            "java.util.List", "java.util.List", "int", "boolean"};

    static void install(ClassLoader loader, HostState state) {
        attempt("reels-pool", state, () -> pool(loader, "X.05Yg", "A0X", SELECT, null, VideoAdPolicy.Surface.REELS, state));
        attempt("reels-pool-validated", state, () -> pool(loader, "X.064A", "A0V", SELECT_VALIDATED, "X.05Yg", VideoAdPolicy.Surface.REELS, state));
        for (String owner : new String[]{"X.06EV", "X.06EU"}) {
            attempt("video-pool", state, () -> pool(loader, owner, "A0X", SELECT, null, VideoAdPolicy.Surface.VIDEO, state));
            attempt("video-pool-validated", state, () -> pool(loader, owner, "A0V", SELECT_VALIDATED, null, VideoAdPolicy.Surface.VIDEO, state));
        }
        VideoAdResponses585.install(loader, state);
        attempt("reels-completion", state, () -> reelsCompletion(loader, "X.0AG7", "X.0AG7", state));
        attempt("reels-completion-inherited", state, () -> reelsCompletion(loader, "X.0ANd", "X.0AG6", state));
        VideoAdMetadataHooks585.install(loader, state);
        attempt("reels-banner", state, () -> reelsBanner(loader, state));
        ReelsOverlayHooks585.install(loader, state);
        XposedBridge.log("FacebookGoodies: video hooks=" + state.videoHooks.get());
    }

    private static void pool(ClassLoader loader, String owner, String name, String[] parameters,
                             String receiverType, VideoAdPolicy.Surface surface, HostState state) throws ReflectiveOperationException {
        Method method = method(loader, owner, name, "X.062O", parameters);
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
        Method render = method(loader, "X.0LeF", "A17", "X.01JC", new String[]{"X.01PR"});
        Constructor<?> empty = Class.forName("X.02G9", false, loader).getDeclaredConstructor();
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
    private static void reelsCompletion(ClassLoader loader, String owner, String receiverName, HostState state)
            throws ReflectiveOperationException {
        Method success = method(loader, owner, "A03", "void", new String[]{"X.0AYr"});
        Method noAd = method(loader, owner, "A02", "void", new String[]{});
        Class<?> receiver = Class.forName(receiverName, false, loader);
        register(success, state, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam param) {
                if (!state.options.ads || !receiver.isInstance(param.thisObject)) return;
                try {
                    // The host's no-ad path clears fetch state and completes the request.
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
