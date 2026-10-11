package ymg.pwcca.facebookgoodies;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class DownloadProgressTest {
    @Test public void preservesEarlierStagesAndShowsSeparateSourceSearches(){
        long[] time={0};List<String> updates=new ArrayList<>();DownloadProgress progress=new DownloadProgress(updates::add,()->time[0]);
        progress.begin(DownloadProgress.Stage.VIDEO_URL,"Finding highest-resolution video URL…");time[0]=250;
        progress.complete(DownloadProgress.Stage.VIDEO_URL,"Found · 1080 × 1920");
        progress.begin(DownloadProgress.Stage.AUDIO_URL,"Finding audio URL…");
        assertTrue(progress.render().contains("Video URL: Found"));assertTrue(progress.render().contains("Audio URL: Finding"));
        assertTrue(updates.get(0).contains("Finding highest-resolution"));
    }
    @Test public void connectionWaitContinuesToShowElapsedTime(){
        long[] time={0};DownloadProgress progress=new DownloadProgress(x->{},()->time[0]);
        progress.transfer(DownloadProgress.Stage.VIDEO,1);time[0]=7000;
        assertTrue(progress.render().contains("Video download: Connecting…"));assertTrue(progress.render().contains("7.0 s"));
    }
    @Test public void percentagesRequireAllSegmentLengths(){
        long[] time={0};DownloadProgress progress=new DownloadProgress(x->{},()->time[0]);
        progress.transfer(DownloadProgress.Stage.VIDEO,2);progress.connected(DownloadProgress.Stage.VIDEO,1048576);time[0]=1000;progress.bytes(DownloadProgress.Stage.VIDEO,524288);
        assertFalse(progress.render().contains("%"));
        progress.connected(DownloadProgress.Stage.VIDEO,1048576);
        assertTrue(progress.render().contains("25%"));
    }
    @Test public void unknownSizeDoesNotInventPercentageOrLoseCompletedBytes(){
        long[] time={0};DownloadProgress progress=new DownloadProgress(x->{},()->time[0]);
        progress.transfer(DownloadProgress.Stage.AUDIO,1);progress.connected(DownloadProgress.Stage.AUDIO,-1);time[0]=2000;progress.bytes(DownloadProgress.Stage.AUDIO,1048576);
        assertFalse(progress.render().contains("%"));progress.completeTransfer(DownloadProgress.Stage.AUDIO);
        assertTrue(progress.render().contains("Downloaded 1.0 MB"));assertTrue(progress.render().contains("0.5 MB/s"));
    }
    @Test public void finishingStopsUpdatesAndLeavesCompletedStagesVisible(){
        List<String> updates=new ArrayList<>();long[] time={0};DownloadProgress progress=new DownloadProgress(updates::add,()->time[0]);
        progress.complete(DownloadProgress.Stage.VIDEO_URL,"Found");progress.begin(DownloadProgress.Stage.VIDEO,"Connecting…");
        time[0]=1000;progress.close("Cancelled");int count=updates.size();time[0]=2000;progress.tick();
        assertEquals(count,updates.size());assertTrue(progress.isClosed());assertTrue(progress.render().contains("Video URL: Found"));assertTrue(progress.render().contains("Video download: Cancelled"));
    }
}
