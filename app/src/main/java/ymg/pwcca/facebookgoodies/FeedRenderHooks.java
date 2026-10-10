package ymg.pwcca.facebookgoodies;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/** Replaces only matching News Feed rows with Facebook's own empty component. */
final class FeedRenderHooks {
    static void install(ClassLoader loader, FeedClassifier classifier, HostState state) {
        try { installBindings(loader, classifier, state); }
        catch (ReflectiveOperationException | RuntimeException | LinkageError failed) { state.failure("feed-render"); }
    }
    private static void installBindings(ClassLoader loader, FeedClassifier classifier, HostState state) throws ReflectiveOperationException {
        Class<?> component = Class.forName("X.2Zl", false, loader);
        Class<?> wrapper = Class.forName("X.2Zp", false, loader);
        Class<?> context = Class.forName("X.1UN", false, loader);
        Field primary = field(component, "A05");
        Field child = field(component, "A06");
        Field wrapped = field(wrapper, "A03");
        Constructor<?> empty = Class.forName("X.2AO", false, loader).getDeclaredConstructor();
        empty.setAccessible(true);
        hook(component.getDeclaredMethod("A18", context), component, primary, child, null, empty, classifier, state);
        hook(wrapper.getDeclaredMethod("A18", context), component, primary, child, wrapped, empty, classifier, state);
    }
    private static Field field(Class<?> owner, String name) throws NoSuchFieldException {
        Field field = owner.getDeclaredField(name); field.setAccessible(true); return field;
    }
    private static void hook(Method method, Class<?> component, Field primary, Field child, Field wrapped, Constructor<?> empty, FeedClassifier classifier, HostState state) throws NoSuchMethodException {
        if (!method.getReturnType().isAssignableFrom(empty.getDeclaringClass())) throw new NoSuchMethodException("Layout result mismatch");
        XposedBridge.hookMethod(method, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                state.renderCalls.incrementAndGet();
                Options options = state.options;
                if (!options.ads && !options.suggestions) return;
                Object row = wrapped == null ? param.thisObject : wrapped.get(param.thisObject);
                if (!component.isInstance(row)) return;
                FilterPolicy.Action action = classifier.decide(primary.get(row), child.get(row), options);
                if (action == FilterPolicy.Action.KEEP) return;
                param.setResult(empty.newInstance());
                state.removed(action); state.renderRemoved.incrementAndGet();
            }
        });
        state.hooks.incrementAndGet();
    }
}
