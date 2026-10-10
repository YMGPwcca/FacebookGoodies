package ymg.pwcca.facebookgoodies;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

final class FilterPolicy {
    enum Action { KEEP, AD, SUGGESTION, THREADS, STORIES_TOP, STORIES_MIDDLE, REELS }
    static boolean sponsoredCategory(String category) { return "SPONSORED".equals(category); }
    static boolean safeContainer(String category) {
        return "FB_SHORTS".equals(category) || "FB_SHORTS_FALLBACK".equals(category)
                || "MULTI_FB_STORIES_TRAY".equals(category);
    }
    static boolean suggestedCategory(String category) {
        return "ENGAGEMENT".equals(category) || "ENGAGEMENT_QP".equals(category);
    }
    static Action decide(String category, boolean sponsored, boolean ads, boolean suggestions) {
        return decide(category, sponsored, ads, suggestions, ContentFilters.OFF);
    }
    static Action decide(String category, boolean sponsored, boolean ads, boolean suggestions, ContentFilters content) {
        Action contentAction = contentAction(category, content);
        if (contentAction != Action.KEEP) return contentAction;
        if (safeContainer(category)) return Action.KEEP;
        if (sponsored || sponsoredCategory(category)) return ads ? Action.AD : Action.KEEP;
        if (suggestions && suggestedCategory(category)) return Action.SUGGESTION;
        return Action.KEEP;
    }
    private static Action contentAction(String category, ContentFilters content) {
        if (content.threads && "PROMOTION".equals(category)) return Action.THREADS;
        if (content.storiesMiddle && "MULTI_FB_STORIES_TRAY".equals(category)) return Action.STORIES_MIDDLE;
        if (content.reels && ("FB_SHORTS".equals(category) || "FB_SHORTS_FALLBACK".equals(category))) return Action.REELS;
        return Action.KEEP;
    }
    static <T> List<T> partition(List<T> source, Predicate<T> isAd) {
        List<T> kept = null;
        for (int i = 0; i < source.size(); i++) {
            T item = source.get(i);
            boolean remove = safelyClassify(item, isAd);
            if (remove && kept == null) kept = new ArrayList<>(source.subList(0, i));
            if (!remove && kept != null) kept.add(item);
        }
        return kept == null ? source : kept;
    }
    private static <T> boolean safelyClassify(T item, Predicate<T> classifier) {
        if (item == null) return false;
        try { return classifier.test(item); } catch (RuntimeException failure) { return false; }
    }
}
