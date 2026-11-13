package ymg.pwcca.facebookgoodies;

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/** Adds a per-chat read-receipt switch to the conversation menus. */
final class MessengerChatMenu {
    private static final String FUNCTION0 = "kotlin.jvm.functions.Function0";
    private static final String SUMMARY = "com.facebook.messaging.model.threads.ThreadSummary";
    private static final String TRACKABLE = "com.facebook.messaging.inbox.units.InboxTrackableItem";
    private static final String IMMUTABLE_LIST = "com.google.common.collect.ImmutableList";
    private static final String SHEET = "com.facebook.messaging.threadlist.threaditemmenu.bottomsheet.SimpleUserControlBottomSheetDialogFragment";
    private static final int RECEIPT_ITEM_ID = -2147480000;
    private static final ThreadLocal<Object> opening = new ThreadLocal<>();

    private final ClassLoader loader;
    private final MessengerBuild build;
    private final MessengerReadControls readControls;
    private final BooleanSupplier hidden;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final AtomicInteger shown = new AtomicInteger();
    private final AtomicInteger toggles = new AtomicInteger();
    private final AtomicInteger errors = new AtomicInteger();
    private final Map<Object, String> threads = new WeakHashMap<>();
    private final Map<Object, Object> injected = new WeakHashMap<>();
    private final Map<Object, String> sheetThreads = new WeakHashMap<>();
    private final Map<Object, Object> sheetFragments = new WeakHashMap<>();
    private final Map<Object, Object> sheetComponents = new WeakHashMap<>();
    private Field sheetBuilder;
    private final Set<Class<?>> callbackHooks = Collections.synchronizedSet(new HashSet<>());
    private Plan plan;
    private BottomShape bottom;

    MessengerChatMenu(ClassLoader loader, MessengerBuild build,
                      MessengerReadControls readControls, BooleanSupplier hidden) {
        this.loader = loader;
        this.build = build;
        this.readControls = readControls;
        this.hidden = hidden;
    }

    int install() {
        int installed = installPopup();
        installed += installBottomSheet();
        return installed;
    }

    private int installPopup() {
        int installed = 0;
        try {
            Class<?> function0 = Class.forName(FUNCTION0, false, loader);
            Class<?> fragment = Class.forName(
                    "com.facebook.messaging.threadlist.threaditemmenu.InboxThreadContextMenuFragment",
                    false, loader);
            Field items = null;
            for (Field field : fragment.getDeclaredFields()) {
                if (field.getType() != List.class) continue;
                if (items != null) throw new NoSuchFieldException("context menu item list is ambiguous");
                items = field;
            }
            if (items == null) throw new NoSuchFieldException("context menu item list not found");
            items.setAccessible(true);
            Method buildList = null;
            for (Method method : fragment.getDeclaredMethods()) {
                if (method.getParameterCount() != 0 || method.getReturnType() != List.class) continue;
                if (buildList != null) throw new NoSuchMethodException("context menu builder is ambiguous");
                buildList = method;
            }
            if (buildList == null) throw new NoSuchMethodException("context menu builder not found");

            Class<?> controller = Class.forName(MessengerHostBindings.menuController(this.build), false, loader);
            Method opener = null;
            for (Method method : controller.getDeclaredMethods()) {
                if (!Modifier.isStatic(method.getModifiers()) || method.getReturnType() != void.class) continue;
                boolean view = false;
                boolean trackable = false;
                boolean summary = false;
                for (Class<?> parameter : method.getParameterTypes()) {
                    if (parameter == View.class) view = true;
                    else if (TRACKABLE.equals(parameter.getName())) trackable = true;
                    else if (SUMMARY.equals(parameter.getName())) summary = true;
                }
                if (!view || !trackable || !summary) continue;
                if (opener != null) throw new NoSuchMethodException("thread menu opener is ambiguous");
                opener = method;
            }
            if (opener == null) throw new NoSuchMethodException("thread menu opener not found");

            final Field itemField = items;
            final Class<?> functionType = function0;
            XposedBridge.hookMethod(opener, new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) {
                    for (Object argument : param.args)
                        if (argument != null && SUMMARY.equals(argument.getClass().getName()))
                            opening.set(argument);
                    if (opening.get() != null && shown.get() < 3)
                        XposedBridge.log("FacebookGoodies: Messenger thread menu opened");
                }

                @Override protected void afterHookedMethod(MethodHookParam param) { opening.remove(); }
            });
            installed++;

