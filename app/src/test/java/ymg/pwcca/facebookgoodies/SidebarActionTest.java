package ymg.pwcca.facebookgoodies;

import org.junit.Test;
import static org.junit.Assert.*;

public class SidebarActionTest {
    @Test public void matchesActualVideoMoreGlyph() { assertTrue(SidebarAction.mediaMore("DOTS_3_HORIZONTAL", "ON_MEDIA")); }
    @Test public void doesNotMatchOtherActionIcons() {
        for(String name : new String[]{"LIKE","COMMENT","SHARE","BOOKMARK","MORE","DOTS_3_VERTICAL"})
            assertFalse(SidebarAction.mediaMore(name,"ON_MEDIA"));
    }
    @Test public void leavesNonMediaAndUnknownSurfacesAlone() {
        assertFalse(SidebarAction.mediaMore("DOTS_3_HORIZONTAL","ON_SURFACE"));
        assertFalse(SidebarAction.mediaMore("DOTS_3_HORIZONTAL","ON_COLOR"));
        assertFalse(SidebarAction.mediaMore(null,null));
    }
}
