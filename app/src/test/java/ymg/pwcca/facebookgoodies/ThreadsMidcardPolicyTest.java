package ymg.pwcca.facebookgoodies;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class ThreadsMidcardPolicyTest {
    enum Kind { THREADS_MIDCARD, USER_SURVEY, NARRATIVE_THREAD, MIDCARD, UNSET_OR_UNRECOGNIZED_ENUM_VALUE }
    public static class Item { final Container value; Item(Container value){this.value=value;} public Container AEn(){return value;} }
    public static class Container { final Card value; Container(Card value){this.value=value;} public Card BPu(){return value;} }
    public static class Card { final Object value; Card(Object value){this.value=value;} public Object B8H(){return value;} }
    private boolean matches(Object item)throws Exception {
        return ThreadsMidcardPolicy.matches(item,Item.class.getMethod("AEn"),Container.class.getMethod("BPu"),Card.class.getMethod("B8H"));
    }
    private Item item(Object kind){return new Item(new Container(new Card(kind)));}
    @Test public void readsOnlyTheTypedThreadsPromotion()throws Exception {
        assertTrue(matches(item(Kind.THREADS_MIDCARD)));
        for(Kind kind:Kind.values())if(kind!=Kind.THREADS_MIDCARD)assertFalse(matches(item(kind)));
        assertFalse(matches(item("THREADS_MIDCARD")));
    }
    @Test public void missingNodesAndOrganicShapesRemainVisible()throws Exception {
        assertFalse(matches(null));assertFalse(matches(new Object()));assertFalse(matches(new Item(null)));
        assertFalse(matches(new Item(new Container(null))));assertFalse(matches(item(null)));
    }
    @Test public void getterFailureRemainsVisible()throws Exception {
        assertFalse(matches(new Item(null){@Override public Container AEn(){throw new IllegalStateException();}}));
    }
    @Test public void neverInspectsDisplayText()throws Exception {
        assertFalse(matches(item(new Object(){@Override public String toString(){throw new AssertionError("No post text or free-form values");}})));
    }
    @Test public void listFilteringPreservesOrderOriginalAndUnknownItems() {
        Item video=item(Kind.USER_SURVEY),threads=item(Kind.THREADS_MIDCARD),unknown=item(null);
        List<Item> source=Arrays.asList(video,threads,unknown,null,threads,video);
        List<Item> kept=ThreadsMidcardPolicy.filter(source,true,value->{try{return matches(value);}catch(Exception failed){throw new IllegalStateException(failed);}});
        assertEquals(Arrays.asList(video,unknown,null,video),kept);assertEquals(6,source.size());
    }
    @Test public void disabledAndUnmatchedListsRetainIdentity() {
        List<Item> source=Arrays.asList(item(Kind.USER_SURVEY));
        assertSame(source,ThreadsMidcardPolicy.filter(source,false,value->{throw new AssertionError();}));
        assertSame(source,ThreadsMidcardPolicy.filter(source,true,value->false));
    }
    @Test public void unreadableListItemFailsOpenWhileThreadsIsRemoved() {
        List<String> source=Arrays.asList("broken","THREADS_MIDCARD");
        assertEquals(Arrays.asList("broken"),ThreadsMidcardPolicy.filter(source,true,value->{if(value.equals("broken"))throw new IllegalStateException();return true;}));
    }
}
