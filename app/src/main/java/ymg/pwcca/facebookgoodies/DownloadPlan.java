package ymg.pwcca.facebookgoodies;

import java.io.IOException;
import java.net.URI;
import java.util.List;

final class DownloadPlan {
    static final class Part {
        final String url;
        final String range;
        Part(String url, String range) throws IOException {
            validateUrl(url);
            if (range != null && !range.matches("[0-9]+-[0-9]+")) throw new DownloadFailure("Unsupported byte range");
            this.url = url; this.range = range;
        }
    }
    static final class Track {
        final List<Part> parts;
        final int width;
        final int height;
        final long bandwidth;
        Track(List<Part> parts, int width, int height, long bandwidth) {
            this.parts = java.util.Collections.unmodifiableList(new java.util.ArrayList<>(parts));
            this.width = width; this.height = height; this.bandwidth = bandwidth;
        }
        long pixels() { return (long) width * height; }
    }
    final Track video;
    final Track audio;
    final boolean adaptive;
    DownloadPlan(Track video, Track audio, boolean adaptive) {
        this.video = video; this.audio = audio; this.adaptive = adaptive;
    }
    static DownloadPlan progressive(String url) throws IOException {
        return new DownloadPlan(new Track(java.util.Collections.singletonList(new Part(url, null)), 0, 0, 0), null, false);
    }
    static void validateUrl(String value) throws IOException {
        try {
            URI uri = URI.create(value);
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null || uri.getUserInfo() != null)
                throw new DownloadFailure("Unsupported media URL");
            if (!allowedHost(host.toLowerCase(java.util.Locale.ROOT))) throw new DownloadFailure("Unsupported media host");
        } catch (IllegalArgumentException invalid) { throw new DownloadFailure("Invalid media URL"); }
    }
    private static boolean allowedHost(String host) {
        for (String domain : new String[]{"fbcdn.net", "fbsbx.com", "facebook.com", "akamaihd.net", "cdninstagram.com"})
            if (host.equals(domain) || host.endsWith("." + domain)) return true;
        return false;
    }
}
