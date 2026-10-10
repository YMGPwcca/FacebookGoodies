package ymg.pwcca.facebookgoodies;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public final class VideoAdPolicyTest {
    @Test public void routesReelsAndWatchResponsesToTheirOwnCounters() {
        assertEquals(VideoAdPolicy.Surface.REELS, VideoAdPolicy.surface(true, "fb_shorts_tab"));
        assertEquals(VideoAdPolicy.Surface.REELS, VideoAdPolicy.surface(true, "shared_reels_landing_page"));
        assertEquals(VideoAdPolicy.Surface.VIDEO, VideoAdPolicy.surface(true, "video_home"));
        assertEquals(VideoAdPolicy.Surface.VIDEO, VideoAdPolicy.surface(true, "newsfeed"));
        assertEquals(VideoAdPolicy.Surface.VIDEO, VideoAdPolicy.surface(true, "watch_and_browse"));
    }

    @Test public void disablingAdsLeavesAllVideoResponsesUntouched() {
        for (String origin : new String[]{"fb_shorts", "fb_shorts_tab", "video_home", "newsfeed", "watch_widget"})
            assertEquals(VideoAdPolicy.Surface.NONE, VideoAdPolicy.surface(false, origin));
    }

    @Test public void sharedPlayerCallbacksDoNotBlockStoriesOrOtherSurfaces() {
        for (String origin : new String[]{"facebook_story", "stories", "biz_story", "messenger_story",
                "marketplace", "instant_shopping", "games", "composer"})
            assertEquals(VideoAdPolicy.Surface.NONE, VideoAdPolicy.surface(true, origin));
    }

    @Test public void missingAndNewOriginsFailOpenRatherThanGuessingByPrefix() {
        for (String origin : new String[]{null, "", "unknown", "fb_shorts_new_surface", "video_editing_gallery",
                "fb_shorts_replace_audio_for_boost", "VIDEO_HOME"})
            assertEquals(VideoAdPolicy.Surface.NONE, VideoAdPolicy.surface(true, origin));
    }
}
