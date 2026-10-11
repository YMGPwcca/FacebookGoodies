package ymg.pwcca.facebookgoodies;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.mp4parser.Container;
import org.mp4parser.PropertyBoxParserImpl;
import org.mp4parser.boxes.sampleentry.SampleEntry;
import org.mp4parser.boxes.sampleentry.VisualSampleEntry;
import org.mp4parser.muxer.Movie;
import org.mp4parser.muxer.Track;
import org.mp4parser.muxer.builder.DefaultMp4Builder;
import org.mp4parser.muxer.container.mp4.MovieCreator;

/** Preserve MP4 sample entries and encoded samples when Android cannot write VP9 in MP4. */
final class PortableMp4Muxer {
    static void mux(File video, File audio, File output, DownloadPlan plan, DownloadTransfer transfer) throws IOException {
        mux(video,audio,output,plan,transfer,new HostState());
    }
    static void mux(File video, File audio, File output, DownloadPlan plan, DownloadTransfer transfer, HostState state) throws IOException {
        List<Track> opened = new ArrayList<>();
        try {
            state.portableError.set(0); state.portableStage.set(70); ensureMapping();
            state.portableStage.set(71);
            transfer.check();
            Movie input = MovieCreator.build(video.getAbsolutePath()); opened.addAll(input.getTracks());
            Movie merged = new Movie(); merged.setMatrix(input.getMatrix());
            state.portableStage.set(72); Track picture = find(input, "vide"); validate(picture, plan.video); merged.addTrack(picture);
            state.portableStage.set(73);
            Movie sound = sound(input,audio,opened);
            state.portableStage.set(74); addSound(merged,sound,audio!=null);
            transfer.check();
            state.portableStage.set(75); Container container = new DefaultMp4Builder().build(merged);
            state.portableStage.set(76);
            try (FileOutputStream file = new FileOutputStream(output)) {
                container.writeContainer(new CancellableChannel(file.getChannel(), transfer));
            }
            transfer.check();
            state.portableStage.set(78);
        } catch (RuntimeException invalid) {
            state.portableError.set(invalid instanceof IllegalArgumentException?1:invalid instanceof IllegalStateException?2:invalid instanceof NullPointerException?3:invalid instanceof IndexOutOfBoundsException?6:invalid instanceof ArithmeticException?7:9);
            throw new DownloadFailure("Could not merge this MP4 video",invalid);
        }
        finally { close(opened); }
    }
    private static Movie sound(Movie video,File audio,List<Track> opened) throws IOException {
        if(audio==null)return video;
        Movie sound=MovieCreator.build(audio.getAbsolutePath());opened.addAll(sound.getTracks());return sound;
    }
    private static void addSound(Movie merged,Movie sound,boolean required) throws IOException {
        Track soundtrack=optional(sound,"soun");
        if(soundtrack==null){if(required)throw new DownloadFailure("Downloaded file has no audio track");return;}
        validate(soundtrack,null);merged.addTrack(soundtrack);
    }
    static synchronized void ensureMapping() throws IOException {
        if (PropertyBoxParserImpl.BOX_MAP_CACHE != null) return;
        ClassLoader loader = PortableMp4Muxer.class.getClassLoader();
        InputStream input = loader.getResourceAsStream("assets/mp4-box-map.properties");
        if (input == null) input = loader.getResourceAsStream("isoparser2-default.properties");
        if (input == null) throw new DownloadFailure("The module's MP4 format mapping is unavailable");
        Properties mapping = new Properties();
        try (InputStream resource = input) { mapping.load(resource); }
        PropertyBoxParserImpl.BOX_MAP_CACHE = mapping;
    }
    private static void close(List<Track> tracks) {
        for (Track track : tracks) { try { track.close(); } catch (IOException ignored) {} }
    }
    private static Track find(Movie movie, String kind) throws IOException {
        Track track = optional(movie, kind);
        if (track == null) throw new DownloadFailure("Downloaded file has no video track");
        return track;
    }
    private static Track optional(Movie movie, String kind) {
        for (Track track : movie.getTracks()) if (kind.equals(track.getHandler())) return track;
        return null;
    }
    private static void validate(Track track, DownloadPlan.Track expected) throws IOException {
        if (track.getSamples().isEmpty()) throw new DownloadFailure("Downloaded media track is empty");
        for (SampleEntry entry : track.getSampleEntries()) {
            if ("encv".equals(entry.getType()) || "enca".equals(entry.getType())) throw new DownloadFailure("Protected videos cannot be downloaded");
            if (expected != null && entry instanceof VisualSampleEntry) {
                VisualSampleEntry video = (VisualSampleEntry) entry;
                if ((long) video.getWidth() * video.getHeight() < expected.pixels())
                    throw new DownloadFailure("Downloaded video is below the selected resolution");
            }
        }
    }
    private static final class CancellableChannel implements WritableByteChannel {
        final WritableByteChannel target;
        final DownloadTransfer transfer;
        CancellableChannel(WritableByteChannel target, DownloadTransfer transfer) { this.target=target; this.transfer=transfer; }
        @Override public int write(ByteBuffer source) throws IOException {
            int total = 0;
            while (source.hasRemaining()) {
                transfer.check();
                int originalLimit=source.limit(); source.limit(Math.min(originalLimit,source.position()+262144));
                try { total += target.write(source); } finally { source.limit(originalLimit); }
            }
            return total;
        }
        @Override public boolean isOpen() { return target.isOpen(); }
        @Override public void close() throws IOException { target.close(); }
    }
}
