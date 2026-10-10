package ymg.pwcca.facebookgoodies;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/** Verified ad-only contracts for Facebook 582.0.0.50.54 / 475417129. */
final class VideoAdHooks {
    private static final String[] SELECT = {"X.6aY", "X.3d5", "java.util.List", "int"};
    private static final String[] SELECT_VALIDATED = {
            "com.facebook.auth.usersession.FbUserSession", "X.6aX", "X.6aY", "X.3d5",
            "java.util.List", "java.util.List", "int", "boolean"};

    static void install(ClassLoader loader, HostState state) {
        attempt("reels-pool", state, () -> pool(loader, "X.5J5", "A0N", SELECT, null, VideoAdPolicy.Surface.REELS, state));
        attempt("reels-pool-validated", state, () -> pool(loader, "X.6Ce", "A0L", SELECT_VALIDATED, "X.5J5", VideoAdPolicy.Surface.REELS, state));
        for (String owner : new String[]{"X.6HQ", "X.6HP"}) {
            attempt("video-pool", state, () -> pool(loader, owner, "A0N", SELECT, null, VideoAdPolicy.Surface.VIDEO, state));
            attempt("video-pool-validated", state, () -> pool(loader, owner, "A0L", SELECT_VALIDATED, null, VideoAdPolicy.Surface.VIDEO, state));
        }
        for (String owner : new String[]{"X.T85", "X.T86"})
            attempt("reels-completion", state, () -> reelsCompletion(loader, owner, state));
        attempt("video-completion", state, () -> videoCompletion(loader, state));
        attempt("reels-banner", state, () -> reelsBanner(loader, state));
        XposedBridge.log("FacebookGoodies: video hooks=" + state.videoHooks.get());
    }

    private static void pool(ClassLoader loader, String owner, String name, String[] parameters,
                             String receiverType, VideoAdPolicy.Surface surface, HostState state) throws ReflectiveOperationException {
        Method method = method(loader, owner, name, "X.5j9", parameters);
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
        Method success = method(loader, owner, "A02", "void", new String[]{"X.74K"});
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

    private static void videoCompletion(ClassLoader loader, HostState state) throws ReflectiveOperationException {
        Class<?> callback = Class.forName("X.Sx0", false, loader);
        Class<?> origin = Class.forName("com.facebook.video.common.playerorigin.PlayerOrigin", false, loader);
        Field playerOrigin = callback.getDeclaredField("A09");
        Field originName = origin.getDeclaredField("A00");
        if (playerOrigin.getType() != origin || originName.getType() != String.class)
            throw new NoSuchFieldException("Video origin contract mismatch");
        playerOrigin.setAccessible(true);
        originName.setAccessible(true);
        Method success = method(loader, "X.Sx0", "onSuccess", "void", new String[]{"java.lang.Object"});
        // The original callback explicitly accepts null and runs its empty-result completion path.
        // Never cancel its future or bypass its callback; host-video restoration must still run.
        register(success, state, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam param) {
                if (!state.options.ads || param.args[0] == null) return;
                try {
                    Object source = playerOrigin.get(param.thisObject);
                    String name = source == null ? null : (String) originName.get(source);
                    VideoAdPolicy.Surface surface = VideoAdPolicy.surface(true, name);
                    if (surface == VideoAdPolicy.Surface.NONE) return;
                    param.args[0] = null;
                    state.videoAdFiltered(surface);
                } catch (ReflectiveOperationException | RuntimeException failed) {
                    state.failure("video-completion");
                }
            }
        });
    }

    private static void reelsBanner(ClassLoader loader, HostState state) throws ReflectiveOperationException {
        Method render = method(loader, "X.Sqm", "A18", "X.1HX", new String[]{"X.1UN"});
        Constructor<?> empty = Class.forName("X.2AO", false, loader).getDeclaredConstructor();
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
