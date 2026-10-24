package ymg.pwcca.facebookgoodies;

import java.lang.reflect.Method;
import java.util.List;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/** Remove embedded instream-break nodes from getter results, preserving cached originals. */
final class VideoAdMetadataHooks584 {
    static void install(ClassLoader loader, HostState state) {
        try { getters(loader, state); }
        catch (ReflectiveOperationException | RuntimeException | LinkageError failed) { state.failure("video-metadata-install"); }
    }

    private static void getters(ClassLoader loader, HostState state) throws ReflectiveOperationException {
        Class<?> base = Class.forName("com.facebook.graphql.modelutil.BaseModelWithTree", false, loader);
        Class<?> media = Class.forName("com.facebook.graphql.model.GraphQLMedia", false, loader);
        Class<?> node = Class.forName("X.45C", false, loader);
        Class<?> immutable = Class.forName("com.google.common.collect.ImmutableList", false, loader);
        Object empty = immutable.getDeclaredMethod("of").invoke(null);
        Method list = base.getDeclaredMethod("getCachedModelList", int.class, Class.class, int.class);
        Method scalar = base.getDeclaredMethod("getCachedModel", int.class, Class.class, int.class);
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
