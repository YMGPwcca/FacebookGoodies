package ymg.pwcca.facebookgoodies;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Explicit player origins from Facebook 582. Unknown and non-video surfaces stay untouched. */
final class VideoAdPolicy {
    enum Surface { NONE, REELS, VIDEO }
    private static final Set<String> REELS = new HashSet<>(Arrays.asList(
            "fb_shorts", "fb_shorts_tab", "fb_shorts_unified_player", "fb_shorts_midcard",
            "fb_shorts_bookmark", "fb_shorts_audio_aggregate", "shared_reels_landing_page",
            "reel_video_scoped_search_feed"));
    private static final Set<String> VIDEO = new HashSet<>(Arrays.asList(
            "newsfeed", "video_home", "watch_and_browse", "watch_creator_qp", "watch_widget",
            "video_scoped_search_feed", "vod_video_scoped_search_feed", "results_video_scoped"));

    static Surface surface(boolean enabled, String origin) {
        return surface(enabled, origin, null);
    }

    static Surface surface(boolean enabled, String origin, String suborigin) {
        if (!enabled || origin == null) return Surface.NONE;
        if (reelsViewer(origin, suborigin)) return Surface.REELS;
        if (REELS.contains(origin)) return Surface.REELS;
        return VIDEO.contains(origin) ? Surface.VIDEO : Surface.NONE;
    }

    private static boolean reelsViewer(String origin, String suborigin) {
        if ("fb_shorts_native_in_feed_unit".equals(suborigin)) return "newsfeed".equals(origin);
        if ("fb_shorts_native_watch_in_feed_unit".equals(suborigin)) return "video_home".equals(origin);
        if (!"fb_shorts_viewer".equals(suborigin)) return false;
        return "video_home".equals(origin) || "following_np_subtab".equals(origin) || "results".equals(origin);
    }
}
