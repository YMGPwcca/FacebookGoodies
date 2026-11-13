package ymg.pwcca.facebookgoodies;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public final class MessengerHostBindingsTest {
    @Test public void both586BuildsCoverTheSameHostTypes() {
        assertEquals(MessengerHostBindings.baseNames(), MessengerHostBindings.rebuildBaseNames());
    }

    @Test public void everySupportedBaseNameResolvesPerBuild() throws Exception {
        for (String base : MessengerHostBindings.baseNames()) {
            String release = MessengerHostBindings.typeName(MessengerBuild.MESSENGER_586, base);
            String rebuild = MessengerHostBindings.typeName(MessengerBuild.MESSENGER_586_REBUILD, base);
            assertTrue(base + " release", release.startsWith("X."));
            assertTrue(base + " rebuild", rebuild.startsWith("X."));
            assertEquals(base, MessengerHostBindings.typeName(MessengerBuild.MESSENGER_585, base));
            assertEquals(base, MessengerHostBindings.typeName(MessengerBuild.MESSENGER_584_1, base));
        }
    }

    @Test public void aiSummaryTargetsResolvePerBuild() {
        assertEquals("X.9XY", MessengerHostBindings.summaryPillHandler(MessengerBuild.MESSENGER_584_1));
        assertEquals("X.Ado", MessengerHostBindings.summaryPillHandler(MessengerBuild.MESSENGER_585));
        assertEquals("X.AIu", MessengerHostBindings.summaryPillHandler(MessengerBuild.MESSENGER_586));
        assertEquals("X.AMT", MessengerHostBindings.summaryPillHandler(MessengerBuild.MESSENGER_586_REBUILD));
        assertEquals("X.9T0", MessengerHostBindings.summaryJumpPill(MessengerBuild.MESSENGER_584_1));
        assertEquals("X.AUP", MessengerHostBindings.summaryJumpPill(MessengerBuild.MESSENGER_585));
        assertEquals("X.AIt", MessengerHostBindings.summaryJumpPill(MessengerBuild.MESSENGER_586));
        assertEquals("X.AMS", MessengerHostBindings.summaryJumpPill(MessengerBuild.MESSENGER_586_REBUILD));
    }

    @Test public void menuControllerResolvesPerBuild() throws Exception {
        assertEquals("X.4Ts", MessengerHostBindings.menuController(MessengerBuild.MESSENGER_584_1));
        assertEquals("X.4LQ", MessengerHostBindings.menuController(MessengerBuild.MESSENGER_585));
        assertEquals("X.4gP", MessengerHostBindings.menuController(MessengerBuild.MESSENGER_586));
        assertEquals("X.4hk", MessengerHostBindings.menuController(MessengerBuild.MESSENGER_586_REBUILD));
    }
}
