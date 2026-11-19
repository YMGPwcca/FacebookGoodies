package ymg.pwcca.facebookgoodies;

import java.lang.reflect.Method;
import java.util.List;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/** Remove embedded instream-break nodes from getter results, preserving cached originals. */
final class VideoAdMetadataHooks587 {
    static void install(FacebookHostBindings loader, HostState state) {
        try { getters(loader, state); }
        catch (ReflectiveOperationException | RuntimeException | LinkageError failed) {
            state.failure("video-metadata-install");
        }
    }

    private static void getters(FacebookHostBindings loader, HostState state) throws ReflectiveOperationException {
        Class<?> base = loader.type("com.facebook.graphql.modelutil.BaseModelWithTree");
        Class<?> media = loader.type("com.facebook.graphql.model.GraphQLMedia");
        Class<?> node = loader.type("X.04CT");
        Class<?> immutable = loader.type("com.google.common.collect.ImmutableList");
        Object empty = loader.method(immutable, "of").invoke(null);
        Method list = loader.method(base, "getCachedModelList", int.class, Class.class, int.class);
        Method scalar = loader.method(base, "getCachedModel", int.class, Class.class, int.class);
        if (list.getReturnType() != immutable || scalar.getReturnType() != base)
            throw new NoSuchMethodException("Video metadata getter contract mismatch");
        hook(list, media, node, empty, true, state);
        hook(scalar, media, node, null, false, state);
    }

    private static void hook(Method method, Class<?> media, Class<?> node, Object empty, boolean list, HostState state) {
        method.setAccessible(true);
        XposedBridge.hookMethod(method, new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                if (!state.options.ads || param.hasThrowable()) return;
                if (!media.isInstance(param.thisObject) || param.args[1] != node) return;
                if (!matches((Integer) param.args[0], (Integer) param.args[2], list)) return;
                // Preserve the native "contains breaks" flag while launch props are computed.
                // Clearing that flag together with its nodes would trigger a fallback fetch.
                if (ReelsAdOptions.buildingLaunchOptions()) return;
                if (!hasMetadata(param.getResult(), list)) return;
                param.setResult(empty);
                state.videoMetadata.incrementAndGet();
                state.videoAdFiltered(VideoAdPolicy.Surface.VIDEO);
            }
        });
        state.hooks.incrementAndGet();
        state.videoHooks.incrementAndGet();
    }

    private static boolean matches(int field, int type, boolean list) {
        return list ? VideoAdMetadataPolicy.list(field, type) : VideoAdMetadataPolicy.scalar(field, type);
    }

    private static boolean hasMetadata(Object result, boolean list) {
        if (result == null) return false;
        return !list || (result instanceof List && !((List<?>) result).isEmpty());
    }
}
