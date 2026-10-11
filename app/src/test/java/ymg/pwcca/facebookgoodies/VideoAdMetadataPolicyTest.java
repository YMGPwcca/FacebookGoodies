package ymg.pwcca.facebookgoodies;

import org.junit.Test;
import static org.junit.Assert.*;

public final class VideoAdMetadataPolicyTest {
    @Test public void knownInstreamListsAndOptionalBreakNodesAreSeparated() {
        assertTrue(VideoAdMetadataPolicy.list(-1087806254, 162951166));
        assertTrue(VideoAdMetadataPolicy.scalar(-168910330, 162951166));
        assertTrue(VideoAdMetadataPolicy.scalar(559446729, 162951166));
        assertFalse(VideoAdMetadataPolicy.scalar(-1087806254, 162951166));
        assertFalse(VideoAdMetadataPolicy.list(-168910330, 162951166));
        assertFalse(VideoAdMetadataPolicy.list(559446729, 162951166));
    }

    @Test public void anAdTypeOnAnotherMediaFieldCannotRemoveTheOrganicField() {
        for (int field : new int[]{0, -738997328, 100346066, -1773963760, 1199916693}) {
            assertFalse(VideoAdMetadataPolicy.list(field, 162951166));
            assertFalse(VideoAdMetadataPolicy.scalar(field, 162951166));
        }
    }

    @Test public void exactFieldHashesStillRequireTheVerifiedAdModelType() {
        for (int field : new int[]{-1087806254, -168910330, 559446729}) {
            assertFalse(VideoAdMetadataPolicy.list(field, -541423194));
            assertFalse(VideoAdMetadataPolicy.scalar(field, -541423194));
            assertFalse(VideoAdMetadataPolicy.list(field, 0));
            assertFalse(VideoAdMetadataPolicy.scalar(field, 0));
        }
    }
}
