package ymg.pwcca.facebookgoodies;

enum FacebookBuild {
    FACEBOOK_582, FACEBOOK_583, UNSUPPORTED;

    static FacebookBuild identify(String versionName, long versionCode) {
        if ("582.0.0.50.54".equals(versionName) && versionCode == 475417129L)
            return FACEBOOK_582;
        if ("583.0.0.39.55".equals(versionName) && versionCode == 475614366L)
            return FACEBOOK_583;
        return UNSUPPORTED;
    }
}
