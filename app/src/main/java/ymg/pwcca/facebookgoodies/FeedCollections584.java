package ymg.pwcca.facebookgoodies;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** Rebuilds Facebook's immutable collections without editing cached originals. */
final class FeedCollections584 {
    private final Method copyOf;
    private final FeedClassifier584 classifier;
    private final HostState state;
    FeedCollections584(ClassLoader loader, FeedClassifier584 classifier, HostState state) throws ReflectiveOperationException {
        copyOf = Class.forName("com.google.common.collect.ImmutableList", false, loader).getMethod("copyOf", Iterable.class);
        this.classifier = classifier; this.state = state;
    }
    Object filter(Object original, Class<?> expected, Options options, boolean cache) {
        if (!(original instanceof List)) return original;
        List<?> source = (List<?>) original;
        List<Object> kept = new ArrayList<>(source.size());
        List<FilterPolicy.Action> actions = partition(source, kept, options);
        if (actions.isEmpty()) return original;
        return rebuild(original, expected, kept, actions, cache);
    }
    private List<FilterPolicy.Action> partition(List<?> source, List<Object> kept, Options options) {
        List<FilterPolicy.Action> actions = new ArrayList<>();
        for (Object item : source) {
            FilterPolicy.Action action = classifier.decide(item, null, options);
            if (action == FilterPolicy.Action.KEEP) kept.add(item); else actions.add(action);
        }
        return actions;
    }
    private Object rebuild(Object original, Class<?> expected, List<Object> kept, List<FilterPolicy.Action> actions, boolean cache) {
        try {
            Object replacement = copyOf.invoke(null, kept);
            if (!expected.isInstance(replacement)) return original;
            for (FilterPolicy.Action action : actions) state.removed(action);
            if (cache) state.cacheRemoved.addAndGet(actions.size());
            return replacement;
        } catch (ReflectiveOperationException | RuntimeException failed) {
            state.failure("feed-list-rebuild"); return original;
        }
    }
}
