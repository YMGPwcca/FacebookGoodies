package ymg.pwcca.facebookgoodies;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Tracks local-only values written to host summaries during this process. */
final class MessengerLocalReadValues {
    private final ConcurrentHashMap<String, Set<Long>> values = new ConcurrentHashMap<>();

    void record(String thread, long value) {
        values.computeIfAbsent(thread, ignored -> ConcurrentHashMap.newKeySet()).add(value);
    }

    boolean contains(String thread, long value) {
        Set<Long> local = values.get(thread);
        return local != null && local.contains(value);
    }

    void discardThrough(String thread, long confirmed) {
        Set<Long> local = values.get(thread);
        if (local != null) local.removeIf(value -> value <= confirmed);
    }
}
