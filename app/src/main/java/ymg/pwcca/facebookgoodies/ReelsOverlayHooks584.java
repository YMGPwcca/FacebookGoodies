package ymg.pwcca.facebookgoodies;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/** Ad flags and affiliate attachment surfaces, without replacing the Reel's content. */
final class ReelsOverlayHooks584 {
    private static final String SCOPE = "facebookgoodies.reels.options";

    static void install(ClassLoader loader, HostState state) {
        try { launchOptions(loader, state); }
        catch (ReflectiveOperationException | RuntimeException | LinkageError failed) { state.failure("reels-launch-options"); }
        installCard(loader, "X.Tad", "X.28P", "render", true, state);
        installCard(loader, "X.Th1", "X.3tD", "A0l", false, state);
        installCard(loader, "X.ThR", "X.3tD", "A0l", false, state);
    }

    private static void launchOptions(ClassLoader loader, HostState state) throws ReflectiveOperationException {
        Class<?> builder = Class.forName("com.google.common.collect.ImmutableMap$Builder", false, loader);
        Method launch = Class.forName("X.8gf", false, loader).getDeclaredMethod("A01",
                Class.forName("com.facebook.auth.usersession.FbUserSession", false, loader),
                Class.forName("X.ST1", false, loader), builder,
                Class.forName("com.google.common.collect.ImmutableMap", false, loader),
                Boolean.class, Boolean.class, Boolean.class, String.class, String.class, int.class);
        Method put = builder.getDeclaredMethod("put", Object.class, Object.class);
        if (launch.getReturnType() != void.class || !Modifier.isStatic(launch.getModifiers()) || put.getReturnType() != builder)
            throw new NoSuchMethodException("Reels launch options contract mismatch");
        launch.setAccessible(true);
        put.setAccessible(true);
        ReelsAdOptions options = new ReelsAdOptions();
        XposedBridge.hookMethod(put, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam param) {
                Object replacement = options.value(param.thisObject, state.options.ads, param.args[0], param.args[1]);
                if (replacement == param.args[1]) return;
                param.args[1] = replacement;
                state.reelsLaunchFlags.incrementAndGet();
            }
        });
        XposedBridge.hookMethod(launch, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam param) {
                param.setObjectExtra(SCOPE, options.enter(param.args[2]));
            }
            @Override protected void afterHookedMethod(MethodHookParam param) {
                // Runs for both normal returns and exceptions, including nested option builders.
                options.restore(param.getObjectExtra(SCOPE));
            }
        });
        state.hooks.addAndGet(2);
        state.videoHooks.addAndGet(2);
    }

    private static void installCard(ClassLoader loader, String owner, String context, String method, boolean affiliate, HostState state) {
        try { card(loader, owner, context, method, affiliate, state); }
        catch (ReflectiveOperationException | RuntimeException | LinkageError failed) { state.failure("video-card-" + owner); }
    }

    private static void card(ClassLoader loader, String owner, String context, String method, boolean affiliate, HostState state) throws ReflectiveOperationException {
        Method render = Class.forName(owner, false, loader).getDeclaredMethod(method, Class.forName(context, false, loader));
        Constructor<?> empty = Class.forName("X.26x", false, loader).getDeclaredConstructor();
        if (!render.getReturnType().getName().equals("X.3SE") || !render.getReturnType().isAssignableFrom(empty.getDeclaringClass()))
            throw new NoSuchMethodException("Affiliate attachment render contract mismatch");
        render.setAccessible(true);
        empty.setAccessible(true);
        XposedBridge.hookMethod(render, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam param) {
                if (!state.options.ads) return;
                try {
                    param.setResult(empty.newInstance());
                    if (affiliate) state.affiliateCards.incrementAndGet(); else state.videoCards.incrementAndGet();
                    state.videoAdFiltered(affiliate ? VideoAdPolicy.Surface.REELS : VideoAdPolicy.Surface.VIDEO);
                } catch (ReflectiveOperationException | RuntimeException failed) {
                    state.failure("video-card-render-" + owner);
                }
            }
        });
        state.hooks.incrementAndGet();
        state.videoHooks.incrementAndGet();
    }
}
