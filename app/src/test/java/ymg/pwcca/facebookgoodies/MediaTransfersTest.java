package ymg.pwcca.facebookgoodies;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.concurrent.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class MediaTransfersTest {
    private static final String VIDEO="https://video.fbcdn.net/video.mp4",AUDIO="https://video.fbcdn.net/audio.mp4";
    private static class Response extends HttpURLConnection {
        final CountDownLatch connected;
        final CountDownLatch released=new CountDownLatch(1);
        final String data;
        final boolean blocking;
        final int status;
        volatile boolean disconnected;
        Response(String url,String data,CountDownLatch connected,boolean blocking,int status)throws Exception{super(new URL(url));this.data=data;this.connected=connected;this.blocking=blocking;this.status=status;}
        @Override public int getResponseCode()throws IOException{
            connected.countDown();
            try{if(!connected.await(3,TimeUnit.SECONDS))throw new IOException("Transfers did not overlap");}
            catch(InterruptedException e){throw new IOException("Interrupted");}
            return status;
        }
        @Override public long getContentLengthLong(){return data.length();}
        @Override public InputStream getInputStream(){
            if(!blocking)return new ByteArrayInputStream(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return new InputStream(){@Override public int read()throws IOException{try{released.await(3,TimeUnit.SECONDS);}catch(InterruptedException e){}throw new IOException("Disconnected");}};
        }
        @Override public void disconnect(){disconnected=true;released.countDown();}
        @Override public boolean usingProxy(){return false;}
        @Override public void connect(){}
    }
    private static DownloadPlan plan()throws Exception{
        return new DownloadPlan(DownloadPlan.progressive(VIDEO).video,DownloadPlan.progressive(AUDIO).video,true);
    }
    @Test public void overlapsVideoAndAudioAndLeavesTransferUsableForMux()throws Exception{
        CountDownLatch connected=new CountDownLatch(2);
        Response video=new Response(VIDEO,"picture",connected,false,200),audio=new Response(AUDIO,"sound",connected,false,200);
        DownloadTransfer transfer=new DownloadTransfer(url->url.equals(VIDEO)?video:audio);
        File v=File.createTempFile("picture",".mp4"),a=File.createTempFile("sound",".mp4");
        try{
            MediaTransfers.download(transfer,plan(),v,a,count->{},count->{});transfer.check();
            assertEquals("picture",new String(Files.readAllBytes(v.toPath()),"UTF-8"));assertEquals("sound",new String(Files.readAllBytes(a.toPath()),"UTF-8"));
            assertTrue(video.disconnected);assertTrue(audio.disconnected);
        }finally{v.delete();a.delete();}
    }
    @Test public void cancellationDisconnectsBothTracksBeforeReturning()throws Exception{
        CountDownLatch connected=new CountDownLatch(2);
        Response video=new Response(VIDEO,"picture",connected,true,200),audio=new Response(AUDIO,"sound",connected,true,200);
        DownloadTransfer transfer=new DownloadTransfer(url->url.equals(VIDEO)?video:audio);
        File v=File.createTempFile("picture",".mp4"),a=File.createTempFile("sound",".mp4");ExecutorService executor=Executors.newSingleThreadExecutor();
        try{
            Future<?> result=executor.submit(()->{try{MediaTransfers.download(transfer,plan(),v,a,count->{},count->{});}catch(Exception e){throw new RuntimeException(e);}});
            assertTrue(connected.await(3,TimeUnit.SECONDS));transfer.cancel();
            try{result.get(3,TimeUnit.SECONDS);fail("must cancel");}catch(ExecutionException expected){}
            assertTrue(transfer.cancelled());assertTrue(video.disconnected);assertTrue(audio.disconnected);
        }finally{executor.shutdownNow();v.delete();a.delete();}
    }
    @Test public void oneFailureStopsTheOtherTrackWithoutCallingItUserCancellation()throws Exception{
        CountDownLatch connected=new CountDownLatch(2);
        Response video=new Response(VIDEO,"picture",connected,true,200),audio=new Response(AUDIO,"sound",connected,false,403);
        DownloadTransfer transfer=new DownloadTransfer(url->url.equals(VIDEO)?video:audio);
        File v=File.createTempFile("picture",".mp4"),a=File.createTempFile("sound",".mp4");
        try{
            try{MediaTransfers.download(transfer,plan(),v,a,count->{},count->{});fail("must fail");}catch(IOException expected){assertTrue(expected.getMessage().contains("403"));}
            assertFalse(transfer.cancelled());assertTrue(video.disconnected);assertTrue(audio.disconnected);
        }finally{v.delete();a.delete();}
    }
}
