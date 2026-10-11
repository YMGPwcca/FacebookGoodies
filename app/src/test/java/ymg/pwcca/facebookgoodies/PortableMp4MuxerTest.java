package ymg.pwcca.facebookgoodies;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Collections;
import org.junit.Test;
import org.mp4parser.muxer.Movie;
import org.mp4parser.muxer.Track;
import org.mp4parser.muxer.container.mp4.MovieCreator;
import static org.junit.Assert.*;

public class PortableMp4MuxerTest {
    @Test public void loadsMappingFromModuleLoaderBeforeOpeningMedia() throws Exception {
        org.mp4parser.PropertyBoxParserImpl.BOX_MAP_CACHE = null;
        PortableMp4Muxer.ensureMapping();
        assertNotNull(org.mp4parser.PropertyBoxParserImpl.BOX_MAP_CACHE.getProperty("vp09"));
        assertNotNull(org.mp4parser.PropertyBoxParserImpl.BOX_MAP_CACHE.getProperty("mp4a"));
    }
    @Test public void preservesFragmentedVp9AndAacSamplesAtOriginalResolution() throws Exception {
        File video=fixture("vp9.mp4"),audio=fixture("audio.m4a"),out=File.createTempFile("merged-vp9",".mp4");
        DownloadPlan.Track picture=new DownloadPlan.Track(Collections.emptyList(),720,1280,100000);
        try {
            PortableMp4Muxer.mux(video,audio,out,new DownloadPlan(picture,null,true),new DownloadTransfer());
            Movie merged=MovieCreator.build(out.getAbsolutePath());
            try {
                assertEquals(2,merged.getTracks().size());
                assertEquals("vp09",merged.getTracks().get(0).getSampleEntries().get(0).getType());
                assertEquals(720,merged.getTracks().get(0).getTrackMetaData().getWidth(),0);
                assertEquals(1280,merged.getTracks().get(0).getTrackMetaData().getHeight(),0);
                assertEquals(hash(video,"vide"),hash(merged,"vide"));
                assertEquals(hash(audio,"soun"),hash(merged,"soun"));
            } finally { close(merged); }
        } finally {video.delete();audio.delete();out.delete();}
    }
    @Test public void cancellationStopsPortableMerge() throws Exception {
        File video=fixture("vp9.mp4"),out=File.createTempFile("cancel-vp9",".mp4");
        DownloadTransfer transfer=new DownloadTransfer();transfer.cancel();
        try {
            try {PortableMp4Muxer.mux(video,null,out,new DownloadPlan(new DownloadPlan.Track(Collections.emptyList(),720,1280,0),null,true),transfer);fail("must cancel");}
            catch(java.io.IOException expected) {assertTrue(transfer.cancelled());}
        } finally {video.delete();out.delete();}
    }
    private static File fixture(String name) throws Exception {
        File file=File.createTempFile("fixture", ".mp4");
        try(InputStream input=PortableMp4MuxerTest.class.getResourceAsStream("/"+name)){Files.copy(input,file.toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);}
        return file;
    }
    private static String hash(File file,String type) throws Exception {
        Movie movie=MovieCreator.build(file.getAbsolutePath());
        try{return hash(movie,type);}finally{close(movie);}
    }
    private static String hash(Movie movie,String type) throws Exception {
        MessageDigest digest=MessageDigest.getInstance("SHA-256");
        for(Track track:movie.getTracks())if(type.equals(track.getHandler()))for(org.mp4parser.muxer.Sample sample:track.getSamples())digest.update(sample.asByteBuffer());
        StringBuilder text=new StringBuilder();for(byte value:digest.digest())text.append(String.format("%02x",value));return text.toString();
    }
    private static void close(Movie movie) throws Exception {for(Track track:movie.getTracks())track.close();}
}
