package ymg.pwcca.facebookgoodies;

import java.util.HashMap;
import java.util.Map;

/** Override only advertising flags written into the current Reel's launch options. */
final class ReelsAdOptions {
    private static final Map<String, Boolean> FLAGS = flags();
    private static final ThreadLocal<Object> BUILDER = new ThreadLocal<>();

    private static Map<String, Boolean> flags() {
        Map<String, Boolean> values = new HashMap<>();
        values.put("ReelsIsEligibleForInContentAds", false);
        values.put("ReelsIsEligibleForShoppable", false);
        values.put("ReelsBannerAdSuppressed", true);
        return values;
    }

    Object enter(Object current) {
        Object previous = BUILDER.get();
        BUILDER.set(current);
        return previous;
    }

    void restore(Object previous) {
        if (previous == null) BUILDER.remove(); else BUILDER.set(previous);
    }

    static boolean buildingLaunchOptions() { return BUILDER.get() != null; }

    Object value(Object target, boolean enabled, Object key, Object original) {
        if (!enabled || target == null || target != BUILDER.get()) return original;
        Boolean replacement = FLAGS.get(key);
        return replacement == null ? original : replacement;
    }
}
