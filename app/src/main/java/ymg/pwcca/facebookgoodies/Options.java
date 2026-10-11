package ymg.pwcca.facebookgoodies;

import android.os.Bundle;

final class Options {
    static final Options OFF = new Options(false, false, false, false);
    final boolean ads;
    final boolean suggestions;
    final boolean receipts;
    final boolean capture;
    final boolean downloads;
    final ContentFilters content;
    Options(boolean ads, boolean suggestions, boolean receipts, boolean capture) {
        this(ads, suggestions, receipts, capture, ContentFilters.OFF);
    }
    Options(boolean ads, boolean suggestions, boolean receipts, boolean capture, ContentFilters content) {
        this(ads, suggestions, receipts, capture, content, false);
    }
    Options(boolean ads, boolean suggestions, boolean receipts, boolean capture, ContentFilters content, boolean downloads) {
        this.ads = ads; this.suggestions = suggestions; this.receipts = receipts; this.capture = capture;
        this.content = content;
        this.downloads = downloads;
    }
    boolean hasFeedFilters() { return ads || suggestions || content.hasFeedRules(); }
    static Options from(Bundle values) {
        if (values == null) return OFF;
        ContentFilters content = new ContentFilters(values.getBoolean("hide_threads"),
                values.getBoolean("hide_stories_top"), values.getBoolean("hide_stories_middle"), values.getBoolean("hide_reels"));
        return new Options(values.getBoolean("ads"), values.getBoolean("suggestions"), values.getBoolean("receipts"), values.getBoolean("capture"), content, values.getBoolean("downloads"));
    }
}
