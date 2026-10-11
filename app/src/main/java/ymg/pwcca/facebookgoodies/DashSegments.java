package ymg.pwcca.facebookgoodies;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.w3c.dom.Element;

final class DashSegments {
    private static final int MAX_PARTS = 20000;
    static List<DownloadPlan.Part> parts(Element representation, Element adaptation, String base) throws IOException {
        Element list = inherited(representation, adaptation, "SegmentList");
        if (list != null) return segmentList(list, base);
        Element template = inherited(representation, adaptation, "SegmentTemplate");
        if (template != null) return template(template, representation, base);
        // Facebook SegmentBase represents a complete MP4; fetch it without its playback byte ranges.
        return Collections.singletonList(new DownloadPlan.Part(base, null));
    }
    private static Element inherited(Element r, Element a, String name) {
        Element child = DashManifest.child(r, name);
        return child == null ? DashManifest.child(a, name) : child;
    }
    private static List<DownloadPlan.Part> segmentList(Element list, String base) throws IOException {
        List<DownloadPlan.Part> parts = new ArrayList<>();
        Element init = DashManifest.child(list, "Initialization");
        if (init == null) throw new DownloadFailure("Missing video initialization");
        add(parts, DashManifest.resolve(base, init.getAttribute("sourceURL")), optional(init.getAttribute("range")));
        for (Element segment : DashManifest.children(list, "SegmentURL"))
            add(parts, DashManifest.resolve(base, segment.getAttribute("media")), optional(segment.getAttribute("mediaRange")));
        if (parts.size() < 2) throw new DownloadFailure("Missing video segments");
        return parts;
    }
    private static List<DownloadPlan.Part> template(Element template, Element representation, String base) throws IOException {
        Element timeline = DashManifest.child(template, "SegmentTimeline");
        if (timeline == null) throw new DownloadFailure("Unsupported video segment template");
        List<DownloadPlan.Part> parts = new ArrayList<>();
        String id = representation.getAttribute("id");
        String bandwidth = representation.getAttribute("bandwidth");
        String initialization = template.getAttribute("initialization");
        if (initialization.isEmpty()) throw new DownloadFailure("Missing video initialization");
        long number = DashManifest.number(template.getAttribute("startNumber"), 1);
        add(parts, DashManifest.resolve(base, expand(initialization, id, bandwidth, number, 0)), null);
        long time = 0;
        for (Element s : DashManifest.children(timeline, "S")) {
            time = DashManifest.number(s.getAttribute("t"), time);
            long duration = DashManifest.number(s.getAttribute("d"), 0);
            long repeat = DashManifest.number(s.getAttribute("r"), 0);
            if (duration <= 0 || repeat < 0 || repeat >= MAX_PARTS) throw new DownloadFailure("Unsupported video timeline");
            for (long i = 0; i <= repeat; i++) {
                String media = expand(template.getAttribute("media"), id, bandwidth, number++, time);
                add(parts, DashManifest.resolve(base, media), null);
                if (time > Long.MAX_VALUE - duration) throw new DownloadFailure("Invalid video timeline");
                time += duration;
            }
        }
        if (parts.size() < 2) throw new DownloadFailure("Missing video segments");
        return parts;
    }
    private static String expand(String pattern, String id, String bandwidth, long number, long time) throws IOException {
        String value = pattern.replace("$$", "\u0001").replace("$RepresentationID$", id).replace("$Bandwidth$", bandwidth)
                .replace("$Number$", Long.toString(number)).replace("$Time$", Long.toString(time));
        if (value.isEmpty() || value.contains("$")) throw new DownloadFailure("Unsupported video segment format");
        return value.replace("\u0001", "$");
    }
    private static void add(List<DownloadPlan.Part> parts, String url, String range) throws IOException {
        if (parts.size() >= MAX_PARTS) throw new DownloadFailure("Video has too many segments");
        parts.add(new DownloadPlan.Part(url, range));
    }
    private static String optional(String value) { return value.isEmpty() ? null : value; }
}
