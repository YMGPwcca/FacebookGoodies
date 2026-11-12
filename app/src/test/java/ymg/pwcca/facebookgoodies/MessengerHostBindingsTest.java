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
}
