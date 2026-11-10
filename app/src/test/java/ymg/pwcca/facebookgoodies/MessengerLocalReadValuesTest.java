package ymg.pwcca.facebookgoodies;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class MessengerLocalReadValuesTest {
    @Test public void localOverrideDoesNotBlockNewerRemoteReadOrAnotherChat() {
        MessengerLocalReadValues local = new MessengerLocalReadValues();
        local.record("first", 300);
        assertEquals(200, MessengerSeenBorderPolicy.syncedRead(300, 200, false, local.contains("first", 300)));
        assertEquals(301, MessengerSeenBorderPolicy.syncedRead(301, 200, false, local.contains("first", 301)));
        assertEquals(300, MessengerSeenBorderPolicy.syncedRead(300, 200, false, local.contains("second", 300)));
    }

    @Test public void everyAppliedValueRemainsGuardedUntilConfirmedPastIt() {
        MessengerLocalReadValues local = new MessengerLocalReadValues();
        local.record("first", 300);
        local.record("first", 500);
        assertEquals(250, MessengerSeenBorderPolicy.syncedRead(300, 250, false, local.contains("first", 300)));
        assertEquals(250, MessengerSeenBorderPolicy.syncedRead(500, 250, false, local.contains("first", 500)));
        local.discardThrough("first", 400);
        assertEquals(400, MessengerSeenBorderPolicy.syncedRead(300, 400, false, local.contains("first", 300)));
        assertEquals(400, MessengerSeenBorderPolicy.syncedRead(500, 400, false, local.contains("first", 500)));
    }
}
