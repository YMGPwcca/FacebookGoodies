package ymg.pwcca.facebookgoodies;

import java.io.IOException;
import org.junit.Test;
import static org.junit.Assert.*;

public class DashManifestTest {
    private static String mpd(String body) { return "<MPD xmlns='urn:mpeg:dash:schema:mpd:2011' type='static'><Period>" + body + "</Period></MPD>"; }
    private static String video(String id, int w, int h, long bitrate) {
        return "<Representation id='" + id + "' width='" + w + "' height='" + h + "' bandwidth='" + bitrate + "'>"
                + "<BaseURL>https://video.fbcdn.net/" + id + ".mp4?token=x&amp;valid=1</BaseURL><SegmentBase indexRange='100-200'/></Representation>";
    }
    private static String videos(String body) { return "<AdaptationSet mimeType='video/mp4'>" + body + "</AdaptationSet>"; }
    @Test public void picksResolutionBeforeBitrateAndKeepsSignedUrl() throws Exception {
        DownloadPlan p = DashManifest.parse(mpd(videos(video("720", 1280, 720, 9000000) + video("2160", 3840, 2160, 2000000))), null);
        assertEquals(2160, p.video.height); assertEquals(3840, p.video.width);
        assertEquals("https://video.fbcdn.net/2160.mp4?token=x&valid=1", p.video.parts.get(0).url);
        assertNull(p.video.parts.get(0).range); assertNull(p.audio);
    }
    @Test public void bitrateBreaksEqualResolutionTieAcrossCodecs() throws Exception {
        DownloadPlan p = DashManifest.parse(mpd(videos(video("low", 1920, 1080, 2000000) + video("high", 1920, 1080, 4000000))), null);
        assertEquals(4000000, p.video.bandwidth);
    }
    @Test public void picksHighestBandwidthAudioAndDoesNotDownloadOtherVariants() throws Exception {
        String audio = "<AdaptationSet mimeType='audio/mp4'><Representation bandwidth='64000'><BaseURL>https://video.fbcdn.net/low.m4a</BaseURL></Representation>"
                + "<Representation bandwidth='128000'><BaseURL>https://video.fbcdn.net/high.m4a</BaseURL></Representation></AdaptationSet>";
        DownloadPlan p = DashManifest.parse(mpd(videos(video("v", 1920, 1080, 2000000)) + audio), null);
        assertEquals(128000, p.audio.bandwidth); assertEquals(1, p.video.parts.size()); assertEquals(1, p.audio.parts.size());
    }
    @Test public void resolvesHierarchicalBaseAndSegmentList() throws Exception {
        String xml = "<MPD xmlns='urn:mpeg:dash:schema:mpd:2011'><BaseURL>https://video.fbcdn.net/</BaseURL><Period><BaseURL>movie/</BaseURL>"
                + "<AdaptationSet mimeType='video/mp4'><Representation width='1920' height='1080'><SegmentList>"
                + "<Initialization sourceURL='init.mp4'/><SegmentURL media='first.m4s'/><SegmentURL media='second.m4s'/></SegmentList>"
                + "</Representation></AdaptationSet></Period></MPD>";
        DownloadPlan p = DashManifest.parse(xml, null);
        assertEquals(3, p.video.parts.size()); assertEquals("https://video.fbcdn.net/movie/second.m4s", p.video.parts.get(2).url);
    }
    @Test public void expandsFiniteTimelineAndEscapedDollar() throws Exception {
        String body = "<AdaptationSet mimeType='video/mp4'><SegmentTemplate initialization='$RepresentationID$-init.mp4' media='$RepresentationID$-$Number$-$Time$-$$.m4s' startNumber='7'>"
                + "<SegmentTimeline><S t='10' d='5' r='1'/><S d='5'/></SegmentTimeline></SegmentTemplate><Representation id='v' width='1920' height='1080'/></AdaptationSet>";
        DownloadPlan p = DashManifest.parse(mpd(body), "https://video.fbcdn.net/movie/manifest.mpd");
        assertEquals(4, p.video.parts.size()); assertEquals("https://video.fbcdn.net/movie/v-9-20-$.m4s", p.video.parts.get(3).url);
    }
    @Test(expected=IOException.class) public void rejectsLiveManifest() throws Exception {
        DashManifest.parse(mpd(videos(video("v", 1920, 1080, 1))).replace("type='static'", "type='dynamic'"), null);
    }
    @Test(expected=IOException.class) public void rejectsDrmWithoutFallingBackToLowerResolution() throws Exception {
        String highest = video("high", 3840, 2160, 2).replace("<SegmentBase", "<ContentProtection schemeIdUri='urn:uuid:test'/><SegmentBase");
        DashManifest.parse(mpd(videos(video("low", 1280, 720, 1) + highest)), null);
    }
    @Test(expected=IOException.class) public void rejectsUnsupportedHighestContainer() throws Exception {
        String high = "<AdaptationSet mimeType='video/webm'>" + video("high", 3840, 2160, 2) + "</AdaptationSet>";
        DashManifest.parse(mpd(videos(video("low", 1280, 720, 1)) + high), null);
    }
    @Test(expected=IOException.class) public void rejectsEntityDeclarations() throws Exception {
        DashManifest.parse("<!DOCTYPE MPD [<!ENTITY x SYSTEM 'file:///private'>]>" + mpd(videos(video("v", 1, 1, 1))), null);
    }
    @Test(expected=IOException.class) public void rejectsNegativeRepeat() throws Exception {
        String body = "<AdaptationSet mimeType='video/mp4'><SegmentTemplate initialization='init.mp4' media='$Number$.m4s'><SegmentTimeline><S d='5' r='-1'/></SegmentTimeline>"
                + "</SegmentTemplate><Representation width='1920' height='1080'/></AdaptationSet>";
        DashManifest.parse(mpd(body), "https://video.fbcdn.net/manifest.mpd");
    }
    @Test(expected=IOException.class) public void rejectsMultiplePeriods() throws Exception {
        DashManifest.parse(mpd(videos(video("v", 1, 1, 1))).replace("</MPD>", "<Period/></MPD>"), null);
    }
    @Test(expected=IOException.class) public void rejectsNonMediaHost() throws Exception {
        DashManifest.parse(mpd(videos(video("v", 1, 1, 1))).replace("video.fbcdn.net", "fbcdn.net.attacker.example"), null);
    }
    @Test(expected=IOException.class) public void rejectsMalformedManifest() throws Exception { DashManifest.parse("<MPD>", null); }
}
