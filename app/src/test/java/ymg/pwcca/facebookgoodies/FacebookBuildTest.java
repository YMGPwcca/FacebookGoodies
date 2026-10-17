package ymg.pwcca.facebookgoodies;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public final class FacebookBuildTest {
    @Test public void preservesBothSupportedBuilds() {
        assertEquals(FacebookBuild.FACEBOOK_582, FacebookBuild.identify("582.0.0.50.54", 475417129L));
        assertEquals(FacebookBuild.FACEBOOK_583, FacebookBuild.identify("583.0.0.39.55", 475614366L));
    }

    @Test public void rejectsMismatchedAndUnknownBuilds() {
        assertEquals(FacebookBuild.UNSUPPORTED, FacebookBuild.identify("582.0.0.50.54", 475614366L));
        assertEquals(FacebookBuild.UNSUPPORTED, FacebookBuild.identify("583.0.0.39.55", 475417129L));
        assertEquals(FacebookBuild.UNSUPPORTED, FacebookBuild.identify("583.0.0.39.55", 475614365L));
        assertEquals(FacebookBuild.UNSUPPORTED, FacebookBuild.identify(null, 475614366L));
    }
}
