package ymg.pwcca.facebookgoodies;

import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Predicate;

/** Matches the shipped Shorts midcard enum, never post text or generic MIDCARD. */
final class ThreadsMidcardPolicy {
    static <T> List<T> filter(List<T> source,boolean enabled,Predicate<T> matches) {
        return enabled?FilterPolicy.partition(source,matches):source;
    }
    static boolean matches(Object item,Method midcard,Method payload,Method type) {
        try {
            if(item==null)return false;
            Object container=midcard.invoke(item);
            Object card=container==null?null:payload.invoke(container);
            Object kind=card==null?null:type.invoke(card);
            return kind instanceof Enum && "THREADS_MIDCARD".equals(((Enum<?>)kind).name());
        } catch(ReflectiveOperationException | RuntimeException failed) { return false; }
    }
}
