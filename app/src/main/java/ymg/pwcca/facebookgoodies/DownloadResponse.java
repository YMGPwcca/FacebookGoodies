package ymg.pwcca.facebookgoodies;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class DownloadResponse {
    private static final Pattern CONTENT_RANGE = Pattern.compile("bytes ([0-9]+)-([0-9]+)/(?:[0-9]+|\\*)");
    static void validate(int status, String contentRange, String requestedRange) throws IOException {
        if (requestedRange == null) {
            if (status != 200) throw new DownloadFailure("Media unavailable (HTTP " + status + ")");
            return;
        }
        if (status != 206 || contentRange == null) throw new DownloadFailure("Server did not honor media byte range");
        Matcher m = CONTENT_RANGE.matcher(contentRange);
        if (!m.matches() || !requestedRange.equals(m.group(1) + "-" + m.group(2)))
            throw new DownloadFailure("Incorrect media byte range");
    }
    static void validateLength(String range, long length) throws IOException {
        if (range == null) return;
        String[] ends = range.split("-");
        try {
            long start = Long.parseLong(ends[0]); long end = Long.parseLong(ends[1]);
            if (end < start || end - start == Long.MAX_VALUE || end - start + 1 != length)
                throw new DownloadFailure("Incomplete media byte range");
        } catch (NumberFormatException invalid) { throw new DownloadFailure("Invalid media byte range"); }
    }
}
