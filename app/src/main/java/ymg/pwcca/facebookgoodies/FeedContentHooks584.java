package ymg.pwcca.facebookgoodies;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/** Independent top-tray and inline-Story controls for the supported Facebook build. */
final class FeedContentHooks584 {
    static void install(ClassLoader loader, HostState state) {
        topTray(loader, "A00", "X.1tV", state);
        topTray(loader, "A01", "X.4h6", state);
        inlineStories(loader, state);
        ThreadsVideoHooks584.install(loader,state);
    }

    private static void topTray(ClassLoader loader, String name, String resultName, HostState state) {
        try {
            Class<?> owner = Class.forName("X.2eY", false, loader);
            Class<?> builder = Class.forName("com.google.common.collect.ImmutableList$Builder", false, loader);
            Method method = owner.getDeclaredMethod(name, owner, builder);
            if (!Modifier.isStatic(method.getModifiers()) || method.getReturnType() != Class.forName(resultName, false, loader))
                throw new NoSuchMethodException("Top Stories adapter contract mismatch");
            method.setAccessible(true);
            XposedBridge.hookMethod(method, new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) {
                    if (!state.options.content.storiesTop) return;
                    // Both builders already return null when the top Stories adapter is disabled.
                    param.setResult(null);
                    state.removed(FilterPolicy.Action.STORIES_TOP);
                }
            });
            registered(state);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failed) {
            state.failure("top-stories-" + name);
        }
    }

    private static void inlineStories(ClassLoader loader, HostState state) {
        try {
            Class<?> owner = Class.forName("X.B5E", false, loader);
            Method render = owner.getDeclaredMethod("A0l", Class.forName("X.3tD", false, loader));
            Constructor<?> empty = Class.forName("X.26x", false, loader).getDeclaredConstructor();
            if (!render.getReturnType().getName().equals("X.3SE") || !render.getReturnType().isAssignableFrom(empty.getDeclaringClass()))
                throw new NoSuchMethodException("Inline Stories render contract mismatch");
            render.setAccessible(true);
            empty.setAccessible(true);
            XposedBridge.hookMethod(render, new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) {
                    if (!state.options.content.storiesMiddle) return;
                    try {
                        param.setResult(empty.newInstance());
                        state.removed(FilterPolicy.Action.STORIES_MIDDLE);
                    } catch (ReflectiveOperationException | RuntimeException failed) {
                        state.failure("inline-stories-render");
                    }
                }
            });
            registered(state);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failed) {
            state.failure("inline-stories-install");
        }
    }

    private static void registered(HostState state) {
        state.hooks.incrementAndGet();
        state.contentHooks.incrementAndGet();
    }
}
