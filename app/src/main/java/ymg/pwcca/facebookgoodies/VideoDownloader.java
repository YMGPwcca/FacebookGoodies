package ymg.pwcca.facebookgoodies;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

final class VideoDownloader {
    interface Listener {
        void progress(String message);
        void finished(String message, boolean success);
    }
    private final Context app;
    private final HostState state;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final AtomicBoolean busy = new AtomicBoolean();
    private volatile DownloadTransfer transfer;
    VideoDownloader(Context app) { this(app, new HostState()); }
    VideoDownloader(Context app, HostState state) {
        // Application.attach runs before LoadedApk publishes getApplicationContext().
        // The supplied Application is already a valid Context and must not be replaced with null.
        this.app = app instanceof android.app.Application ? app : app.getApplicationContext();
        if (this.app == null) throw new IllegalArgumentException("Application context unavailable");
        this.state = state;
    }
    boolean busy() { return busy.get(); }
    boolean start(DownloadMedia media, Listener listener) {
        if (!busy.compareAndSet(false, true)) return false;
        DownloadTransfer task = new DownloadTransfer(); transfer = task;
        Thread thread = new Thread(() -> run(media, task, listener), "FacebookGoodies-download");
        thread.setDaemon(true); thread.start();
        return true;
    }
    void cancel() { DownloadTransfer task = transfer; if (task != null) task.cancel(); }
    private void run(DownloadMedia media, DownloadTransfer task, Listener listener) {
        File directory = null;
        boolean success = false;
        String message;
        DownloadProgress progress=new DownloadProgress(text->publishProgress(listener,text));
        Runnable ticker=new Runnable(){public void run(){if(progress.isClosed())return;progress.tick();if(!progress.isClosed())main.postDelayed(this,1000);}};
        main.post(ticker);
        try {
            progress.begin(DownloadProgress.Stage.PREPARE,"Reading current player…");
            state.downloadError.set(0); state.downloadStage.set(1);
            directory = temporaryDirectory();
            progress.complete(DownloadProgress.Stage.PREPARE,"Ready");
            execute(media,task,directory,progress);
            message = "Saved to Downloads/FacebookGoodies"; success = true;
        } catch (IOException failure) {
            diagnostic(failure);
            message = task.cancelled() ? "Download cancelled" : failure instanceof DownloadFailure ? failure.getMessage()
                    : "Download failed. Check connection and storage, then play the video again.";
        }
        catch (RuntimeException failure) {
            diagnostic(failure);
            message = task.cancelled() ? "Download cancelled" : runtimeMessage(state.downloadStage.get());
        }
        finally {
            finishProgress(directory,progress,success,task.cancelled());
            main.removeCallbacks(ticker);transfer=null;busy.set(false);
        }
        boolean result = success;
        String text = message == null ? "Download failed. Try playing the video again." : message;
        main.post(() -> listener.finished(text, result));
    }
    private void finishProgress(File directory,DownloadProgress progress,boolean success,boolean cancelled) {
        progress.begin(DownloadProgress.Stage.CLEANUP,"Removing temporary media…");
        boolean cleaned=cleanup(directory);
        progress.complete(DownloadProgress.Stage.CLEANUP,cleaned?"Cleared":"Cleanup will retry on the next download");
        progress.close(success?"Complete":cancelled?"Cancelled":"Stopped after failure");
    }
    private void execute(DownloadMedia media,DownloadTransfer task,File directory,DownloadProgress progress) throws IOException {
        state.downloadStage.set(2);DownloadPlan plan=plan(media,task,progress);
        File video=new File(directory,"video.mp4"),audio=plan.audio==null?null:new File(directory,"audio.mp4");
        downloadTracks(task,plan,video,audio,progress);
        task.check();progress.begin(DownloadProgress.Stage.MERGE,"Reading and combining media tracks…");
        File output=new File(directory,"complete.mp4");
        state.downloadStage.set(5);DownloadMuxer.mux(video,audio,output,plan,task,state);
        progress.complete(DownloadProgress.Stage.MERGE,"MP4 ready");
        progress.begin(DownloadProgress.Stage.SAVE,"Writing to Downloads/FacebookGoodies…");
        task.check();state.downloadStage.set(6);DownloadStorage.publish(app,output,task);
        progress.complete(DownloadProgress.Stage.SAVE,"Saved");state.downloadStage.set(8);
    }
    private void downloadTracks(DownloadTransfer task,DownloadPlan plan,File video,File audio,DownloadProgress progress) throws IOException {
        progress.transfer(DownloadProgress.Stage.VIDEO,plan.video.parts.size());
        if(audio!=null)progress.transfer(DownloadProgress.Stage.AUDIO,plan.audio.parts.size());
        else progress.complete(DownloadProgress.Stage.AUDIO,"No separate audio required");
        state.downloadStage.set(3);
        MediaTransfers.download(task,plan,video,audio,transferProgress(progress,DownloadProgress.Stage.VIDEO),transferProgress(progress,DownloadProgress.Stage.AUDIO));
        state.videoContainer.set(MediaContainer.kind(video));state.audioContainer.set(audio==null?0:MediaContainer.kind(audio));
    }
    private void diagnostic(Exception failure) {
        state.downloadError.set(errorCode(failure));
        StringBuilder frames = new StringBuilder();
        for (StackTraceElement frame : failure.getStackTrace()) {
            if (!frame.getClassName().startsWith("ymg.pwcca.facebookgoodies.")) continue;
            if (frames.length() > 512) break;
            frames.append(' ').append(frame.getClassName()).append('.').append(frame.getMethodName()).append(':').append(frame.getLineNumber());
        }
        Throwable cause=failure.getCause();
        if(cause!=null)for(StackTraceElement frame:cause.getStackTrace()){
            if(!frame.getClassName().startsWith("org.mp4parser."))continue;
            if(frames.length()>1400)break;
            frames.append(' ').append(frame.getClassName()).append('.').append(frame.getMethodName()).append(':').append(frame.getLineNumber());
        }
        android.util.Log.w("FacebookGoodies", "download stage=" + state.downloadStage.get() + " error=" + state.downloadError.get() + frames);
    }
    private static int errorCode(Exception failure) {
        if (failure instanceof SecurityException) return 1;
        if (failure instanceof IllegalArgumentException) return 2;
        if (failure instanceof NullPointerException) return 3;
        if (failure instanceof UnsupportedOperationException) return 4;
        if (failure instanceof ClassCastException) return 5;
        if (failure instanceof IOException) return 10;
        return 9;
    }
    private static String runtimeMessage(int stage) {
        if (stage == 1) return "Could not prepare the download. Restart Facebook and try again.";
        if (stage == 2) return "Could not read this video's sources. Replay it and try again.";
        if (stage == 6) return "Could not save the video. Check storage access and free space.";
        return "Could not download this video. Replay it and try again.";
    }
    private DownloadPlan plan(DownloadMedia media, DownloadTransfer task,DownloadProgress progress) throws IOException {
        progress.begin(DownloadProgress.Stage.STREAM,"Reading stream information…");
        if (DownloadMedia.present(media.manifest)) {
            String manifest = media.manifest.trim();
            String base = null;
            if (!manifest.startsWith("<")) {
                base=manifest;progress.transfer(DownloadProgress.Stage.STREAM,1);
                manifest=task.manifest(base,transferProgress(progress,DownloadProgress.Stage.STREAM));
            }
            progress.complete(DownloadProgress.Stage.STREAM,"Stream information loaded");
            return DashManifest.parse(manifest,base,sourceProgress(progress));
        }
        progress.complete(DownloadProgress.Stage.STREAM,"Direct player source");
        progress.begin(DownloadProgress.Stage.VIDEO_URL,"Finding video URL…");
        String url = DownloadMedia.present(media.hd) ? media.hd : media.sd;
        if (!DownloadMedia.present(url)) throw new DownloadFailure("No download source for this video");
        DownloadPlan plan=DownloadPlan.progressive(url);
        progress.complete(DownloadProgress.Stage.VIDEO_URL,DownloadMedia.present(media.hd)?"HD source found":"Source found");
        progress.complete(DownloadProgress.Stage.AUDIO_URL,"Included in the video file");
        return plan;
    }
    private static DashManifest.SourceProgress sourceProgress(DownloadProgress progress) {
        return new DashManifest.SourceProgress(){
            public void finding(boolean video){progress.begin(video?DownloadProgress.Stage.VIDEO_URL:DownloadProgress.Stage.AUDIO_URL,video?"Finding highest-resolution video URL…":"Finding audio URL…");}
            public void found(boolean video,DownloadPlan.Track track){
                String detail=track==null?"No separate audio source":video?"Found · "+track.width+" × "+track.height:"Found · "+(track.bandwidth/1000)+" kbps";
                progress.complete(video?DownloadProgress.Stage.VIDEO_URL:DownloadProgress.Stage.AUDIO_URL,detail);
            }
        };
    }
    private static DownloadTransfer.Progress transferProgress(DownloadProgress progress,DownloadProgress.Stage stage){
        return new DownloadTransfer.Progress(){
            public void connecting(){progress.connecting(stage);}
            public void connected(long bytes){progress.connected(stage,bytes);}
            public void bytes(long bytes){progress.bytes(stage,bytes);}
            public void finished(){progress.completeTransfer(stage);}
        };
    }
    private File temporaryDirectory() throws IOException {
        File parent = new File(app.getCacheDir(), "facebookgoodies-download");
        if (!parent.isDirectory() && !parent.mkdirs()) throw new DownloadFailure("Cannot create download workspace");
        // One job at a time: any previous directory belongs to an interrupted process.
        File[] old = parent.listFiles();
        if (old != null) for (File entry : old) cleanup(entry);
        File directory = new File(parent, java.util.UUID.randomUUID().toString());
        if (!directory.mkdir()) throw new DownloadFailure("Cannot create download workspace");
        return directory;
    }
    private static boolean cleanup(File directory) {
        if (directory == null) return true;
        boolean clean=true;
        File[] files = directory.listFiles();
        if (files != null) for (File file : files) clean &= file.delete() || !file.exists();
        return (directory.delete() || !directory.exists()) && clean;
    }
    private void publishProgress(Listener listener, String text) { main.post(() -> listener.progress(text)); }
}
