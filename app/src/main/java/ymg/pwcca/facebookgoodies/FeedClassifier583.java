package ymg.pwcca.facebookgoodies;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Reads verified category/marker contracts; never deserializes CSR content. */
final class FeedClassifier583 {
    private final Class<?> story;
    private final Class<?> multiAds;
    private final Class<?> categoryModel;
    private final Class<?> csrItem;
    private final Class<?> csrImplementation;
    private final Method edgeCategory;
    private final Method sponsoredData;
    private final Method csrExtras;
    private final Method csrCategory;
    private final Field cachedModel;

    FeedClassifier583(ClassLoader loader) throws ReflectiveOperationException {
        story = Class.forName("com.facebook.graphql.model.GraphQLStory", false, loader);
        multiAds = Class.forName("com.facebook.graphql.model.GraphQLFBMultiAdsFeedUnit", false, loader);
        categoryModel = Class.forName("X.3fX", false, loader);
        csrItem = Class.forName("X.5vF", false, loader);
        csrImplementation = Class.forName("X.2GR", false, loader);
        edgeCategory = categoryModel.getMethod("B9r");
        // In 583 A0S is a different GraphQLStory field; sponsored_data moved to A0g.
        sponsoredData = story.getMethod("A0g");
        csrExtras = csrItem.getMethod("B7p");
        csrCategory = csrExtras.getReturnType().getMethod("A0B");
        cachedModel = csrImplementation.getDeclaredField("A0L"); cachedModel.setAccessible(true);
        verifyCategory(edgeCategory); verifyCategory(csrCategory);
    }
    private static void verifyCategory(Method method) throws NoSuchMethodException {
        if (!method.getReturnType().getName().equals("com.crossapp.graphql.facebook.enums.GraphQLFeedStoryCategory"))
            throw new NoSuchMethodException("Unexpected feed category contract");
    }
    FilterPolicy.Action decide(Object primary, Object child, Options options) {
        String category = categoryOf(primary);
        boolean sponsored = sponsored(primary) || sponsored(child);
        return FilterPolicy.decide(category, sponsored, options.ads, options.suggestions, options.content);
    }
    private String categoryOf(Object value) {
        if (value == null) return null;
        try { return readCategory(value); }
        catch (ReflectiveOperationException | RuntimeException failed) { return null; }
    }
    private String readCategory(Object value) throws ReflectiveOperationException {
        if (categoryModel.isInstance(value)) return enumName(edgeCategory.invoke(value));
        if (!csrItem.isInstance(value)) return null;
        Object extras = csrExtras.invoke(value);
        return extras == null ? null : enumName(csrCategory.invoke(extras));
    }
    private static String enumName(Object value) { return value instanceof Enum ? ((Enum<?>) value).name() : null; }
    private boolean sponsored(Object value) {
        if (value == null) return false;
        if (multiAds.isInstance(value)) return true;
        try { return sponsoredModel(value); }
        catch (ReflectiveOperationException | RuntimeException failed) { return false; }
    }
    private boolean sponsoredModel(Object value) throws ReflectiveOperationException {
        if (story.isInstance(value)) return sponsoredData.invoke(value) != null;
        if (!csrImplementation.isInstance(value)) return false;
        Object cached = cachedModel.get(value);
        if (cached == value) return false;
        return multiAds.isInstance(cached) || (story.isInstance(cached) && sponsoredData.invoke(cached) != null);
    }
}
