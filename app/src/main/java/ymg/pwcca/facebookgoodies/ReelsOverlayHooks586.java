package ymg.pwcca.facebookgoodies;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/** Reels ad flags and attachment/card surfaces, without replacing Reel content. */
final class ReelsOverlayHooks586 {
    private static final String SCOPE = "facebookgoodies.reels.options";

    static void install(ClassLoader loader, HostState state) {
        try { launchOptions(loader, state); }
        catch (ReflectiveOperationException | RuntimeException | LinkageError failed) {
            state.failure("reels-launch-options");
        }
        installCard(loader, "X.0dWQ", "X.02cz", "render", true, state);
        installCard(loader, "X.0TLE", "X.01S3", "A1E", false, state);
        installCard(loader, "X.0TLe", "X.01S3", "A1E", false, state);
    }

    private static void launchOptions(ClassLoader loader, HostState state) throws ReflectiveOperationException {
        Class<?> builder = Class.forName("com.google.common.collect.ImmutableMap$Builder", false, loader);
        Method launch = Class.forName("X.096o", false, loader).getDeclaredMethod("A01",
                Class.forName("com.facebook.auth.usersession.FbUserSession", false, loader),
                Class.forName("X.0c7s", false, loader), builder,
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
                // Runs for normal returns and exceptions, including nested option builders.
                options.restore(param.getObjectExtra(SCOPE));
            }
        });
        state.hooks.addAndGet(2);
        state.videoHooks.addAndGet(2);
    }

    private static void installCard(ClassLoader loader, String owner, String context, String method,
                                    boolean affiliate, HostState state) {
        try { card(loader, owner, context, method, affiliate, state); }
        catch (ReflectiveOperationException | RuntimeException | LinkageError failed) {
            state.failure("video-card-" + owner);
        }
    }

    private static void card(ClassLoader loader, String owner, String context, String method,
                             boolean affiliate, HostState state) throws ReflectiveOperationException {
        Method render = Class.forName(owner, false, loader).getDeclaredMethod(method, Class.forName(context, false, loader));
        Constructor<?> empty = Class.forName("X.035j", false, loader).getDeclaredConstructor();
        if (!render.getReturnType().getName().equals("X.01I3")
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
