package ymg.pwcca.facebookgoodies;

import android.os.Bundle;

final class Options {
    static final Options OFF = new Options(false, false, false, false);
    final boolean ads;
    final boolean suggestions;
    final boolean receipts;
    final boolean capture;
    Options(boolean ads, boolean suggestions, boolean receipts, boolean capture) {
        this.ads = ads; this.suggestions = suggestions; this.receipts = receipts; this.capture = capture;
    }
    static Options from(Bundle values) {
        if (values == null) return OFF;
        return new Options(values.getBoolean("ads"), values.getBoolean("suggestions"), values.getBoolean("receipts"), values.getBoolean("capture"));
    }
}
