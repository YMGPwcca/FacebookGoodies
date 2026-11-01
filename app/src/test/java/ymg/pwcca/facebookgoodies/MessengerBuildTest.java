package ymg.pwcca.facebookgoodies;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public final class MessengerBuildTest {
    @Test public void identifiesExactSupportedBuilds() {
        assertEquals(MessengerBuild.MESSENGER_584_1,
                MessengerBuild.identify("584.1.0.61.98", 346817021L));
        assertEquals(MessengerBuild.MESSENGER_585,
                MessengerBuild.identify("585.0.0.53.94", 347015062L));
    }

    @Test public void rejectsMismatchedAndUnknownBuilds() {
        assertEquals(MessengerBuild.UNSUPPORTED,
                MessengerBuild.identify("584.1.0.61.98", 347015062L));
        assertEquals(MessengerBuild.UNSUPPORTED,
                MessengerBuild.identify("585.0.0.53.94", 346817021L));
        assertEquals(MessengerBuild.UNSUPPORTED,
                MessengerBuild.identify("585.0.0.53.94", 347015061L));
        assertEquals(MessengerBuild.UNSUPPORTED,
                MessengerBuild.identify(null, 347015062L));
    }
}
