package ymg.pwcca.facebookgoodies;

import org.junit.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

public final class FilterPolicyTest {
    @Test public void removesSponsoredWithoutReorderingOrganicItems() {
        List<String> before = Arrays.asList("friend", "SPONSORED", "group", "SPONSORED", "PROMOTION");
        assertEquals(Arrays.asList("friend", "group", "PROMOTION"), FilterPolicy.partition(before, FilterPolicy::sponsoredCategory));
        assertEquals(5, before.size());
    }
    @Test public void uncertainClassificationKeepsContent() {
        List<String> before = Arrays.asList("organic", null, "unknown");
        assertSame(before, FilterPolicy.partition(before, value -> { if ("unknown".equals(value)) throw new IllegalStateException(); return false; }));
    }
    @Test public void noAdsPreservesCollectionIdentity() {
        List<String> before = Arrays.asList("friend", "PROMOTION");
        assertSame(before, FilterPolicy.partition(before, FilterPolicy::sponsoredCategory));
    }
    @Test public void allAdsYieldsEmptyResult() {
        assertTrue(FilterPolicy.partition(Arrays.asList("SPONSORED", "SPONSORED"), FilterPolicy::sponsoredCategory).isEmpty());
    }
    @Test public void suggestionsAreSeparateFromAds() {
        assertEquals(FilterPolicy.Action.SUGGESTION, FilterPolicy.decide("ENGAGEMENT", false, false, true));
        assertEquals(FilterPolicy.Action.SUGGESTION, FilterPolicy.decide("ENGAGEMENT_QP", false, false, true));
        assertEquals(FilterPolicy.Action.KEEP, FilterPolicy.decide("ENGAGEMENT", false, true, false));
    }
    @Test public void ordinarySharedPostsAndUnknownCategoriesStay() {
        for (String kind : Arrays.asList("ORGANIC", "PROMOTION", "UNKNOWN", null))
            assertEquals(FilterPolicy.Action.KEEP, FilterPolicy.decide(kind, false, true, true));
    }
    @Test public void containersWithSponsoredChildrenStayIntact() {
        assertEquals(FilterPolicy.Action.KEEP, FilterPolicy.decide("FB_SHORTS", true, true, true));
        assertEquals(FilterPolicy.Action.KEEP, FilterPolicy.decide("MULTI_FB_STORIES_TRAY", true, true, true));
    }
    @Test public void disablingAdsDoesNotReclassifyAdsAsSuggestions() {
        assertEquals(FilterPolicy.Action.KEEP, FilterPolicy.decide("SPONSORED", false, false, true));
        assertEquals(FilterPolicy.Action.KEEP, FilterPolicy.decide("ENGAGEMENT", true, false, true));
    }
    @Test public void sponsoredMarkerWorksWithoutCategoryMetadata() {
        assertEquals(FilterPolicy.Action.AD, FilterPolicy.decide(null, true, true, true));
    }
}
