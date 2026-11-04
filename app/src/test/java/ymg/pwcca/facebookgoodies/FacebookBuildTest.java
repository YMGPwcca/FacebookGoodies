package ymg.pwcca.facebookgoodies;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public final class FacebookBuildTest {
    @Test public void preservesAllSupportedBuilds() {
        assertEquals(FacebookBuild.FACEBOOK_582, FacebookBuild.identify("582.0.0.50.54", 475417129L));
        assertEquals(FacebookBuild.FACEBOOK_583, FacebookBuild.identify("583.0.0.39.55", 475614366L));
        assertEquals(FacebookBuild.FACEBOOK_584, FacebookBuild.identify("584.0.0.51.56", 475817510L));
        assertEquals(FacebookBuild.FACEBOOK_584_476015532, FacebookBuild.identify("584.0.0.51.56", 476015532L));
        assertEquals(FacebookBuild.FACEBOOK_585, FacebookBuild.identify("585.0.0.47.54", 476015694L));
    }

    @Test public void rejectsMismatchedAndUnknownBuilds() {
        assertEquals(FacebookBuild.UNSUPPORTED, FacebookBuild.identify("582.0.0.50.54", 475614366L));
        assertEquals(FacebookBuild.UNSUPPORTED, FacebookBuild.identify("583.0.0.39.55", 475417129L));
        assertEquals(FacebookBuild.UNSUPPORTED, FacebookBuild.identify("583.0.0.39.55", 475614365L));
        assertEquals(FacebookBuild.UNSUPPORTED, FacebookBuild.identify("584.0.0.51.56", 475817509L));
        assertEquals(FacebookBuild.UNSUPPORTED, FacebookBuild.identify("584.0.0.51.56", 476015531L));
        assertEquals(FacebookBuild.UNSUPPORTED, FacebookBuild.identify(null, 475614366L));
        assertEquals(FacebookBuild.UNSUPPORTED, FacebookBuild.identify("585.0.0.47.54", 476015693L));
        assertEquals(FacebookBuild.UNSUPPORTED, FacebookBuild.identify("584.0.0.51.56", 476015694L));
    }
}