            XposedBridge.hookAllConstructors(fragment, new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam param) {
                    Object summary = opening.get();
                    if (summary == null) return;
                    String thread = readControls.threadKeyOf(summary);
                    if (thread == null) {
                        errors.incrementAndGet();
                        return;
                    }
                    threads.put(param.thisObject, thread);
                }
            });
            installed++;

            XposedBridge.hookMethod(buildList, new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) {
                    if (!hidden.getAsBoolean()) return;
                    Object fragmentInstance = param.thisObject;
                    String thread = threads.get(fragmentInstance);
                    if (thread == null) return;
                    try {
                        Object current = itemField.get(fragmentInstance);
                        if (!(current instanceof List<?>)) return;
                        @SuppressWarnings("unchecked")
                        List<Object> items = (List<Object>) current;
                        Object previous = injected.remove(fragmentInstance);
                        if (previous != null) items.remove(previous);
                        if (items.isEmpty()) return;
                        Plan resolved = plan(items.get(0));
                        Object callback = Proxy.newProxyInstance(loader, new Class<?>[]{functionType},
                                (proxy, method, args) -> {
                                    if ("invoke".equals(method.getName())) {
                                        main.post(() -> toggle(thread));
                                        return null;
                                    }
                                    if ("getArity".equals(method.getName())) return 0;
                                    if ("hashCode".equals(method.getName())) return System.identityHashCode(proxy);
                                    if ("equals".equals(method.getName()))
                                        return args != null && args.length == 1 && proxy == args[0];
                                    return "FacebookGoodies read receipt";
                                });
                        Object item = resolved.constructor.newInstance(
                                resolved.arguments(items.get(0), titleFor(thread), callback));
                        items.add(item);
                        injected.put(fragmentInstance, item);
                        shown.incrementAndGet();
                        if (shown.get() <= 6)
                            XposedBridge.log("FacebookGoodies: Messenger popup receipt item shown state="
                                    + readControls.receiptsAllowedFor(thread));
                    } catch (ReflectiveOperationException | RuntimeException failure) {
                        errors.incrementAndGet();
                        XposedBridge.log("FacebookGoodies: Messenger chat menu item failed: "
                                + failure.getClass().getSimpleName());
                    }
                }
            });
            installed++;
            XposedBridge.log("FacebookGoodies: Messenger popup menu hooks=" + installed);
        } catch (ReflectiveOperationException | RuntimeException failure) {
            errors.incrementAndGet();
            XposedBridge.log("FacebookGoodies: Messenger popup menu unavailable: "
                    + failure.getClass().getSimpleName());
        }
        return installed;
    }

    private int installBottomSheet() {
        try {
            Class<?> sheet = Class.forName(SHEET, false, loader);
            Field[] fields = sheet.getDeclaredFields();
            if (fields.length != 1) throw new NoSuchFieldException("bottom sheet builder field changed");
            Field builder = fields[0];
            builder.setAccessible(true);
            Method render = null;
            for (Method method : sheet.getDeclaredMethods()) {
                if (method.getParameterCount() != 1 || method.getReturnType() == void.class
                        || method.getReturnType() == boolean.class) continue;
                if (render != null) throw new NoSuchMethodException("bottom sheet render is ambiguous");
                render = method;
            }
            if (render == null) throw new NoSuchMethodException("bottom sheet render not found");
            final Field builderField = builder;
            sheetBuilder = builder;
            XposedBridge.hookMethod(render, new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) {
                    if (!hidden.getAsBoolean()) return;
                    try {
                        Object value = builderField.get(param.thisObject);
                        if (value == null) return;
                        BottomShape shape = bottom(value);
                        Object summary = shape.summary.get(value);
                        if (summary == null) return;
                        String thread = readControls.threadKeyOf(summary);
                        if (thread == null) return;
                        Object current = shape.items.get(value);
                        if (!(current instanceof List<?>)) return;
                        List<?> list = (List<?>) current;
                        List<Object> kept = new ArrayList<>(list.size());
                        Object template = null;
                        for (Object item : list) {
                            if (shape.id.getInt(item) == RECEIPT_ITEM_ID) continue;
                            if (template == null) template = item;
                            kept.add(item);
                        }
                        if (template == null) return;
                        Object item = shape.clone(template, titleFor(thread));
                        kept.add(item);
                        shape.items.set(value, shape.copyOf.invoke(null, kept));
                        Object callback = shape.callback.get(value);
                        if (callback != null) {
                            sheetThreads.put(callback, thread);
                            sheetFragments.put(callback, param.thisObject);
                            hookCallback(callback.getClass(), shape.itemType);
                        }
                        shown.incrementAndGet();
                        if (shown.get() <= 6)
                            XposedBridge.log("FacebookGoodies: Messenger bottom receipt item shown state="
                                    + readControls.receiptsAllowedFor(thread));
                    } catch (ReflectiveOperationException | RuntimeException failure) {
                        errors.incrementAndGet();
                        XposedBridge.log("FacebookGoodies: Messenger bottom menu item failed: "
                                + failure.getClass().getSimpleName());
                    }
                }

                @Override protected void afterHookedMethod(MethodHookParam param) {
                    Object result = param.getResult();
                    if (result != null) sheetComponents.put(param.thisObject, result);
                }
            });
            XposedBridge.log("FacebookGoodies: Messenger bottom menu hook installed");
            return 1;
        } catch (ReflectiveOperationException | RuntimeException failure) {
            errors.incrementAndGet();
            XposedBridge.log("FacebookGoodies: Messenger bottom menu unavailable: "
                    + failure.getClass().getSimpleName());
            return 0;
        }
    }

    private void hookCallback(Class<?> type, Class<?> itemType) {
        if (!callbackHooks.add(type)) return;
        for (Method method : type.getDeclaredMethods()) {
            if (method.getParameterCount() != 1 || method.getReturnType() != void.class) continue;
            Class<?> parameter = method.getParameterTypes()[0];
            if (parameter == Object.class || !parameter.isAssignableFrom(itemType)) continue;
            XposedBridge.hookMethod(method, new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) {
                    if (!hidden.getAsBoolean() || !(param.args[0] instanceof Object)) return;
                    Object item = param.args[0];
                    String thread = sheetThreads.get(param.thisObject);
                    if (thread == null || item.getClass() != itemType) return;
                    try {
                        if (bottom.id.getInt(item) != RECEIPT_ITEM_ID) return;
                        param.setResult(null);
                        toggle(thread);
                        refreshSheet(sheetFragments.get(param.thisObject), thread);
                    } catch (IllegalAccessException | RuntimeException failure) {
                        errors.incrementAndGet();
                    }
                }
            });
        }
    }

    /** Rebuilds the sheet component so the label changes without closing the sheet. */
    private void refreshSheet(Object fragment, String thread) {
        if (fragment == null || bottom == null || sheetBuilder == null) return;
        try {
            Object builder = sheetBuilder.get(fragment);
            if (builder == null) return;
            Object current = bottom.items.get(builder);
            if (!(current instanceof List<?>)) return;
            List<Object> kept = new ArrayList<>();
            Object template = null;
            for (Object item : (List<?>) current) {
                if (bottom.id.getInt(item) == RECEIPT_ITEM_ID) continue;
                if (template == null) template = item;
                kept.add(item);
            }
            if (template == null) return;
            kept.add(bottom.clone(template, titleFor(thread)));
            Object list = bottom.copyOf.invoke(null, kept);
            bottom.items.set(builder, list);
            Object component = sheetComponents.get(fragment);
            if (component == null) return;
            Object rebuilt = rebuild(component, list);
            if (rebuilt != null && applyComponent(fragment, rebuilt, component))
                sheetComponents.put(fragment, rebuilt);
        } catch (ReflectiveOperationException | RuntimeException failure) {
            errors.incrementAndGet();
            XposedBridge.log("FacebookGoodies: Messenger bottom menu refresh failed: "
                    + failure.getClass().getSimpleName());
        }
    }

    private Object rebuild(Object component, Object list) throws ReflectiveOperationException {
        Class<?> type = component.getClass();
        Field[] fields = type.getDeclaredFields();
        for (Field field : fields) field.setAccessible(true);
        Constructor<?> constructor = null;
        for (Constructor<?> candidate : type.getDeclaredConstructors()) {
            if (candidate.getParameterCount() != fields.length) continue;
            if (constructor != null) throw new NoSuchMethodException("bottom component constructor is ambiguous");
            constructor = candidate;
        }
        if (constructor == null) throw new NoSuchMethodException("bottom component constructor not found");
        Class<?>[] parameters = constructor.getParameterTypes();
        Object[] arguments = new Object[parameters.length];
        Map<Class<?>, Integer> used = new LinkedHashMap<>();
        for (int index = 0; index < parameters.length; index++) {
            Class<?> parameter = parameters[index];
            if (IMMUTABLE_LIST.equals(parameter.getName())) {
                arguments[index] = list;
                continue;
            }
            int wanted = used.getOrDefault(parameter, 0);
            Field match = null;
            int seen = 0;
            for (Field field : fields) {
                if (field.getType() != parameter) continue;
                if (seen++ == wanted) { match = field; break; }
            }
            if (match == null) throw new NoSuchFieldException("bottom component argument not found");
            used.put(parameter, wanted + 1);
            arguments[index] = match.get(component);
        }
        return constructor.newInstance(arguments);
    }

    private boolean applyComponent(Object fragment, Object component, Object expected) {
        View root = null;
        try {
            Object view = fragment.getClass().getMethod("getView").invoke(fragment);
            if (view instanceof View) root = (View) view;
        } catch (ReflectiveOperationException | RuntimeException failure) {
            errors.incrementAndGet();
        }
        if (root == null) return false;
        if (applyToLitho(root, component, expected)) return true;
        XposedBridge.log("FacebookGoodies: Messenger bottom menu view not found");
        return false;
    }

    private static boolean applyToLitho(View view, Object component, Object expected) {
        Field tree = componentTreeField(view);
        if (tree != null) {
            try {
                Object value = tree.get(view);
                if (value != null && componentMatches(value, expected)) {
                    for (Method method : view.getClass().getMethods()) {
                        if (method.getParameterCount() != 1 || method.getReturnType() != void.class) continue;
                        if (!method.getName().startsWith("A1")) continue;
                        Class<?> parameter = method.getParameterTypes()[0];
                        if (parameter == Object.class || !parameter.isInstance(component)) continue;
                        try {
                            method.invoke(view, component);
                            return true;
                        } catch (ReflectiveOperationException | RuntimeException ignored) { }
                    }
                }
            } catch (IllegalAccessException | RuntimeException ignored) { }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++)
                if (applyToLitho(group.getChildAt(index), component, expected)) return true;
        }
        return false;
    }

    private static Field componentTreeField(View view) {
        for (Field field : view.getClass().getFields()) {
            if (!"com.facebook.litho.ComponentTree".equals(field.getType().getName())) continue;
            return field;
        }
        for (Field field : view.getClass().getDeclaredFields()) {
            if (!"com.facebook.litho.ComponentTree".equals(field.getType().getName())) continue;
            field.setAccessible(true);
            return field;
        }
        return null;
    }

    private static boolean componentMatches(Object tree, Object expected) {
        for (Field field : tree.getClass().getDeclaredFields()) {
            if (field.getType() == Object.class || field.getType().isPrimitive()) continue;
            if (!field.getType().isAssignableFrom(expected.getClass())) continue;
            try {
                field.setAccessible(true);
                Object root = field.get(tree);
                if (root == expected || (root != null && root.getClass() == expected.getClass())) return true;
            } catch (IllegalAccessException | RuntimeException ignored) { }
        }
        return false;
    }

    int shownCount() { return shown.get(); }
    int toggleCount() { return toggles.get(); }
    int errorCount() { return errors.get(); }

    private String titleFor(String thread) {
        return readControls.receiptsAllowedFor(thread) ? "Read receipts: On" : "Read receipts: Off";
    }

    private void toggle(String thread) {
        boolean next = !readControls.receiptsAllowedFor(thread);
        readControls.setReceiptsAllowed(thread, next);
        toggles.incrementAndGet();
        XposedBridge.log("FacebookGoodies: Messenger chat menu toggle state=" + next);
    }

    private Plan plan(Object template) throws ReflectiveOperationException {
        Class<?> type = template.getClass();
        if (plan != null && plan.type == type) return plan;
        Constructor<?> constructor = null;
        for (Constructor<?> candidate : type.getDeclaredConstructors()) {
            int strings = 0;
            boolean callback = false;
            for (Class<?> parameter : candidate.getParameterTypes()) {
                if (parameter == String.class) strings++;
                else if (FUNCTION0.equals(parameter.getName())) callback = true;
            }
            if (strings != 2 || !callback) continue;
            if (constructor != null) throw new NoSuchMethodException("context menu item shape is ambiguous");
            constructor = candidate;
        }
        if (constructor == null) throw new NoSuchMethodException("context menu item shape not found");
        constructor.setAccessible(true);
        Field[] fields = type.getDeclaredFields();
        for (Field field : fields) field.setAccessible(true);
        Plan resolved = new Plan(type, constructor, fields);
        plan = resolved;
        return resolved;
    }

    private BottomShape bottom(Object builder) throws ReflectiveOperationException {
        Class<?> type = builder.getClass();
        if (bottom != null && bottom.builderType == type) return bottom;
        Field items = null;
        Field summary = null;
        for (Field field : type.getDeclaredFields()) {
            if (IMMUTABLE_LIST.equals(field.getType().getName())) {
                if (items != null) throw new NoSuchFieldException("bottom menu list is ambiguous");
                items = field;
            } else if (SUMMARY.equals(field.getType().getName())) {
                if (summary != null) throw new NoSuchFieldException("bottom menu thread is ambiguous");
                summary = field;
            }
        }
        if (items == null || summary == null)
            throw new NoSuchFieldException("bottom menu fields not found");
        items.setAccessible(true);
        summary.setAccessible(true);
        Object current = items.get(builder);
        if (!(current instanceof List<?>) || ((List<?>) current).isEmpty())
            throw new NoSuchFieldException("bottom menu items not available");
        Class<?> itemType = ((List<?>) current).get(0).getClass();
        Field id = null;
        Field title = null;
        Field[] itemFields = itemType.getDeclaredFields();
        for (Field field : itemFields) {
            field.setAccessible(true);
            if (field.getType() == int.class) {
                if (id != null) throw new NoSuchFieldException("bottom menu id is ambiguous");
                id = field;
            } else if (field.getType() == CharSequence.class) {
                if (title != null) throw new NoSuchFieldException("bottom menu title is ambiguous");
                title = field;
            }
        }
        if (id == null || title == null) throw new NoSuchFieldException("bottom menu item shape not found");
        Constructor<?> constructor = null;
        for (Constructor<?> candidate : itemType.getDeclaredConstructors()) {
            if (candidate.getParameterCount() != itemFields.length) continue;
            if (constructor != null) throw new NoSuchMethodException("bottom menu item constructor is ambiguous");
            constructor = candidate;
        }
        if (constructor == null) throw new NoSuchMethodException("bottom menu item constructor not found");
        constructor.setAccessible(true);
        Field callback = null;
        for (Field field : type.getDeclaredFields()) {
            if (field.getType() == items.getType()) continue;
            for (Method method : field.getType().getDeclaredMethods()) {
                if (method.getParameterCount() != 1 || method.getReturnType() != void.class) continue;
                if (method.getParameterTypes()[0] == Object.class) continue;
                if (!method.getParameterTypes()[0].isAssignableFrom(itemType)) continue;
                if (callback != null) continue;
                callback = field;
            }
        }
        if (callback == null) throw new NoSuchFieldException("bottom menu callback not found");
        callback.setAccessible(true);
        Method copyOf = Class.forName(IMMUTABLE_LIST, false, loader).getMethod("copyOf", Iterable.class);
        BottomShape resolved = new BottomShape(type, items, summary, callback, itemType,
                id, title, constructor, itemFields, copyOf);
        bottom = resolved;
        return resolved;
    }

    private static final class Plan {
        final Class<?> type;
        final Constructor<?> constructor;
        final Field[] fields;

        Plan(Class<?> type, Constructor<?> constructor, Field[] fields) {
            this.type = type;
            this.constructor = constructor;
            this.fields = fields;
        }

        Object[] arguments(Object template, String title, Object callback) throws ReflectiveOperationException {
            Class<?>[] parameters = constructor.getParameterTypes();
            Object[] arguments = new Object[parameters.length];
            Map<Class<?>, Integer> used = new LinkedHashMap<>();
            int strings = 0;
            for (int index = 0; index < parameters.length; index++) {
                Class<?> parameter = parameters[index];
                if (parameter == String.class) {
                    arguments[index] = strings++ == 0 ? title : null;
                } else if (FUNCTION0.equals(parameter.getName())) {
                    arguments[index] = callback;
                } else {
                    int wanted = used.getOrDefault(parameter, 0);
                    Field match = null;
                    int seen = 0;
                    for (Field field : fields) {
                        if (field.getType() != parameter) continue;
                        if (seen++ == wanted) { match = field; break; }
                    }
                    if (match == null) throw new NoSuchFieldException("context menu argument not found");
                    used.put(parameter, wanted + 1);
                    arguments[index] = match.get(template);
                }
            }
            return arguments;
        }
    }

    private static final class BottomShape {
        final Class<?> builderType;
        final Field items;
        final Field summary;
        final Field callback;
        final Class<?> itemType;
        final Field id;
        final Field title;
        final Constructor<?> constructor;
        final Field[] fields;
        final Method copyOf;

        BottomShape(Class<?> builderType, Field items, Field summary, Field callback, Class<?> itemType,
                    Field id, Field title, Constructor<?> constructor, Field[] fields, Method copyOf) {
            this.builderType = builderType;
            this.items = items;
            this.summary = summary;
            this.callback = callback;
            this.itemType = itemType;
            this.id = id;
            this.title = title;
            this.constructor = constructor;
            this.fields = fields;
            this.copyOf = copyOf;
        }

        Object clone(Object template, String title) throws ReflectiveOperationException {
            Class<?>[] parameters = constructor.getParameterTypes();
            Object[] arguments = new Object[parameters.length];
            Map<Class<?>, Integer> used = new LinkedHashMap<>();
            int strings = 0;
            for (int index = 0; index < parameters.length; index++) {
                Class<?> parameter = parameters[index];
                if (parameter == int.class) {
                    arguments[index] = RECEIPT_ITEM_ID;
                } else if (parameter == CharSequence.class) {
                    arguments[index] = title;
                } else {
                    int wanted = used.getOrDefault(parameter, 0);
                    Field match = null;
                    int seen = 0;
                    for (Field field : fields) {
                        if (field.getType() != parameter) continue;
                        if (seen++ == wanted) { match = field; break; }
                    }
                    if (match == null) throw new NoSuchFieldException("bottom menu argument not found");
                    used.put(parameter, wanted + 1);
                    arguments[index] = match.get(template);
                }
            }
            return constructor.newInstance(arguments);
        }
    }
}
