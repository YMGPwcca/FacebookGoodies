package ymg.pwcca.facebookgoodies;

import java.lang.reflect.Method;
import java.util.Arrays;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

final class AdHooks587 {
    static void install(FacebookHostBindings loader, HostState state) throws ReflectiveOperationException {
        FeedClassifier587 classifier = new FeedClassifier587(loader);
        FeedCollections587 collections = new FeedCollections587(loader, classifier, state);
        Class<?> manager = loader.type("X.01th");
        for (Method method : manager.getDeclaredMethods()) installInsertion(method, classifier, state);
        Class<?> tree = loader.type("com.facebook.graphql.modelutil.BaseModelWithTree");
        for (String name : new String[]{"getCachedModelList", "getCachedVirtualModelList", "getTreeModelList", "getVirtualTreeModelList"})
            installLists(tree, name, collections, state);
        FeedCacheHooks587.install(loader, collections, state);
        FeedRenderHooks587.install(loader, classifier, state);
    }
    private static void installInsertion(Method method, FeedClassifier587 classifier, HostState state) {
        if (!"addNewEdgeToCollection".equals(method.getName()) || method.getReturnType() != boolean.class) return;
        if (Arrays.stream(method.getParameterTypes()).noneMatch(t -> t.getName().equals("com.facebook.graphql.model.GraphQLFeedUnitEdge"))) return;
        XposedBridge.hookMethod(method, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam param) {
                Options options = state.options;
                if (!options.hasFeedFilters()) return;
                for (Object value : param.args) {
                    FilterPolicy.Action action = classifier.decide(value, null, options);
                    if (action == FilterPolicy.Action.KEEP) continue;
                    state.removed(action); param.setResult(false); return;
                }
            }
        });
        state.hooks.incrementAndGet();
    }
    private static void installLists(Class<?> tree, String name, FeedCollections587 collections, HostState state) {
        java.util.Set<XC_MethodHook.Unhook> added = XposedBridge.hookAllMethods(tree, name, new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                Options options = state.options;
                if (!options.hasFeedFilters() || param.hasThrowable()) return;
                Object original = param.getResult();
                Object filtered = collections.filter(original, ((Method) param.method).getReturnType(), options, false);
                if (filtered != original) param.setResult(filtered);
            }
        });
        state.hooks.addAndGet(added.size());
    }
}
