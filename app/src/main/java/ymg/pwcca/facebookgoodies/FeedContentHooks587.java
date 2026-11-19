package ymg.pwcca.facebookgoodies;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/** Independent top-tray and inline-Story controls for the supported Facebook build. */
final class FeedContentHooks587 {
    static void install(FacebookHostBindings loader, HostState state) {
        topTray(loader, "A00", "X.01r0", state);
        topTray(loader, "A01", "X.04gF", state);
        if (loader.is588()) storiesComponent(loader, "X.02us", true, state);
        storiesComponent(loader, "X.0Biy", false, state);
        ThreadsVideoHooks587.install(loader, state);
    }

    private static void topTray(FacebookHostBindings loader, String name, String resultName, HostState state) {
        try {
            Class<?> owner = loader.type("X.02fU");
            Class<?> builder = loader.type("com.google.common.collect.ImmutableList$Builder");
            Method method = loader.method(owner, name, owner, builder);
            if (!Modifier.isStatic(method.getModifiers()) || method.getReturnType() != loader.type(resultName))
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

    private static void storiesComponent(FacebookHostBindings loader, String ownerName, boolean top, HostState state) {
        String point = top ? "top-stories-render" : "inline-stories-render";
        try {
            Class<?> owner = loader.type(ownerName);
            Method render = loader.method(owner, "A19", loader.type("X.01Mw"));
            Constructor<?> empty = loader.type("X.02Cy").getDeclaredConstructor();
            if (!render.getReturnType().getName().equals(loader.typeName("X.01Kp")) || !render.getReturnType().isAssignableFrom(empty.getDeclaringClass()))
                throw new NoSuchMethodException("Stories render contract mismatch");
            // The 588 top renderer also serves other tabs. A null feed defaults to News Feed.
            Field feed = top ? owner.getDeclaredField("A00") : null;
            Object newsFeed = top ? loader.type("com.facebook.api.feedtype.FeedType").getField("A0h").get(null) : null;
            if (feed != null) {
                if (!feed.getType().isInstance(newsFeed)) throw new NoSuchFieldException("Top Stories feed type");
                feed.setAccessible(true);
            }
            render.setAccessible(true);
            empty.setAccessible(true);
            XposedBridge.hookMethod(render, new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) {
                    if (top ? !state.options.content.storiesTop : !state.options.content.storiesMiddle) return;
                    try {
                        Object feedType = feed == null ? null : feed.get(param.thisObject);
                        if (feedType != null && !newsFeed.equals(feedType)) return;
                        param.setResult(empty.newInstance());
                        state.removed(top ? FilterPolicy.Action.STORIES_TOP : FilterPolicy.Action.STORIES_MIDDLE);
                    } catch (ReflectiveOperationException | RuntimeException failed) {
                        state.failure(point);
                    }
                }
            });
            registered(state);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failed) {
            state.failure(point + "-install");
        }
    }

    private static void registered(HostState state) {
        state.hooks.incrementAndGet();
        state.contentHooks.incrementAndGet();
    }
}
