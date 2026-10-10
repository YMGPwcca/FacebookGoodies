package ymg.pwcca.facebookgoodies;

/** Optional feed-content controls, independent from ads and privacy settings. */
final class ContentFilters {
    static final ContentFilters OFF = new ContentFilters(false, false, false, false);
    final boolean threads;
    final boolean storiesTop;
    final boolean storiesMiddle;
    final boolean reels;

    ContentFilters(boolean threads, boolean storiesTop, boolean storiesMiddle, boolean reels) {
        this.threads = threads;
        this.storiesTop = storiesTop;
        this.storiesMiddle = storiesMiddle;
        this.reels = reels;
    }

    boolean hasFeedRules() { return threads || storiesMiddle || reels; }
}
