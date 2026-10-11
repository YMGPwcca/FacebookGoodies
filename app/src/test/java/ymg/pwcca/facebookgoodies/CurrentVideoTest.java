package ymg.pwcca.facebookgoodies;

import org.junit.Test;
import static org.junit.Assert.*;

public class CurrentVideoTest {
    private static DownloadMedia media(String id) { return new DownloadMedia(id, null, "https://video.fbcdn.net/" + id + ".mp4", null); }
    @Test public void keepsOnlyCurrentPlayer() {
        CurrentVideo current = new CurrentVideo(); Object first = new Object(); Object second = new Object();
        current.replace(first, media("first"));
        current.replace(second, media("second"));
        assertNull(current.snapshot(first)); assertEquals("second", current.snapshot(second).id);
    }
    @Test public void samePlayerReuseReplacesVideo() {
        CurrentVideo current = new CurrentVideo(); Object player = new Object();
        current.replace(player, media("first")); current.replace(player, media("second"));
        assertEquals("second", current.snapshot(player).id);
    }
    @Test public void clickedSnapshotSurvivesScrollingWithoutChangingIdentity() {
        CurrentVideo current = new CurrentVideo(); Object first = new Object(); Object second = new Object();
        current.replace(first, media("first")); DownloadMedia clicked = current.snapshot(first);
        current.replace(second, media("second")); current.clear();
        assertEquals("first", clicked.id); assertNull(current.player()); assertNull(current.snapshot(second));
    }
    @Test public void clearDropsMetadataAndOwner() {
        CurrentVideo current = new CurrentVideo(); Object player = new Object(); current.replace(player, media("first"));
        current.clear(); assertNull(current.player()); assertNull(current.snapshot(player)); assertNull(current.snapshot(null));
    }
}
