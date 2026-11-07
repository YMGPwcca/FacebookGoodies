package ymg.pwcca.facebookgoodies;

enum MessengerBuild {
    MESSENGER_584_1, MESSENGER_585, MESSENGER_586, UNSUPPORTED;

    static MessengerBuild identify(String versionName, long versionCode) {
        if ("584.1.0.61.98".equals(versionName) && versionCode == 346817021L)
            return MESSENGER_584_1;
        if ("585.0.0.53.94".equals(versionName) && versionCode == 347015062L)
            return MESSENGER_585;
        if ("586.0.0.55.92".equals(versionName) && versionCode == 347215273L)
            return MESSENGER_586;
        return UNSUPPORTED;
    }
}
