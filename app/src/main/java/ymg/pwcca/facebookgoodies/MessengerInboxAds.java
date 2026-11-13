package ymg.pwcca.facebookgoodies;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/** Drops sponsored thread rows from the Messenger inbox before items are built. */
final class MessengerInboxAds {
    private MessengerInboxAds() { }

    static int install(ClassLoader loader, BooleanSupplier enabled, AtomicInteger removed)
            throws ReflectiveOperationException {
        Class<?> supplier = Class.forName(
                "com.facebook.messaging.msys.threadlist.plugins.core.itemsupplier.ThreadListItemSupplierImplementation",
                false, loader);
        Class<?> immutableList = Class.forName("com.google.common.collect.ImmutableList", false, loader);
        Class<?> summary = Class.forName("com.facebook.messaging.model.threads.ThreadSummary", false, loader);
        Class<?> adContext = Class.forName("com.facebook.messaging.model.threads.ads.AdContextData", false, loader);

        Field rows = null;
        for (Field field : supplier.getDeclaredFields()) {
            if (field.getType() != immutableList) continue;
            if (rows != null) throw new NoSuchFieldException("Messenger inbox row list is ambiguous");
            rows = field;
        }
        if (rows == null) throw new NoSuchFieldException("Messenger inbox row list not found");

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

        rows.setAccessible(true);
        ad.setAccessible(true);
        Method copyOf = immutableList.getMethod("copyOf", Iterable.class);
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
}
