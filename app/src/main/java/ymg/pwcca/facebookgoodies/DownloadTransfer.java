package ymg.pwcca.facebookgoodies;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

final class DownloadTransfer {
    interface Progress {
        void bytes(long count);
        default void connecting() {}
        default void connected(long size) {}
        default void finished() {}
    }
    interface ConnectionFactory { HttpURLConnection open(String url) throws IOException; }
    private final ConnectionFactory connections;
    private final AtomicBoolean cancelled = new AtomicBoolean();
    private final AtomicBoolean stopped = new AtomicBoolean();
    private final java.util.Set<HttpURLConnection> active = java.util.concurrent.ConcurrentHashMap.newKeySet();
    DownloadTransfer() { this(url -> (HttpURLConnection) new URL(url).openConnection()); }
    DownloadTransfer(ConnectionFactory connections) { this.connections = connections; }
    void cancel() { cancelled.set(true); abort(); }
    void abort() { stopped.set(true); for(HttpURLConnection c:active)c.disconnect(); }
    void check() throws IOException { if (stopped.get()) throw new DownloadFailure(cancelled.get()?"Download cancelled":"Media transfer stopped"); }
    boolean cancelled() { return cancelled.get(); }
    String manifest(String url) throws IOException {
        return manifest(url,count->{});
    }
    String manifest(String url,Progress progress) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        copy(new DownloadPlan.Part(url, null), out, progress, 4 * 1024 * 1024);
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
    void track(DownloadPlan.Track track, File destination, Progress progress) throws IOException {
        try (OutputStream out = new FileOutputStream(destination)) {
            for (DownloadPlan.Part part : track.parts) copy(part, out, progress, Long.MAX_VALUE);
        }
        progress.finished();
    }
    private HttpURLConnection open(DownloadPlan.Part part) throws IOException {
        String url = part.url;
        for (int redirects = 0; redirects < 6; redirects++) {
            check(); DownloadPlan.validateUrl(url);
            HttpURLConnection c = connections.open(url);
            active.add(c);
            try {
                check();configure(c,part.range);
                if(!redirect(c.getResponseCode()))return c;
                String location=c.getHeaderField("Location");
                if(location==null)throw new DownloadFailure("Missing media redirect");
                url=DashManifest.resolve(url,location);
            } catch(IOException|RuntimeException failure) { release(c);throw failure; }
            release(c);
        }
        throw new DownloadFailure("Too many media redirects");
    }
    private static void configure(HttpURLConnection c,String range) {
        c.setInstanceFollowRedirects(false);c.setConnectTimeout(20000);c.setReadTimeout(30000);
        c.setRequestProperty("Accept-Encoding","identity");
        if(range!=null)c.setRequestProperty("Range","bytes="+range);
    }
    private void release(HttpURLConnection connection) { active.remove(connection);connection.disconnect(); }
    private static boolean redirect(int status) { return status == 301 || status == 302 || status == 303 || status == 307 || status == 308; }
    private void copy(DownloadPlan.Part part, OutputStream out, Progress progress, long limit) throws IOException {
        HttpURLConnection c = null;
        try {
            progress.connecting();
            c = open(part);
            DownloadResponse.validate(c.getResponseCode(), c.getHeaderField("Content-Range"), part.range);
            long expected = c.getContentLengthLong();
            progress.connected(expected);
            if (expected > limit) throw new DownloadFailure("Video manifest is too large");
            long read;
            try (InputStream in = c.getInputStream()) { read = stream(in, out, progress, limit); }
            if (expected >= 0 && expected != read) throw new DownloadFailure("Incomplete media response");
            DownloadResponse.validateLength(part.range, read);
        } finally {
            if(c!=null)release(c);
        }
    }
    private long stream(InputStream in, OutputStream out, Progress progress, long limit) throws IOException {
        byte[] buffer = new byte[65536];
        long total = 0;
        int count;
        while ((count = in.read(buffer)) != -1) {
            check(); total += count;
            if (total > limit) throw new DownloadFailure("Video manifest is too large");
            out.write(buffer, 0, count); progress.bytes(count);
        }
        check();
        return total;
    }
}
