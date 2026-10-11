package ymg.pwcca.facebookgoodies;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/** Copies encoded samples into MP4; quality is never changed by transcoding. */
final class DownloadMuxer {
    static void mux(File video, File audio, File output, DownloadPlan plan, DownloadTransfer transfer) throws IOException {
        mux(video, audio, output, plan, transfer, new HostState());
    }
    static void mux(File video, File audio, File output, DownloadPlan plan, DownloadTransfer transfer, HostState state) throws IOException {
        try { nativeMux(video, audio, output, plan, transfer, state); }
        catch (DownloadFailure failed) {
            if (state.videoCodec.get() != 4 || state.muxStage.get() != 24) throw failed;
            state.muxStage.set(7); PortableMp4Muxer.mux(video, audio, output, plan, transfer, state);
            state.muxStage.set(8); state.muxError.set(0);
        }
    }
    private static void nativeMux(File video, File audio, File output, DownloadPlan plan, DownloadTransfer transfer, HostState state) throws IOException {
        List<Source> sources = new ArrayList<>();
        MediaMuxer muxer = null;
        boolean started = false;
        state.muxError.set(0); state.videoCodec.set(0); state.audioCodec.set(0);
        try {
            state.muxStage.set(1);
            muxer = new MediaMuxer(output.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            add(sources, video, "video/", muxer, plan.video, state);
            if (audio != null) add(sources, audio, "audio/", muxer, null, state);
            else addOptionalAudio(sources, video, muxer, state);
            state.muxStage.set(4);
            muxer.start(); started = true;
            state.muxStage.set(5);
            copy(sources, muxer, transfer);
            state.muxStage.set(6);
            muxer.stop(); started = false;
            state.muxStage.set(8);
        } catch (RuntimeException failed) {
            state.muxError.set(failed instanceof IllegalArgumentException ? 1 : failed instanceof IllegalStateException ? 2 : failed instanceof NullPointerException ? 3 : failed instanceof ClassCastException ? 5 : 9);
            state.lastMuxFailureStage.set(state.muxStage.get()); state.lastMuxFailureError.set(state.muxError.get());
            state.lastMuxFailureVideoCodec.set(state.videoCodec.get()); state.lastMuxFailureAudioCodec.set(state.audioCodec.get());
            throw new DownloadFailure("Cannot save this video format on this Android version");
        }
        finally { release(muxer, started, sources); }
    }
    private static void release(MediaMuxer muxer, boolean started, List<Source> sources) {
        if (muxer != null) {
            if (started) { try { muxer.stop(); } catch (RuntimeException ignored) {} }
            try { muxer.release(); } catch (RuntimeException ignored) {}
        }
        for (Source source : sources) source.extractor.release();
    }
    private static void add(List<Source> sources, File file, String kind, MediaMuxer muxer, DownloadPlan.Track expected, HostState state) throws IOException {
        MediaExtractor extractor = new MediaExtractor();
        boolean retained = false;
        try {
            int stage = "video/".equals(kind) ? 20 : 30;
            state.muxStage.set(stage + 1);
            open(extractor, file);
            int index = track(extractor, kind);
            if (index < 0) throw new DownloadFailure("Downloaded file has no " + kind.replace("/", "") + " track");
            MediaFormat format = extractor.getTrackFormat(index);
            recordFormat(format, kind, state);
            state.muxStage.set(stage + 2);
            validate(format, expected);
            state.muxStage.set(stage + 3);
            if ("video/".equals(kind) && format.containsKey(MediaFormat.KEY_ROTATION))
                muxer.setOrientationHint(format.getInteger(MediaFormat.KEY_ROTATION));
            state.muxStage.set(stage + 4); int output = muxer.addTrack(format);
            state.muxStage.set(stage + 5);
            extractor.selectTrack(index);
            sources.add(new Source(extractor, output)); retained = true;
        } finally { if (!retained) extractor.release(); }
    }
    private static void addOptionalAudio(List<Source> sources, File file, MediaMuxer muxer, HostState state) throws IOException {
        MediaExtractor probe = new MediaExtractor();
        boolean hasAudio;
        try { open(probe, file); hasAudio = track(probe, "audio/") >= 0; }
        finally { probe.release(); }
        if (hasAudio) add(sources, file, "audio/", muxer, null, state);
    }
    private static void recordFormat(MediaFormat format, String kind, HostState state) {
        String mime = format.getString(MediaFormat.KEY_MIME);
        if ("video/".equals(kind)) {
            state.videoWidth.set(format.getInteger(MediaFormat.KEY_WIDTH)); state.videoHeight.set(format.getInteger(MediaFormat.KEY_HEIGHT));
            state.videoCodec.set("video/avc".equals(mime) ? 1 : "video/hevc".equals(mime) ? 2 : "video/av01".equals(mime) ? 3 : "video/x-vnd.on2.vp9".equals(mime) ? 4 : 9);
        } else state.audioCodec.set("audio/mp4a-latm".equals(mime) ? 1 : "audio/opus".equals(mime) ? 2 : 9);
    }
    private static int track(MediaExtractor extractor, String prefix) {
        for (int i = 0; i < extractor.getTrackCount(); i++) {
            String mime = extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME);
            if (mime != null && mime.startsWith(prefix)) return i;
        }
        return -1;
    }
    private static void open(MediaExtractor extractor, File file) throws IOException {
        try (java.io.FileInputStream input = new java.io.FileInputStream(file)) { extractor.setDataSource(input.getFD()); }
    }
    private static void validate(MediaFormat format, DownloadPlan.Track expected) throws IOException {
        if (expected == null || expected.pixels() == 0) return;
        long pixels = (long) format.getInteger(MediaFormat.KEY_WIDTH) * format.getInteger(MediaFormat.KEY_HEIGHT);
        if (pixels < expected.pixels()) throw new DownloadFailure("Downloaded video is below the selected resolution");
    }
    private static void copy(List<Source> sources, MediaMuxer muxer, DownloadTransfer transfer) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocateDirect(1024 * 1024);
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        long timestampOffset = timestampOffset(sources);
        Source source;
        while ((source = next(sources)) != null) {
            transfer.check();
            long size = source.extractor.getSampleSize();
            if (size > 64 * 1024 * 1024) throw new DownloadFailure("Video sample is too large");
            if (size > buffer.capacity()) buffer = ByteBuffer.allocateDirect((int) size);
            buffer.clear();
            int count = source.extractor.readSampleData(buffer, 0);
            if (count < 0) throw new DownloadFailure("Incomplete video sample");
            int flags = source.extractor.getSampleFlags();
            if ((flags & (MediaExtractor.SAMPLE_FLAG_ENCRYPTED | MediaExtractor.SAMPLE_FLAG_PARTIAL_FRAME)) != 0)
                throw new DownloadFailure("Unsupported protected or partial video sample");
            info.set(0, count, source.extractor.getSampleTime() + timestampOffset,
                    (flags & MediaExtractor.SAMPLE_FLAG_SYNC) != 0 ? MediaCodec.BUFFER_FLAG_KEY_FRAME : 0);
            muxer.writeSampleData(source.output, buffer, info);
            source.samples++;
            source.extractor.advance();
        }
        for (Source track : sources) if (track.samples == 0) throw new DownloadFailure("Downloaded media track is empty");
    }
    private static Source next(List<Source> sources) {
        Source next = null;
        for (Source source : sources) {
            long time = source.extractor.getSampleTime();
            if (source.extractor.getSampleTrackIndex() >= 0 && (next == null || time < next.extractor.getSampleTime())) next = source;
        }
        return next;
    }
    private static long timestampOffset(List<Source> sources) {
        long first = 0;
        for (Source source : sources)
            if (source.extractor.getSampleTrackIndex() >= 0) first = Math.min(first, source.extractor.getSampleTime());
        return -first;
    }
    private static final class Source {
        final MediaExtractor extractor;
        final int output;
        long samples;
        Source(MediaExtractor extractor, int output) { this.extractor = extractor; this.output = output; }
    }
}
