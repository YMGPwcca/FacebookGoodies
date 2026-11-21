package ymg.pwcca.facebookgoodies;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/** Drops sponsored thread rows from the Messenger inbox. */
final class MessengerInboxAds {
    private MessengerInboxAds() { }

    static int install(ClassLoader loader, MessengerBuild build, BooleanSupplier enabled, AtomicInteger removed)
            throws ReflectiveOperationException {
        Class<?> supplier = Class.forName(
                "com.facebook.messaging.msys.threadlist.plugins.core.itemsupplier.ThreadListItemSupplierImplementation",
                false, loader);
        Class<?> immutableList = Class.forName("com.google.common.collect.ImmutableList", false, loader);
        Class<?> summary = Class.forName("com.facebook.messaging.model.threads.ThreadSummary", false, loader);
        Class<?> adContext = Class.forName("com.facebook.messaging.model.threads.ads.AdContextData", false, loader);

        Method items = null;
        for (Method method : supplier.getDeclaredMethods()) {
            if (method.getParameterCount() != 0 || method.getReturnType() != immutableList) continue;
            if (items != null) throw new NoSuchMethodException("Messenger inbox item builder is ambiguous");
            items = method;
        }
        if (items == null) throw new NoSuchMethodException("Messenger inbox item builder not found");

        Field ad = null;
        for (Field field : summary.getDeclaredFields()) {
            if (field.getType() != adContext) continue;
            if (ad != null) throw new NoSuchFieldException("Messenger ad context field is ambiguous");
            ad = field;
        }
        if (ad == null) throw new NoSuchFieldException("Messenger ad context field not found");

        ad.setAccessible(true);
        Method copyOf = immutableList.getMethod("copyOf", Iterable.class);
        if (build.is588()) {
            installRenderedRows(loader, build, summary, items, ad, copyOf, enabled, removed);
            return 1;
        }
        Field rows = null;
        for (Field field : supplier.getDeclaredFields()) {
            if (field.getType() != immutableList) continue;
            if (rows != null) throw new NoSuchFieldException("Messenger inbox row list is ambiguous");
            rows = field;
        }
        if (rows == null) throw new NoSuchFieldException("Messenger inbox row list not found");
        rows.setAccessible(true);
        Field rowField = rows;
        Field adField = ad;

        XposedBridge.hookMethod(items, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam param) {
                if (!enabled.getAsBoolean()) return;
                try {
                    Object current = rowField.get(param.thisObject);
                    if (!(current instanceof List<?>)) return;
                    List<?> list = (List<?>) current;
                    if (list.isEmpty()) return;
                    List<Object> kept = new ArrayList<>(list.size());
                    int dropped = 0;
                    for (Object row : list) {
                        if (summary.isInstance(row) && adField.get(row) != null) {
                            dropped++;
                            continue;
                        }
                        kept.add(row);
                    }
                    if (dropped == 0) return;
                    rowField.set(param.thisObject, copyOf.invoke(null, kept));
                    removed.addAndGet(dropped);
                } catch (ReflectiveOperationException | RuntimeException failure) {
                    XposedBridge.log("FacebookGoodies: Messenger inbox ad filter failed: "
                            + failure.getClass().getSimpleName());
                }
            }
        });
        return 1;
    }

    private static void installRenderedRows(ClassLoader loader, MessengerBuild build, Class<?> summaryType, Method items,
                                            Field adField, Method copyOf, BooleanSupplier enabled,
                                            AtomicInteger removed) throws ReflectiveOperationException {
        Class<?> threadRow = Class.forName(MessengerHostBindings.type588(build, "X.2Cr"), false, loader);
        Class<?> folderRow = Class.forName(MessengerHostBindings.type588(build, "X.35F"), false, loader);
        Field threadSummary = threadRow.getDeclaredField("A03");
        Field folderSummary = folderRow.getDeclaredField("A04");
        if (threadSummary.getType() != summaryType || folderSummary.getType() != summaryType)
            throw new NoSuchFieldException("Messenger inbox item summary type changed");
        threadSummary.setAccessible(true);
        folderSummary.setAccessible(true);

        // The 588 supplier also uses its input list for pagination and profile prefetch.
        XposedBridge.hookMethod(items, new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                if (!enabled.getAsBoolean() || !(param.getResult() instanceof List<?>)) return;
                List<?> original = (List<?>) param.getResult();
                List<Object> kept = new ArrayList<>(original.size());
                try {
                    for (Object row : original) {
                        Object summary = null;
                        if (threadRow.isInstance(row)) summary = threadSummary.get(row);
                        else if (folderRow.isInstance(row)) summary = folderSummary.get(row);
                        if (summary == null || adField.get(summary) == null) kept.add(row);
                    }
                    int dropped = original.size() - kept.size();
                    if (dropped == 0) return;
                    param.setResult(copyOf.invoke(null, kept));
                    removed.addAndGet(dropped);
                } catch (ReflectiveOperationException | RuntimeException failure) {
                    XposedBridge.log("FacebookGoodies: Messenger inbox ad filter failed: "
                            + failure.getClass().getSimpleName());
                }
            }
        });
    }
}
