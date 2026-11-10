package ymg.pwcca.facebookgoodies;

final class MessengerSeenBorderPolicy {
    private MessengerSeenBorderPolicy() { }

    static boolean show(boolean enabled, boolean incoming, long messageTime,
                        long confirmedReadTime) {
        return enabled && incoming && messageTime > 0 && messageTime > confirmedReadTime;
    }

    static long initialRead(long observedReadTime, long localReadTime, long previousInitial) {
        if (previousInitial > 0 || observedReadTime <= 0) return previousInitial;
        if (localReadTime > 0 && observedReadTime >= localReadTime) return previousInitial;
        return observedReadTime;
    }

    static long syncedRead(long observed, long confirmed, boolean liveStream, boolean localValue) {
        if (observed <= confirmed || observed <= 0 || !liveStream && localValue) return confirmed;
        return observed;
    }

    static long sentWatermark(Long explicit, Long pendingDerived, Long latestObserved) {
        if (explicit != null && explicit > 0) return explicit;
        return Math.max(pendingDerived == null ? 0 : pendingDerived,
                latestObserved == null ? 0 : latestObserved);
    }
}
