package ymg.pwcca.facebookgoodies;

import java.lang.reflect.Method;
import java.util.List;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/** Reject only the native Threads promotion before Shorts items enter the pager. */
final class ThreadsVideoHooks585 {
    static void install(ClassLoader loader, HostState state, FacebookBuild build) {
        try { installResponse(loader,state,build); }
        catch (ReflectiveOperationException | RuntimeException | LinkageError failed) { state.failure("threads-video-response-install"); }
    }

    private static void installResponse(ClassLoader loader,HostState state,FacebookBuild build) throws ReflectiveOperationException {
        Class<?> edge=Class.forName("X.04rv",false,loader);
        Class<?> node=Class.forName("X.07vW",false,loader);
        Class<?> item=Class.forName("X.07i1",false,loader);
        Class<?> container=Class.forName("X.05pV",false,loader);
        Class<?> card=Class.forName("X.05rD",false,loader);
        Class<?> kind=Class.forName("X.05VV",false,loader);
        verifyKind(kind);
        Method unwrap=method(edge,"A00",node),value=method(node,"A00",item);
        Method midcard=method(item,"ANn",container),payload=method(container,"D7y",card),type=method(card,"CKZ",kind);
        Class<?> responseOwner=Class.forName("X.04sA",false,loader);
        Method response=method(responseOwner,"Cj1",List.class,Object.class);
        Method copy=Class.forName("com.google.common.collect.ImmutableList",false,loader).getMethod("copyOf",Iterable.class);
        XposedBridge.hookMethod(response,new XC_MethodHook(){
            @Override protected void afterHookedMethod(MethodHookParam param) {
                if(!state.options.content.threads || param.hasThrowable() || !(param.getResult() instanceof List))return;
                List<?> source=(List<?>)param.getResult();
                List<?> kept=ThreadsMidcardPolicy.filter(source,true,entry->matchesEdge(entry,edge,unwrap,value,midcard,payload,type));
                if(kept==source)return;
                try {
                    // Preserve cached originals and the host's immutable collection type.
                    Object replacement=copy.invoke(null,kept);
                    if(!(replacement instanceof List))return;
                    param.setResult(replacement);
                    int removed=source.size()-kept.size();
                    state.threads.addAndGet(removed);state.threadsVideo.addAndGet(removed);
                } catch(ReflectiveOperationException | RuntimeException failed) { state.failure("threads-video-list-rebuild"); }
            }
        });
        state.hooks.incrementAndGet();state.contentHooks.incrementAndGet();
    }

    private static boolean matchesEdge(Object entry,Class<?> edge,Method unwrap,Method value,Method midcard,Method payload,Method type) {
        if(!edge.isInstance(entry))return false;
        try {
            Object node=unwrap.invoke(entry);
            return node!=null && ThreadsMidcardPolicy.matches(value.invoke(node),midcard,payload,type);
        } catch(ReflectiveOperationException | RuntimeException failed) { return false; }
    }

    private static void verifyKind(Class<?> kind) throws NoSuchFieldException {
        if(!kind.isEnum())throw new NoSuchFieldException("Midcard enum contract mismatch");
        // Check enum identity at installation; no ordinal is hard-coded.
        for(Object value:kind.getEnumConstants())if("THREADS_MIDCARD".equals(((Enum<?>)value).name()))return;
        throw new NoSuchFieldException("Threads midcard type missing");
    }

    private static Method method(Class<?> owner,String name,Class<?> result,Class<?>... parameters) throws NoSuchMethodException {
        Method method=owner.getDeclaredMethod(name,parameters);
        if(method.getReturnType()!=result)throw new NoSuchMethodException("Threads video return contract mismatch");
        method.setAccessible(true);return method;
    }
}
