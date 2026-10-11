package ymg.pwcca.facebookgoodies;

import android.app.Instrumentation;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Collections;

/** Platform-only tests: no new test runtime or network dependencies. */
public final class DownloadInstrumentation extends Instrumentation {
    @Override public void onCreate(Bundle arguments) { super.onCreate(arguments); start(); }
    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            runChecks(); result.putString("stream", "PASS: DASH parse, lossless 1080p mux with audio, silent video, cancellation, atomic storage cleanup\n");
            finish(ActivityResult.OK, result);
        } catch (Throwable failure) {
            result.putString("stream", "FAIL: " + failure.getClass().getSimpleName() + ": " + failure.getMessage() + "\n");
            finish(ActivityResult.FAILED, result);
        }
    }
    private void runChecks() throws Exception {
        Context app = getTargetContext();
        verifyAttachContext(app);
        File video = asset("video.mp4"); File audio = asset("audio.m4a"); File output = new File(app.getCacheDir(), "qa-mux.mp4");
        try {
            String xml = "<MPD xmlns='urn:mpeg:dash:schema:mpd:2011'><Period><AdaptationSet mimeType='video/mp4'>"
                    + "<Representation width='1920' height='1080' bandwidth='100'><BaseURL>https://video.fbcdn.net/qa.mp4</BaseURL><SegmentBase/></Representation>"
                    + "</AdaptationSet></Period></MPD>";
            DownloadPlan parsed = DashManifest.parse(xml, null);
            require(parsed.video.height == 1080, "Android manifest parsing");
            DownloadPlan.Track track = new DownloadPlan.Track(Collections.emptyList(),1920,1080,100);
            DownloadPlan.Track sound = new DownloadPlan.Track(Collections.emptyList(),0,0,128000);
            DownloadPlan plan = new DownloadPlan(track,sound,true);
            DownloadTransfer transfer = new DownloadTransfer();
            DownloadMuxer.mux(video,audio,output,plan,transfer);
            verifyTracks(output,true);
            require(samples(video,"video/").equals(samples(output,"video/")),"Video samples changed");
            require(samples(audio,"audio/").equals(samples(output,"audio/")),"Audio samples changed");
            long originalOffset = firstTime(video,"video/") - firstTime(audio,"audio/");
            long savedOffset = firstTime(output,"video/") - firstTime(output,"audio/");
            require(Math.abs(originalOffset - savedOffset) < 1000,"Audio/video timing changed");
            int before = downloads(app);
            DownloadStorage.publish(app,output,transfer);
            require(downloads(app) == before + 1,"Published download is missing");
            DownloadTransfer cancelled = new DownloadTransfer(); cancelled.cancel();
            try { DownloadStorage.publish(app,output,cancelled); throw new AssertionError("Cancelled publish succeeded"); }
            catch(java.io.IOException expected) { require(downloads(app) == before + 1,"Cancelled publish leaked a row"); }
            try { DownloadMuxer.mux(video,audio,output,plan,cancelled); throw new AssertionError("Cancelled mux succeeded"); }
            catch(java.io.IOException expected) {}
            DownloadMuxer.mux(video,null,output,new DownloadPlan(track,null,false),transfer);
            verifyTracks(output,false);
            verifyVp9Fallback(app,audio,transfer);
        } finally { video.delete(); audio.delete(); output.delete(); removeDownloads(app); }
    }
    private void verifyVp9Fallback(Context app,File audio,DownloadTransfer transfer) throws Exception {
        File video=asset("vp9-unfragmented.mp4");File output=new File(app.getCacheDir(),"qa-vp9.mp4");
        DownloadPlan.Track track=new DownloadPlan.Track(Collections.emptyList(),720,1280,100000);
        HostState state=new HostState();
        org.mp4parser.PropertyBoxParserImpl.BOX_MAP_CACHE=null;
        String phase="native-mux";
        try{
            DownloadMuxer.mux(video,audio,output,new DownloadPlan(track,null,true),transfer,state);
            require(state.muxStage.get()==8,"VP9 fallback did not finish");
            require(state.portableStage.get()==78,"Portable VP9 mux path was not exercised");
            phase="video-samples";
            require(samples(video,"video/").equals(samples(output,"video/")),"VP9 samples changed");
            phase="audio-samples";
            require(samples(audio,"audio/").equals(samples(output,"audio/")),"VP9 audio samples changed");
            require(Math.abs(firstTime(video,"video/")-firstTime(audio,"audio/")-firstTime(output,"video/")+firstTime(output,"audio/"))<1000,"VP9 AV timing changed");
        }catch(Exception failed){throw new AssertionError("VP9 phase="+phase+" stage="+state.muxStage.get()+" codec="+state.videoCodec.get()+" "+failed.getClass().getSimpleName());}
        finally{video.delete();output.delete();}
    }
    private void verifyAttachContext(Context app) throws Exception {
        android.app.Application early = new android.app.Application() {
            @Override public Context getApplicationContext() { return null; }
            @Override public File getCacheDir() { return app.getCacheDir(); }
        };
        VideoDownloader downloader = new VideoDownloader(early);
        java.lang.reflect.Field field = VideoDownloader.class.getDeclaredField("app"); field.setAccessible(true);
        require(field.get(downloader) == early,"Attach-time Application was replaced with null");
        java.lang.reflect.Method create = VideoDownloader.class.getDeclaredMethod("temporaryDirectory"); create.setAccessible(true);
        File workspace = (File) create.invoke(downloader);
        require(workspace.isDirectory(),"Attach-time download workspace failed"); workspace.delete();
    }
    private File asset(String name) throws Exception {
        File file = new File(getTargetContext().getCacheDir(), "qa-" + name);
        try(InputStream in=getContext().getAssets().open(name); FileOutputStream out=new FileOutputStream(file)) {
            byte[] bytes=new byte[65536]; int count; while((count=in.read(bytes))!=-1)out.write(bytes,0,count);
        }
        return file;
    }
    private static void verifyTracks(File file, boolean audio) throws Exception {
        MediaExtractor extractor = new MediaExtractor();
        try(java.io.FileInputStream input=new java.io.FileInputStream(file)) {
            extractor.setDataSource(input.getFD());
            boolean foundVideo=false,foundAudio=false;
            for(int i=0;i<extractor.getTrackCount();i++) {
                MediaFormat format=extractor.getTrackFormat(i); String mime=format.getString(MediaFormat.KEY_MIME);
                if(mime.startsWith("video/")) { require(format.getInteger(MediaFormat.KEY_HEIGHT)==1080,"Wrong resolution"); foundVideo=true; }
                if(mime.startsWith("audio/"))foundAudio=true;
            }
            require(foundVideo && foundAudio==audio,"Missing or unexpected tracks");
        } finally {extractor.release();}
    }
    private static String samples(File file,String prefix) throws Exception {
        MediaExtractor extractor=new MediaExtractor(); MessageDigest digest=MessageDigest.getInstance("SHA-256");
        try(java.io.FileInputStream input=new java.io.FileInputStream(file)) {
            extractor.setDataSource(input.getFD());
            for(int i=0;i<extractor.getTrackCount();i++) if(extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME).startsWith(prefix)){extractor.selectTrack(i);break;}
            ByteBuffer buffer=ByteBuffer.allocate(1024*1024);
            while(extractor.getSampleTrackIndex()>=0){buffer.clear();int count=extractor.readSampleData(buffer,0);require(count>=0,"Missing sample");buffer.limit(count);digest.update(buffer);extractor.advance();}
            byte[] value=digest.digest(); StringBuilder out=new StringBuilder();for(byte b:value)out.append(String.format("%02x",b));return out.toString();
        } finally {extractor.release();}
    }
    private static long firstTime(File file,String prefix) throws Exception {
        MediaExtractor extractor = new MediaExtractor();
        try(java.io.FileInputStream input = new java.io.FileInputStream(file)) {
            extractor.setDataSource(input.getFD());
            for(int i=0;i<extractor.getTrackCount();i++)
                if(extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME).startsWith(prefix)){extractor.selectTrack(i);break;}
            require(extractor.getSampleTrackIndex()>=0,"No first sample");
            return extractor.getSampleTime();
        } finally { extractor.release(); }
    }
    private static int downloads(Context app) {
        try(Cursor c=app.getContentResolver().query(MediaStore.Downloads.EXTERNAL_CONTENT_URI,new String[]{"_id"},"relative_path=?",new String[]{"Download/FacebookGoodies/"},null)){return c==null?0:c.getCount();}
    }
    private static void removeDownloads(Context app) { app.getContentResolver().delete(MediaStore.Downloads.EXTERNAL_CONTENT_URI,"relative_path=?",new String[]{"Download/FacebookGoodies/"}); }
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private static final class ActivityResult { static final int OK=-1; static final int FAILED=0; }
}
