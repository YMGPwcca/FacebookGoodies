package ymg.pwcca.facebookgoodies;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/** Reels ad flags and attachment/card surfaces, without replacing Reel content. */
final class ReelsOverlayHooks587 {
    private static final String SCOPE = "facebookgoodies.reels.options";

    static void install(FacebookHostBindings loader, HostState state) {
        try { launchOptions(loader, state); }
        catch (ReflectiveOperationException | RuntimeException | LinkageError failed) {
            state.failure("reels-launch-options");
        }
        installCard(loader, "X.0dCf", "X.02FH", "render", true, state);
        installCard(loader, "X.108j", "X.01Mw", "A19", false, state);
        installCard(loader, "X.0VDC", "X.01Mw", "A19", false, state);
    }

    private static void launchOptions(FacebookHostBindings loader, HostState state) throws ReflectiveOperationException {
        Class<?> builder = loader.type("com.google.common.collect.ImmutableMap$Builder");
        Method launch = loader.method(loader.type("X.08re"), "A01",
                loader.type("com.facebook.auth.usersession.FbUserSession"),
                loader.type("X.0A5B"), builder,
                loader.type("com.google.common.collect.ImmutableMap"),
                Boolean.class, Boolean.class, Boolean.class, String.class, String.class, int.class);
        Method put = loader.method(builder, "put", Object.class, Object.class);
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
                // Runs for normal returns and exceptions, including nested option builders.
                options.restore(param.getObjectExtra(SCOPE));
            }
        });
        state.hooks.addAndGet(2);
        state.videoHooks.addAndGet(2);
    }

    private static void installCard(FacebookHostBindings loader, String owner, String context, String method,
                                    boolean affiliate, HostState state) {
        try { card(loader, owner, context, method, affiliate, state); }
        catch (ReflectiveOperationException | RuntimeException | LinkageError failed) {
            state.failure("video-card-" + owner);
        }
    }

    private static void card(FacebookHostBindings loader, String owner, String context, String method,
                             boolean affiliate, HostState state) throws ReflectiveOperationException {
        Method render = loader.method(loader.type(owner), method, loader.type(context));
        Constructor<?> empty = loader.type("X.02Cy").getDeclaredConstructor();
        if (!render.getReturnType().getName().equals(loader.typeName("X.01Kp"))
                || !render.getReturnType().isAssignableFrom(empty.getDeclaringClass()))
            throw new NoSuchMethodException("Reels attachment render contract mismatch");
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
