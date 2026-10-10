package ymg.pwcca.facebookgoodies;

import org.junit.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

public final class ContentFilterPolicyTest {
    private static final ContentFilters THREADS = new ContentFilters(true, false, false, false);
    private static final ContentFilters TOP = new ContentFilters(false, true, false, false);
    private static final ContentFilters MIDDLE = new ContentFilters(false, false, true, false);
    private static final ContentFilters REELS = new ContentFilters(false, false, false, true);

    private static FilterPolicy.Action decide(String category, ContentFilters content) {
        return FilterPolicy.decide(category, false, false, false, content);
    }

    @Test public void threadsAreOptInAndDoNotRemoveOtherPromotionCategories() {
        assertEquals(FilterPolicy.Action.KEEP, decide("PROMOTION", ContentFilters.OFF));
        assertEquals(FilterPolicy.Action.THREADS, decide("PROMOTION", THREADS));
        assertEquals(FilterPolicy.Action.KEEP, decide("FRIENDLY_FEED_PROMOTION", THREADS));
        assertEquals(FilterPolicy.Action.KEEP, decide("HIGH_VALUE_PROMOTION", THREADS));
    }

    @Test public void reelsRemovalIsIndependentOfAdBlocking() {
        assertEquals(FilterPolicy.Action.REELS, decide("FB_SHORTS", REELS));
        assertEquals(FilterPolicy.Action.REELS, decide("FB_SHORTS_FALLBACK", REELS));
        assertEquals(FilterPolicy.Action.KEEP, FilterPolicy.decide("FB_SHORTS", true, true, true, ContentFilters.OFF));
        assertEquals(FilterPolicy.Action.KEEP, FilterPolicy.decide("FB_SHORTS_FALLBACK", true, true, true, ContentFilters.OFF));
    }

    @Test public void topStoriesSettingDoesNotRemoveMiddleStoriesData() {
        assertEquals(FilterPolicy.Action.KEEP, decide("MULTI_FB_STORIES_TRAY", TOP));
        assertFalse(TOP.hasFeedRules());
        assertEquals(FilterPolicy.Action.STORIES_MIDDLE, decide("MULTI_FB_STORIES_TRAY", MIDDLE));
    }

    @Test public void eachContentSwitchLeavesOtherContentCategoriesAlone() {
        assertEquals(FilterPolicy.Action.KEEP, decide("FB_SHORTS", THREADS));
        assertEquals(FilterPolicy.Action.KEEP, decide("MULTI_FB_STORIES_TRAY", THREADS));
        assertEquals(FilterPolicy.Action.KEEP, decide("PROMOTION", REELS));
        assertEquals(FilterPolicy.Action.KEEP, decide("MULTI_FB_STORIES_TRAY", REELS));
        assertEquals(FilterPolicy.Action.KEEP, decide("FB_SHORTS", MIDDLE));
        assertEquals(FilterPolicy.Action.KEEP, decide("PROMOTION", MIDDLE));
    }

    @Test public void unknownAndOrganicPostsRemainWithEveryContentSwitchEnabled() {
        ContentFilters all = new ContentFilters(true, true, true, true);
        for (String category : Arrays.asList(null, "ORGANIC", "UNKNOWN", "SHOWCASE", "FB_STORIES"))
            assertEquals(FilterPolicy.Action.KEEP, decide(category, all));
    }

    @Test public void contentOnlyFilteringRunsWhenAdsAndSuggestionsAreDisabled() {
        assertTrue(new Options(false, false, false, false, THREADS).hasFeedFilters());
        assertTrue(new Options(false, false, false, false, MIDDLE).hasFeedFilters());
        assertTrue(new Options(false, false, false, false, REELS).hasFeedFilters());
        assertFalse(new Options(false, false, false, false, TOP).hasFeedFilters());
        assertFalse(Options.OFF.hasFeedFilters());
    }

    @Test public void newContentFiltersDoNotImplyAdsOrSuggestionsFilters() {
        assertEquals(FilterPolicy.Action.KEEP, decide("SPONSORED", THREADS));
        assertEquals(FilterPolicy.Action.KEEP, decide("ENGAGEMENT", THREADS));
        assertEquals(FilterPolicy.Action.AD, FilterPolicy.decide("SPONSORED", false, true, false, THREADS));
        assertEquals(FilterPolicy.Action.SUGGESTION, FilterPolicy.decide("ENGAGEMENT", false, false, true, THREADS));
    }

    @Test public void filteringKeepsTheOrderAndOriginalMixedContentList() {
        List<String> source = Arrays.asList("FB_SHORTS", "PROMOTION", "MULTI_FB_STORIES_TRAY", "ORGANIC");
        assertEquals(Arrays.asList("FB_SHORTS", "MULTI_FB_STORIES_TRAY", "ORGANIC"),
                FilterPolicy.partition(source, category -> decide(category, THREADS) != FilterPolicy.Action.KEEP));
        assertEquals(4, source.size());
    }
}
