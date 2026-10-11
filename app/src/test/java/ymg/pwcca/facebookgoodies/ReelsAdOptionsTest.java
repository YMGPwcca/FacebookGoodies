package ymg.pwcca.facebookgoodies;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.Test;
import static org.junit.Assert.*;

public final class ReelsAdOptionsTest {
    @Test public void advertisingFlagsAreDisabledOnlyInsideTheSelectedBuilder() {
        ReelsAdOptions scope = new ReelsAdOptions();
        Object builder = new Object();
        Object previous = scope.enter(builder);
        try {
            assertTrue(ReelsAdOptions.buildingLaunchOptions());
            assertEquals(false, scope.value(builder, true, "ReelsIsEligibleForInContentAds", true));
            assertEquals(false, scope.value(builder, true, "ReelsIsEligibleForShoppable", true));
            assertEquals(true, scope.value(builder, true, "ReelsBannerAdSuppressed", false));
            assertEquals(true, scope.value(new Object(), true, "ReelsIsEligibleForInContentAds", true));
        } finally { scope.restore(previous); }
        assertFalse(ReelsAdOptions.buildingLaunchOptions());
        assertEquals(true, scope.value(builder, true, "ReelsIsEligibleForInContentAds", true));
    }

    @Test public void organicVideoAndNavigationPropsRemainUnchanged() {
        ReelsAdOptions scope = new ReelsAdOptions();
        Object builder = new Object();
        Object value = new Object();
        Object previous = scope.enter(builder);
        try {
            for (String key : new String[]{"ReelsHostVideoGraphQLStoryProps", "ReelsVideoStoryId", "ReelsVideoOwnerId",
                    "ReelsSessionId", "ReelsViewStatePosition", "ReelsLaunchFromVODEntry", "isInProductionUnification"})
                assertSame(value, scope.value(builder, true, key, value));
            assertSame(value, scope.value(builder, true, null, value));
            assertEquals(true, scope.value(builder, true, "ReelsVideoOnFeedContainsInstreamVideoAdBreaks", true));
            assertEquals(true, scope.value(builder, true, "ReelsIsBannerAdsAvailable", true));
            assertEquals(true, scope.value(builder, false, "ReelsIsEligibleForInContentAds", true));
        } finally { scope.restore(previous); }
    }

    @Test public void nestedBuildersRestoreTheOuterScopeAfterAnException() {
        ReelsAdOptions scope = new ReelsAdOptions();
        Object outer = new Object();
        Object inner = new Object();
        Object initial = scope.enter(outer);
        try {
            Object previous = scope.enter(inner);
            try {
                assertEquals(true, scope.value(outer, true, "ReelsIsEligibleForInContentAds", true));
                assertEquals(false, scope.value(inner, true, "ReelsIsEligibleForInContentAds", true));
                throw new IllegalStateException("Simulated builder failure");
            } catch (IllegalStateException expected) {
                assertEquals("Simulated builder failure", expected.getMessage());
            } finally { scope.restore(previous); }
            assertEquals(false, scope.value(outer, true, "ReelsIsEligibleForInContentAds", true));
            assertEquals(true, scope.value(inner, true, "ReelsIsEligibleForInContentAds", true));
        } finally { scope.restore(initial); }
    }

    @Test public void anotherThreadDoesNotInheritTheAdvertisingOverride() throws Exception {
        ReelsAdOptions scope = new ReelsAdOptions();
        Object builder = new Object();
        Object previous = scope.enter(builder);
        ExecutorService worker = Executors.newSingleThreadExecutor();
        try {
            assertEquals(true, worker.submit(() -> {
                assertFalse(ReelsAdOptions.buildingLaunchOptions());
                return scope.value(builder, true, "ReelsIsEligibleForInContentAds", true);
            }).get());
            assertEquals(false, scope.value(builder, true, "ReelsIsEligibleForInContentAds", true));
        } finally { worker.shutdownNow(); scope.restore(previous); }
    }
}
