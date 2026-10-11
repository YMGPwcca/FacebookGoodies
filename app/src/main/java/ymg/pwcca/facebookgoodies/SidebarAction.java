package ymg.pwcca.facebookgoodies;

final class SidebarAction {
    private SidebarAction() {}
    static boolean moreIcon(String icon) { return "DOTS_3_HORIZONTAL".equals(icon); }
    static boolean mediaMore(String icon, String surface) { return moreIcon(icon) && "ON_MEDIA".equals(surface); }
}
