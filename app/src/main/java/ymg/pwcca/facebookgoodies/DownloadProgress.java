package ymg.pwcca.facebookgoodies;

import java.util.EnumMap;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/** A transient, identifier-free view of all stages of the one explicit download. */
final class DownloadProgress {
    enum Stage {
        PREPARE("Current video"), STREAM("Stream information"), VIDEO_URL("Video URL"), AUDIO_URL("Audio URL"),
        VIDEO("Video download"), AUDIO("Audio download"), MERGE("Combining tracks"), SAVE("Saving file"), CLEANUP("Temporary files");
        final String label;
        Stage(String label) { this.label=label; }
    }
    private final EnumMap<Stage,Entry> entries=new EnumMap<>(Stage.class);
    private final Consumer<String> sink;
    private final LongSupplier clock;
    private long published;
    private boolean closed;
    DownloadProgress(Consumer<String> sink) { this(sink,()->System.nanoTime()/1000000); }
    DownloadProgress(Consumer<String> sink,LongSupplier clock) {
        this.sink=sink;this.clock=clock;
        for(Stage stage:Stage.values())entries.put(stage,new Entry());
    }
    synchronized void begin(Stage stage,String detail) {
        Entry entry=entries.get(stage);
        if(entry.started<0)entry.started=clock.getAsLong();
        entry.detail=detail;entry.active=true;emit(true);
    }
    synchronized void complete(Stage stage,String detail) {
        Entry entry=entries.get(stage);
        if(entry.started<0)entry.started=clock.getAsLong();
        entry.ended=clock.getAsLong();entry.active=false;entry.detail=detail;emit(true);
    }
    synchronized void transfer(Stage stage,int segments) {
        Entry entry=entries.get(stage);entry.segments=segments;
        begin(stage,"Connecting…");
    }
    synchronized void connecting(Stage stage) { entries.get(stage).detail="Connecting…";emit(true); }
    synchronized void connected(Stage stage,long size) {
        Entry entry=entries.get(stage);entry.connected++;
        if(size<0)entry.known=false;else entry.expected+=size;
        entry.detail="Downloading";emit(true);
    }
    synchronized void bytes(Stage stage,long count) {
        Entry entry=entries.get(stage);entry.bytes+=count;entry.detail="Downloading";emit(false);
    }
    synchronized void completeTransfer(Stage stage) {
        Entry entry=entries.get(stage);
        long elapsed=Math.max(1,clock.getAsLong()-entry.started);
        complete(stage,String.format(Locale.getDefault(),"Downloaded %.1f MB · %.1f MB/s",entry.bytes/1048576.0,entry.bytes*1000.0/elapsed/1048576.0));
    }
    synchronized boolean isClosed(){return closed;}
    synchronized void tick() { emit(false); }
    synchronized void close(String outcome) {
        long now=clock.getAsLong();
        for(Entry entry:entries.values())if(entry.active){entry.active=false;entry.ended=now;entry.detail=outcome;}
        emit(true);closed=true;
    }
    synchronized String render() {
        StringBuilder text=new StringBuilder();long now=clock.getAsLong();
        for(Stage stage:Stage.values()) {
            Entry entry=entries.get(stage);
            if(text.length()>0)text.append("\n\n");
            text.append(stage.label).append(": ").append(detail(entry,now));
            if(entry.started>=0)text.append(String.format(Locale.getDefault()," · %.1f s",(Math.max(0,(entry.active?now:entry.ended)-entry.started))/1000.0));
        }
        return text.toString();
    }
    private String detail(Entry entry,long now) {
        if(entry.started<0)return "Waiting";
        if(!entry.detail.equals("Downloading"))return entry.detail;
        String bytes=String.format(Locale.getDefault(),"%.1f MB",entry.bytes/1048576.0);
        long elapsed=Math.max(1,now-entry.started);
        String speed=String.format(Locale.getDefault(),"%.1f MB/s",entry.bytes*1000.0/elapsed/1048576.0);
        if(entry.known && entry.connected==entry.segments && entry.expected>0) {
            int percent=(int)Math.min(100,entry.bytes*100.0/entry.expected);
            return String.format(Locale.getDefault(),"%.1f / %.1f MB · %d%% · %s",entry.bytes/1048576.0,entry.expected/1048576.0,percent,speed);
        }
        return bytes+" · "+speed;
    }
    private void emit(boolean force) {
        if(closed)return;
        long now=clock.getAsLong();if(!force && now-published<500)return;
        published=now;sink.accept(render());
    }
    private static final class Entry {
        String detail="Waiting";
        long started=-1,ended=-1,bytes,expected;
        int segments,connected;
        boolean active,known=true;
    }
}
