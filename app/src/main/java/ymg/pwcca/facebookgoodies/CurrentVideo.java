package ymg.pwcca.facebookgoodies;

import java.lang.ref.WeakReference;

/** Exactly one player and its current snapshot, with no previous-video cache. */
final class CurrentVideo {
    private WeakReference<Object> owner = new WeakReference<>(null);
    private DownloadMedia media;
    synchronized void replace(Object player, DownloadMedia value) {
        owner = new WeakReference<>(player);
        media = value;
    }
    synchronized DownloadMedia snapshot(Object player) {
        if (player == null || owner.get() != player) return null;
        return media;
    }
    synchronized Object player() {
        Object player = owner.get();
        if (player == null) clear();
        return player;
    }
    synchronized void clear() { owner.clear(); media = null; }
}
