package ymg.pwcca.facebookgoodies;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public final class MessengerBuildTest {
    @Test public void identifiesExactSupportedBuilds() {
        assertEquals(MessengerBuild.MESSENGER_584_1,
                MessengerBuild.identify("584.1.0.61.98", 346817021L));
        assertEquals(MessengerBuild.MESSENGER_585,
                MessengerBuild.identify("585.0.0.53.94", 347015062L));
        assertEquals(MessengerBuild.MESSENGER_586,
                MessengerBuild.identify("586.0.0.55.92", 347215273L));
        assertEquals(MessengerBuild.MESSENGER_586_REBUILD,
                MessengerBuild.identify("586.0.0.55.92", 347215343L));
    }

    @Test public void marksBoth586BuildsAs586() {
        assertEquals(true, MessengerBuild.MESSENGER_586.is586());
        assertEquals(true, MessengerBuild.MESSENGER_586_REBUILD.is586());
        assertEquals(false, MessengerBuild.MESSENGER_585.is586());
        assertEquals(false, MessengerBuild.MESSENGER_584_1.is586());
        assertEquals(false, MessengerBuild.UNSUPPORTED.is586());
    }

    @Test public void rejectsMismatchedAndUnknownBuilds() {
        assertEquals(MessengerBuild.UNSUPPORTED,
                MessengerBuild.identify("584.1.0.61.98", 347015062L));
        assertEquals(MessengerBuild.UNSUPPORTED,
                MessengerBuild.identify("585.0.0.53.94", 346817021L));
        assertEquals(MessengerBuild.UNSUPPORTED,
                MessengerBuild.identify("585.0.0.53.94", 347015061L));
        assertEquals(MessengerBuild.UNSUPPORTED,
                MessengerBuild.identify("586.0.0.55.92", 347215272L));
        assertEquals(MessengerBuild.UNSUPPORTED,
                MessengerBuild.identify("586.0.0.55.92", 347215342L));
        assertEquals(MessengerBuild.UNSUPPORTED,
                MessengerBuild.identify("586.0.0.55.92", 347215344L));
        assertEquals(MessengerBuild.UNSUPPORTED,
                MessengerBuild.identify("586.0.0.55.91", 347215273L));
        assertEquals(MessengerBuild.UNSUPPORTED,
                MessengerBuild.identify(null, 347015062L));
    }
}
