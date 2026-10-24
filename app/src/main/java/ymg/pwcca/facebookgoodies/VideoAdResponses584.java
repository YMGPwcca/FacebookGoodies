package ymg.pwcca.facebookgoodies;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/** Dedicated instream ad callbacks; preserve Facebook's native empty-result completion. */
final class VideoAdResponses584 {
    private static final String ORIGIN = "com.facebook.video.common.playerorigin.PlayerOrigin";

    static void install(ClassLoader loader, HostState state) {
        install(loader, "X.SYF", new String[]{"A09"}, new String[]{ORIGIN}, state);
        install(loader, "X.V18", new String[]{"A08"}, new String[]{ORIGIN}, state);
        install(loader, "X.BqA", new String[]{"A08", "A04", "A01"},
                new String[]{"X.7wk", "X.7wY", ORIGIN}, state);
    }

    private static void install(ClassLoader loader, String name, String[] names, String[] types, HostState state) {
        try {
            Class<?> owner = Class.forName(name, false, loader);
            Field[] path = originPath(loader, owner, names, types);
            Class<?> origin = Class.forName(ORIGIN, false, loader);
            Field primary = field(origin, "A00", String.class);
            Field secondary = field(origin, "A01", String.class);
            Map<String, Integer> sourceCodes = sourceCodes(loader);
            Method success = owner.getDeclaredMethod("onSuccess", Object.class);
            if (success.getReturnType() != void.class) throw new NoSuchMethodException("Ad callback return mismatch");
            success.setAccessible(true);
            XposedBridge.hookMethod(success, new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) {
                    if (!state.options.ads || param.args[0] == null) return;
                    state.videoResponses.incrementAndGet();
                    filter(param, path, primary, secondary, sourceCodes, state);
                }
            });
            state.hooks.incrementAndGet();
            state.videoHooks.incrementAndGet();
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failed) {
            state.failure("video-response-install-" + name);
        }
    }

    private static void filter(XC_MethodHook.MethodHookParam param, Field[] path, Field primary, Field secondary,
                               Map<String, Integer> sourceCodes, HostState state) {
        try {
            Object origin = readOrigin(param.thisObject, path);
            String source = origin == null ? null : (String) primary.get(origin);
            String subsource = origin == null ? null : (String) secondary.get(origin);
            // Only a shipped enum's numeric code is reported; never a URL, video ID or free-form value.
            state.videoSourceCode.set(sourceCodes.getOrDefault(source, 0));
            VideoAdPolicy.Surface surface = VideoAdPolicy.surface(true, source, subsource);
            if (surface == VideoAdPolicy.Surface.NONE) {
                state.videoUnmatched.incrementAndGet();
                return;
            }
            // These callbacks explicitly accept null. Let them clear fetch state and restore playback.
            param.args[0] = null;
            state.videoAdFiltered(surface);
        } catch (ReflectiveOperationException | RuntimeException failed) {
            state.failure("video-response-origin");
        }
    }

    private static Map<String, Integer> sourceCodes(ClassLoader loader) throws ReflectiveOperationException {
        Object[] sources = Class.forName("X.2iV", false, loader).getEnumConstants();
        if (sources == null) throw new NoSuchFieldException("Player source enum mismatch");
        Map<String, Integer> codes = new HashMap<>();
        for (Object source : sources) codes.put(source.toString(), ((Enum<?>) source).ordinal() + 1);
        return codes;
    }

    private static Object readOrigin(Object value, Field[] path) throws IllegalAccessException {
        for (Field field : path) {
            if (value == null) return null;
            value = field.get(value);
        }
        return value;
    }

    private static Field[] originPath(ClassLoader loader, Class<?> owner, String[] names, String[] types) throws ReflectiveOperationException {
        Field[] path = new Field[names.length];
        for (int i = 0; i < names.length; i++) {
            Class<?> next = Class.forName(types[i], false, loader);
            path[i] = field(owner, names[i], next);
            owner = next;
        }
        return path;
    }

    private static Field field(Class<?> owner, String name, Class<?> type) throws NoSuchFieldException {
        Field field = owner.getDeclaredField(name);
        if (field.getType() != type) throw new NoSuchFieldException("Ad origin field mismatch");
        field.setAccessible(true);
        return field;
    }
}
