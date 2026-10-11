package ymg.pwcca.facebookgoodies;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.*;

/** Two independent media files share the one job's cancellation and are joined before cleanup. */
final class MediaTransfers {
    static void download(DownloadTransfer transfer,DownloadPlan plan,File video,File audio,
                         DownloadTransfer.Progress videoProgress,DownloadTransfer.Progress audioProgress) throws IOException {
        if(audio==null){transfer.track(plan.video,video,videoProgress);return;}
        ExecutorService pool=Executors.newFixedThreadPool(2,r->{Thread t=new Thread(r,"FacebookGoodies-media");t.setDaemon(true);return t;});
        CompletionService<Void> done=new ExecutorCompletionService<>(pool);
        boolean complete=false;
        try {
            done.submit(()->{transfer.track(plan.video,video,videoProgress);return null;});
            done.submit(()->{transfer.track(plan.audio,audio,audioProgress);return null;});
            for(int i=0;i<2;i++)await(done);
            complete=true;
        } finally {
            if(!complete)transfer.abort();pool.shutdownNow();
            join(pool);
        }
    }
    private static void await(CompletionService<Void> done) throws IOException {
        try { done.take().get(); }
        catch(InterruptedException interrupted){Thread.currentThread().interrupt();throw new IOException("Media transfer interrupted");}
        catch(ExecutionException failure){
            Throwable cause=failure.getCause();
            if(cause instanceof IOException)throw (IOException)cause;
            if(cause instanceof RuntimeException)throw (RuntimeException)cause;
            throw new IOException("Media transfer failed");
        }
    }
    private static void join(ExecutorService pool) {
        boolean interrupted=false;
        while(!pool.isTerminated()) {
            try {pool.awaitTermination(250,TimeUnit.MILLISECONDS);}
            catch(InterruptedException ignored){interrupted=true;}
        }
        if(interrupted)Thread.currentThread().interrupt();
    }
}
