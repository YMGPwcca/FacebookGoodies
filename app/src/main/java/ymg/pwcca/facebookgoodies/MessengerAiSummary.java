package ymg.pwcca.facebookgoodies;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/** Removes the in-chat "Summarize N unread messages" AI summary pills. */
final class MessengerAiSummary {
    private MessengerAiSummary() { }

    static int install(ClassLoader loader, MessengerBuild build, BooleanSupplier enabled,
                       AtomicInteger hidden) throws ReflectiveOperationException {
        XC_MethodHook skip = new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam param) {
                if (!enabled.getAsBoolean()) return;
                param.setResult(null);
                hidden.incrementAndGet();
            }
        };
        Class<?> handler = Class.forName(MessengerHostBindings.summaryPillHandler(build), false, loader);
        Method show = null;
        for (Method method : handler.getDeclaredMethods()) {
            if (!Modifier.isPrivate(method.getModifiers()) || method.getReturnType() != void.class
                    || method.getParameterCount() != 1) continue;
            if (show != null) throw new NoSuchMethodException("AI summary pill method is ambiguous");
            show = method;
        }
        if (show == null) throw new NoSuchMethodException("AI summary pill method not found");
        XposedBridge.hookMethod(show, skip);

        Class<?> jump = Class.forName(MessengerHostBindings.summaryJumpPill(build), false, loader);
        Method run = jump.getDeclaredMethod("run");
        XposedBridge.hookMethod(run, skip);
        return 2;
    }
}
