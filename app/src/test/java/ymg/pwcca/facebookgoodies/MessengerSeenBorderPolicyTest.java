package ymg.pwcca.facebookgoodies;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class MessengerSeenBorderPolicyTest {
    @Test public void outlinesEveryIncomingMessageAfterTheLastConfirmedReceipt() {
        assertTrue(MessengerSeenBorderPolicy.show(true, true, 201, 200));
        assertTrue(MessengerSeenBorderPolicy.show(true, true, 202, 200));
        assertTrue(MessengerSeenBorderPolicy.show(true, true, 203, 200));
        assertFalse(MessengerSeenBorderPolicy.show(true, true, 200, 200));
        assertFalse(MessengerSeenBorderPolicy.show(true, false, 201, 200));
    }

    @Test public void aConfirmedReceiptClearsOldBordersButNewMessagesRemainMarked() {
        assertFalse(MessengerSeenBorderPolicy.show(true, true, 201, 203));
        assertFalse(MessengerSeenBorderPolicy.show(true, true, 203, 203));
        assertTrue(MessengerSeenBorderPolicy.show(true, true, 204, 203));
        assertFalse(MessengerSeenBorderPolicy.show(false, true, 204, 203));
        assertFalse(MessengerSeenBorderPolicy.show(true, true, 0, 0));
    }

    @Test public void freezesInitialReceiptBeforeSuppressedLocalReadsAdvance() {
        assertEquals(200, MessengerSeenBorderPolicy.initialRead(200, 0, 0));
        assertEquals(200, MessengerSeenBorderPolicy.initialRead(250, 0, 200));
        assertEquals(200, MessengerSeenBorderPolicy.initialRead(250, 300, 200));
        assertEquals(200, MessengerSeenBorderPolicy.initialRead(300, 300, 200));
        assertEquals(200, MessengerSeenBorderPolicy.initialRead(0, 0, 200));
        assertEquals(0, MessengerSeenBorderPolicy.initialRead(300, 300, 0));
    }

    @Test public void syncedReadClearsOnlyMessagesAtOrBeforeItsWatermark() {
        long confirmed = MessengerSeenBorderPolicy.syncedRead(203, 200, true, false);
        assertFalse(MessengerSeenBorderPolicy.show(true, true, 203, confirmed));
        assertTrue(MessengerSeenBorderPolicy.show(true, true, 204, confirmed));
        assertEquals(203, MessengerSeenBorderPolicy.syncedRead(201, confirmed, true, false));
        assertEquals(203, MessengerSeenBorderPolicy.syncedRead(0, confirmed, true, false));
    }

    @Test public void localReadWatermarkCannotMasqueradeAsSyncedReceipt() {
        assertEquals(200, MessengerSeenBorderPolicy.syncedRead(300, 200, false, true));
        assertEquals(300, MessengerSeenBorderPolicy.syncedRead(300, 200, true, true));
        assertEquals(301, MessengerSeenBorderPolicy.syncedRead(301, 200, false, false));
    }

    @Test public void manualReceiptCanUseRenderedMessagesWhenRequestHasNoTimestamp() {
        assertEquals(203, MessengerSeenBorderPolicy.sentWatermark(null, 200L, 203L));
        assertEquals(205, MessengerSeenBorderPolicy.sentWatermark(null, 205L, 203L));
        assertEquals(204, MessengerSeenBorderPolicy.sentWatermark(204L, 200L, 205L));
        assertEquals(205, MessengerSeenBorderPolicy.sentWatermark(0L, 200L, 205L));
    }
}
