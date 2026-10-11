package ymg.pwcca.facebookgoodies;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

/** Reads only the manifest supplied with the active player, without network discovery. */
final class DashManifest {
    interface SourceProgress {
        void finding(boolean video);
        void found(boolean video,DownloadPlan.Track track);
    }
    static DownloadPlan parse(String xml, String baseUrl) throws IOException {
        return parse(xml,baseUrl,new SourceProgress(){public void finding(boolean video){} public void found(boolean video,DownloadPlan.Track track){}});
    }
    static DownloadPlan parse(String xml,String baseUrl,SourceProgress progress) throws IOException {
        Element root = document(xml);
        if (!"MPD".equals(root.getLocalName())) throw new DownloadFailure("Unsupported video manifest");
        if ("dynamic".equals(root.getAttribute("type"))) throw new DownloadFailure("Live videos cannot be downloaded");
        List<Element> periods = children(root, "Period");
        if (periods.size() != 1) throw new DownloadFailure("Unsupported multi-period video");
        Element period = periods.get(0);
        String base = base(period, base(root, baseUrl));
        List<Candidate> videos = new ArrayList<>();
        List<Candidate> audios = new ArrayList<>();
        for (Element adaptation : children(period, "AdaptationSet"))
            collect(adaptation, base, videos, audios);
        progress.finding(true);Candidate video = best(videos, true);
        if (video == null) throw new DownloadFailure("No downloadable video in manifest");
        DownloadPlan.Track v = video.track();
        progress.found(true,v);progress.finding(false);Candidate audio = best(audios, false);
        DownloadPlan.Track a = audio == null ? null : audio.track();
        progress.found(false,a);
        return new DownloadPlan(v, a, true);
    }
    private static Element document(String xml) throws IOException {
        if (xml == null || xml.length() > 4 * 1024 * 1024) throw new DownloadFailure("Video manifest is too large");
        String upper = xml.toUpperCase(java.util.Locale.ROOT);
        if (upper.contains("<!DOCTYPE") || upper.contains("<!ENTITY")) throw new DownloadFailure("Unsupported XML declaration");
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setExpandEntityReferences(false);
            javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new org.xml.sax.helpers.DefaultHandler() {
                @Override public void fatalError(org.xml.sax.SAXParseException e) throws org.xml.sax.SAXException { throw e; }
                @Override public void error(org.xml.sax.SAXParseException e) throws org.xml.sax.SAXException { throw e; }
            });
            return builder.parse(new InputSource(new StringReader(xml))).getDocumentElement();
        } catch (Exception invalid) { throw new DownloadFailure("Cannot read this video manifest"); }
    }
    private static void collect(Element adaptation, String inheritedBase, List<Candidate> videos, List<Candidate> audios) throws IOException {
        String adaptationBase = base(adaptation, inheritedBase);
        for (Element representation : children(adaptation, "Representation")) {
            Candidate c = new Candidate(adaptation, representation, base(representation, adaptationBase));
            String kind = attr(representation, adaptation, "contentType");
            String mime = attr(representation, adaptation, "mimeType");
            if ("video".equals(kind) || mime.startsWith("video/")) videos.add(c);
            if ("audio".equals(kind) || mime.startsWith("audio/")) audios.add(c);
        }
    }
    private static Candidate best(List<Candidate> candidates, boolean video) {
        Candidate best = null;
        for (Candidate candidate : candidates)
            if (best == null || candidate.betterThan(best, video)) best = candidate;
        return best;
    }
    private static final class Candidate {
        final Element adaptation;
        final Element representation;
        final String url;
        final int width;
        final int height;
        final long bandwidth;
        Candidate(Element a, Element r, String url) throws IOException {
            this.adaptation = a; this.representation = r; this.url = url;
            width = dimension(attr(r, a, "width")); height = dimension(attr(r, a, "height"));
            bandwidth = number(attr(r, a, "bandwidth"), 0);
        }
        boolean betterThan(Candidate other, boolean video) {
            long pixels = (long) width * height;
            long otherPixels = (long) other.width * other.height;
            if (video && pixels != otherPixels) return pixels > otherPixels;
            return bandwidth > other.bandwidth;
        }
        DownloadPlan.Track track() throws IOException {
            if (!children(adaptation, "ContentProtection").isEmpty() || !children(representation, "ContentProtection").isEmpty())
                throw new DownloadFailure("Protected videos cannot be downloaded");
            String mime = attr(representation, adaptation, "mimeType");
            if (!mime.endsWith("/mp4")) throw new DownloadFailure("Highest-quality source uses an unsupported container");
            List<DownloadPlan.Part> parts = DashSegments.parts(representation, adaptation, url);
            return new DownloadPlan.Track(parts, width, height, bandwidth);
        }
    }
    static List<Element> children(Element parent, String name) {
        List<Element> out = new ArrayList<>();
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling())
            if (n instanceof Element && name.equals(n.getLocalName())) out.add((Element) n);
        return out;
    }
    static Element child(Element parent, String name) {
        List<Element> matches = children(parent, name);
        return matches.isEmpty() ? null : matches.get(0);
    }
    static String attr(Element element, Element inherited, String name) {
        String value = element.getAttribute(name);
        return value.isEmpty() ? inherited.getAttribute(name) : value;
    }
    static String base(Element element, String inherited) throws IOException {
        Element base = child(element, "BaseURL");
        return base == null ? inherited : resolve(inherited, base.getTextContent().trim());
    }
    static String resolve(String base, String value) throws IOException {
        try {
            URI uri = URI.create(value);
            if (uri.isAbsolute()) return uri.toString();
            if (!DownloadMedia.present(base)) throw new DownloadFailure("Missing media base URL");
            return URI.create(base).resolve(uri).toString();
        } catch (IllegalArgumentException invalid) { throw new DownloadFailure("Invalid media location"); }
    }
    static long number(String value, long fallback) throws IOException {
        if (value.isEmpty()) return fallback;
        try { return Long.parseLong(value); }
        catch (NumberFormatException invalid) { throw new DownloadFailure("Invalid manifest number"); }
    }
    private static int dimension(String value) throws IOException {
        long result = number(value, 0);
        if (result < 0 || result > 65536) throw new DownloadFailure("Invalid video dimensions");
        return (int) result;
    }
}
