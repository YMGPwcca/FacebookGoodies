package ymg.pwcca.facebookgoodies;

/** Exact GraphQLMedia instream-break fields in Facebook 582. */
final class VideoAdMetadataPolicy {
    private static final int AD_BREAK_TYPE = 162951166;

    static boolean list(int field, int type) {
        return type == AD_BREAK_TYPE && field == -1087806254;
    }

    static boolean scalar(int field, int type) {
        return type == AD_BREAK_TYPE && (field == -168910330 || field == 559446729);
    }
}
