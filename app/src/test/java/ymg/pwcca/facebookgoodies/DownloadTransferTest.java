package ymg.pwcca.facebookgoodies;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

public class DownloadTransferTest {
    private static final String URL = "https://video.fbcdn.net/movie.mp4";
    private static final class Response extends HttpURLConnection {
        final byte[] body;
        final int status;
        final long length;
        String range;
        String location;
        boolean disconnected;
        Response(int status, String body, long length) throws Exception { super(new URL(URL)); this.status=status; this.body=body.getBytes("UTF-8"); this.length=length; }
        @Override public int getResponseCode() { return status; }
        @Override public long getContentLengthLong() { return length; }
        @Override public String getHeaderField(String name) { return "Content-Range".equals(name) ? range : "Location".equals(name) ? location : null; }
        @Override public InputStream getInputStream() { return new ByteArrayInputStream(body); }
        @Override public void disconnect() { disconnected=true; }
        @Override public boolean usingProxy() { return false; }
        @Override public void connect() {}
    }
    @Test public void copiesWholeFileAndClosesConnection() throws Exception {
        Response response = new Response(200, "complete", 8);
        File output = File.createTempFile("download", ".mp4");
        try {
            new DownloadTransfer(url -> response).track(DownloadPlan.progressive(URL).video, output, count -> {});
            assertEquals("complete", new String(Files.readAllBytes(output.toPath()), "UTF-8")); assertTrue(response.disconnected);
            assertNull(response.getRequestProperty("Cookie")); assertNull(response.getRequestProperty("Range"));
        } finally { output.delete(); }
    }
    @Test(expected=IOException.class) public void rejectsShortResponse() throws Exception {
        Response response = new Response(200, "short", 100);
        new DownloadTransfer(url -> response).manifest(URL);
    }
    @Test(expected=IOException.class) public void rejectsUnsolicitedPartialResponse() throws Exception {
        new DownloadTransfer(url -> newResponse(206)).manifest(URL);
    }
    private static Response newResponse(int status) throws IOException {
        try { return new Response(status, "body", 4); } catch (Exception invalid) { throw new IOException(invalid); }
    }
    @Test public void appendsOnlyValidatedRequestedSegments() throws Exception {
        Response first = new Response(206, "abc", 3); first.range="bytes 0-2/6";
        Response second = new Response(206, "def", 3); second.range="bytes 3-5/6";
        int[] index={0}; Response[] responses={first,second};
        DownloadPlan.Track track = new DownloadPlan.Track(Arrays.asList(new DownloadPlan.Part(URL,"0-2"),new DownloadPlan.Part(URL,"3-5")), 1,1,0);
        File output=File.createTempFile("download", ".mp4");
        try {
            new DownloadTransfer(url -> responses[index[0]++]).track(track, output, count -> {});
            assertEquals("abcdef", new String(Files.readAllBytes(output.toPath()), "UTF-8"));
            assertEquals("bytes=3-5",second.getRequestProperty("Range"));
        } finally { output.delete(); }
    }
    @Test(expected=IOException.class) public void rejectsIgnoredRange() throws Exception { DownloadResponse.validate(200, null, "0-2"); }
    @Test(expected=IOException.class) public void rejectsWrongRange() throws Exception { DownloadResponse.validate(206, "bytes 3-5/6", "0-2"); }
    @Test(expected=IOException.class) public void rejectsWrongRangeLength() throws Exception { DownloadResponse.validateLength("0-2", 2); }
    @Test public void cancelsBeforeOpeningAnything() throws Exception {
        DownloadTransfer transfer = new DownloadTransfer(url -> { fail("must not connect"); return null; }); transfer.cancel();
        try { transfer.manifest(URL); fail("must cancel"); } catch(IOException expected) { assertTrue(transfer.cancelled()); }
    }
    @Test public void cancelDuringCopyDisconnectsAndStops() throws Exception {
        Response response = new Response(200, "abc", 3); DownloadTransfer transfer = new DownloadTransfer(url -> response);
        File output=File.createTempFile("download", ".mp4");
        try {
            try { transfer.track(DownloadPlan.progressive(URL).video, output, count -> transfer.cancel()); fail("must cancel"); }
            catch(IOException expected) { assertTrue(response.disconnected); }
        } finally { output.delete(); }
    }
    @Test public void validatesRedirectBeforeOpeningSecondHost() throws Exception {
        Response response = new Response(302, "", 0); response.location="https://attacker.example/media";
        int[] calls={0};
        try { new DownloadTransfer(url -> {calls[0]++; return response;}).manifest(URL); fail("must reject host"); }
        catch(IOException expected) { assertEquals(1,calls[0]); assertTrue(response.disconnected); }
    }
    @Test(expected=IOException.class) public void rejectsHttpsDowngrade() throws Exception {
        Response response = new Response(302, "", 0); response.location="http://video.fbcdn.net/media";
        new DownloadTransfer(url -> response).manifest(URL);
    }
}
