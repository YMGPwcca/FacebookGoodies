package ymg.pwcca.facebookgoodies;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/** Replaces only matching News Feed rows with Facebook's own empty component. */
final class FeedRenderHooks587 {
    static void install(FacebookHostBindings loader, FeedClassifier587 classifier, HostState state) {
        try { installBindings(loader, classifier, state); }
        catch (ReflectiveOperationException | RuntimeException | LinkageError failed) { state.failure("feed-render"); }
    }
    private static void installBindings(FacebookHostBindings loader, FeedClassifier587 classifier, HostState state) throws ReflectiveOperationException {
        Class<?> component = loader.type("X.02cj");
        Class<?> wrapper = loader.type("X.02cx");
        Class<?> context = loader.type("X.01Mw");
        Field primary = field(loader, component, "A05");
        Field child = field(loader, component, "A06");
        Field wrapped = field(loader, wrapper, "A03");
        Constructor<?> empty = loader.type("X.02Cy").getDeclaredConstructor();
        empty.setAccessible(true);
        hook(loader.method(component, "A19", context), component, primary, child, null, empty, classifier, state);
        hook(loader.method(wrapper, "A19", context), component, primary, child, wrapped, empty, classifier, state);
    }
    private static Field field(FacebookHostBindings loader, Class<?> owner, String name) throws NoSuchFieldException {
        Field field = loader.field(owner, name); field.setAccessible(true); return field;
    }
    private static void hook(Method method, Class<?> component, Field primary, Field child, Field wrapped, Constructor<?> empty, FeedClassifier587 classifier, HostState state) throws NoSuchMethodException {
        if (!method.getReturnType().isAssignableFrom(empty.getDeclaringClass())) throw new NoSuchMethodException("Layout result mismatch");
        XposedBridge.hookMethod(method, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                state.renderCalls.incrementAndGet();
                Options options = state.options;
                if (!options.hasFeedFilters()) return;
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
