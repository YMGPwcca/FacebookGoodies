package ymg.pwcca.facebookgoodies;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.io.FileDescriptor;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

final class MessengerMessageBorders {
    private final Context app;
    private final MessengerReadControls reads;
    private final BooleanSupplier markersEnabled;
    private final ClassLoader loader;
    private final MessengerBuild build;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Map<String, Row> rows = new LinkedHashMap<String, Row>(512, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, Row> eldest) {
            return size() > 512;
        }
    };
    private final Map<View, Bound> bound = new WeakHashMap<>();
    private final Map<Object, String> rowIdByComponent =
            Collections.synchronizedMap(new WeakHashMap<>());
    private final AtomicBoolean refreshQueued = new AtomicBoolean();
    private final AtomicInteger errors = new AtomicInteger();
    private final AtomicInteger observedRows = new AtomicInteger();
    private final AtomicInteger mountedRows = new AtomicInteger();
    private final AtomicInteger resolvedThreads = new AtomicInteger();
    private final AtomicInteger mountedMarkers = new AtomicInteger();
    private final AtomicInteger tombstoneRows = new AtomicInteger();
    private final AtomicInteger markerStyleLogs = new AtomicInteger();
    private int installedHooks;
    private Field renderableField;
    private Field threadField;
    private Field alternateRenderableField;
    private Field alternateThreadField;
    private Field messageField;
    private Field binderActionField;
    private Field actionKindField;
    private Field actionRowField;
    private Field rowKeyField;
    private Field keyIdField;
    private Field cleanupKindField;
    private Field cleanupViewField;
    private Class<?> tombstoneClass;
    private Field tombstoneUnrestored;
    private Field tombstoneMissingSender;
    private Method getId;
    private Method getTime;
    private Method isOutgoing;

    MessengerMessageBorders(Context app, ClassLoader loader, MessengerReadControls reads,
                            BooleanSupplier markersEnabled, MessengerBuild build) {
        this.app = app.getApplicationContext();
        this.loader = loader;
        this.reads = reads;
        this.markersEnabled = markersEnabled;
        this.build = build;
        reads.setBorderRefresh(this::refresh);
        reads.setVisibleThreadResolver(this::visibleThreadFor);
    }

    int installedHooks() { return installedHooks; }
    int errorCount() { return errors.get(); }

    void install() {
        if (build == MessengerBuild.MESSENGER_585 || build.is586()) {
            try { installEncryptedRows585(); }
            catch (ReflectiveOperationException | RuntimeException | LinkageError failure) { logFailure("585 encrypted rows", failure); }
            try { installRowModel585(); }
            catch (ReflectiveOperationException | RuntimeException | LinkageError failure) { logFailure("585 row model", failure); }
            try { installRowMount585(); }
            catch (ReflectiveOperationException | RuntimeException | LinkageError failure) { logFailure("585 row mount", failure); }
        } else {
            installRowModel();
            installRowMount();
        }
        XposedBridge.log("FacebookGoodies: Messenger message status hooks=" + installedHooks);
    }

    private void installEncryptedRows585() throws ReflectiveOperationException {
        Class<?> component = hostType("X.AQ8");
        Class<?> context = hostType("X.2ED");
        Class<?> renderable = hostType("X.5OQ");
        Class<?> message = hostType("X.6mm");
        Class<?> deletedMessage = hostType("X.5Qj");
        Field thread = field(component, "A02");
        Field renderableField585 = field(component, "A0K");
        Field messageField585 = field(renderable, "A00");
        Method idMethod = hostMethod(message, "getId");
        Method timeMethod = hostMethod(message, "Cha");
        Method outgoingMethod = hostMethod(message, "D9Z");
        XposedBridge.hookMethod(hostMethod(component, "render", context), new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam param) {
                try {
                    Object wrapped = renderableField585.get(param.thisObject);
                    Object key = thread.get(param.thisObject);
                    if (!renderable.isInstance(wrapped) || key == null) return;
                    Object value = messageField585.get(wrapped);
                    if (!message.isInstance(value)) return;
                    String id = (String) idMethod.invoke(value);
                    long time = ((Number) timeMethod.invoke(value)).longValue();
                    boolean incoming = !((Boolean) outgoingMethod.invoke(value));
                    if (id == null || id.isEmpty() || time <= 0) return;
                    boolean tombstone = deletedMessage.isInstance(value);
                    synchronized (rows) { rows.put(id, new Row(key.toString(), time, incoming, tombstone)); }
                    reads.observeRenderedMessage(key, time, incoming);
                    refresh();
                } catch (ReflectiveOperationException | RuntimeException failure) {
                    logFailure("585 encrypted row", failure);
                }
            }
        });
        installedHooks++;

        Class<?> binder = hostType("X.AS6");
        Class<?> mountContext = hostType("X.1Tx");
        Class<?> action = hostType("X.DDG");
        Class<?> wrapper = hostType("X.AS4");
        Class<?> rowKey = hostType("X.AS3");
        Field binderAction = field(binder, "A00");
        Field actionKind = field(action, "$t");
        Field actionWrapper = field(action, "A01");
        Field wrapperKey = field(wrapper, "A02");
        Field keyId = field(rowKey, "A00");
        XposedBridge.hookMethod(hostMethod(binder, "AMG", mountContext, Object.class, Object.class), new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                try {
                    if (param.getThrowable() != null || !(param.args[1] instanceof View)) return;
                    Object callback = binderAction.get(param.thisObject);
                    if (!action.isInstance(callback) || actionKind.getInt(callback) != 2) return;
                    Object row = actionWrapper.get(callback);
                    if (!wrapper.isInstance(row)) return;
                    Object key = wrapperKey.get(row);
                    if (!rowKey.isInstance(key)) return;
                    String id = (String) keyId.get(key);
                    if (id != null) bindRow((View) param.args[1], id);
                } catch (ReflectiveOperationException | RuntimeException failure) {
                    logFailure("585 encrypted row mount", failure);
                }
            }
        });
        installedHooks++;

        Class<?> cleanup = hostType("X.BST");
        Field cleanupKind = field(cleanup, "$t");
        Field cleanupView = field(cleanup, "A00");
        XposedBridge.hookMethod(hostMethod(cleanup, "invoke"), new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam param) {
                try {
                    Object view = cleanupView.get(param.thisObject);
                    if (cleanupKind.getInt(param.thisObject) == (build.is586() ? 123 : 97)
                            && view instanceof View) unbind((View) view);
                } catch (ReflectiveOperationException | RuntimeException failure) {
                    logFailure("585 encrypted row cleanup", failure);
                }
            }
        });
        installedHooks++;
        XposedBridge.log("FacebookGoodies: Messenger 585 encrypted message status hooks installed");
    }

    private void installRowModel585() throws ReflectiveOperationException {
        Class<?> section = hostType("X.BIO");
        Class<?> sectionOperation = hostType("X.515");
        Class<?> itemState = hostType("X.2Rg");
        Class<?> itemPair = hostType("X.BIP");
        Class<?> messageRow = hostType("X.BHx");
        Class<?> renderedItem = hostType("X.2Qp");
        Class<?> message = hostType("com.facebook.messaging.model.messages.Message");

        Field operationKind = field(sectionOperation, "A01");
        Field itemPairField = field(itemState, "A01");
        Field rowDataField = field(itemPair, "A02");
        Field rowMessageField = field(messageRow, "A03");
        Field rowRenderableField = field(messageRow, "A0B");
        Class<?> renderable = hostType("X.5OQ");
        Field renderableMessage = field(renderable, "A00");
        Class<?> deletedMessage = hostType("X.5Qj");
        Field componentField = field(renderedItem, "A00");
        Field messageThreadField = field(message, "A0e");
        Field messageTimestampField = field(message, "A08");
        Field messageUnsentField = field(message, "A09");
        Field messageIdField = field(message, "A1m");
        Field fallbackMessageIdField = field(message, "A1x");
        Method messageOutgoing = hostMethod(hostType("X.D9j"), "D9Z");
        messageOutgoing.setAccessible(true);

        AtomicInteger rowDispatchLogs = new AtomicInteger();
        AtomicInteger rowEventLogs = new AtomicInteger();
        // 586 builds rows from the Litho event payload inside the row item's constructor;
        // the 585 section dispatch method is not invoked directly on this build.
        boolean operationDispatch = build.is586();
        Class<?> rowItem = operationDispatch ? hostType("X.2Qp") : null;
        final Field operationArgs = operationDispatch ? field(sectionOperation, "A03") : null;
        final Method rowOperation;
        if (operationDispatch) {
            rowOperation = MessengerHostBindings.method(build, rowItem, "CQ1");
            rowOperation.setAccessible(true);
        } else rowOperation = null;
        XC_MethodHook rowHook = new XC_MethodHook() {
            private Object operation(MethodHookParam param) throws ReflectiveOperationException {
                return operationDispatch ? rowOperation.invoke(param.thisObject) : param.args[0];
            }
            private Object input(MethodHookParam param) throws ReflectiveOperationException {
                if (!operationDispatch) return param.args[1];
                Object operation = operation(param);
                if (operation == null) return null;
                Object[] values = (Object[]) operationArgs.get(operation);
                if (values != null)
                    for (Object value : values) if (itemState.isInstance(value)) return value;
                return null;
            }
            private Object output(MethodHookParam param) {
                return operationDispatch ? param.thisObject : param.getResult();
            }
            @Override protected void beforeHookedMethod(MethodHookParam param) {
                if (operationDispatch) return;
                try {
                    int kind = operationKind.getInt(operation(param));
                    Object input = input(param);
                    if (rowDispatchLogs.getAndIncrement() < 5)
                        XposedBridge.log("FacebookGoodies: Messenger 585 row callback entered kind="
                                + kind + " input=" + (input == null ? "null" : input.getClass().getName()));
                } catch (ReflectiveOperationException | RuntimeException failure) {
                    logFailure("585 row callback entry", failure);
                }
            }
            @Override protected void afterHookedMethod(MethodHookParam param) {
                try {
                    if (param.getThrowable() != null) {
                        if (rowDispatchLogs.get() <= 5)
                            XposedBridge.log("FacebookGoodies: Messenger 585 row callback threw "
                                    + param.getThrowable().getClass().getSimpleName());
                        return;
                    }
                    Object operation = operation(param);
                    if (operation == null) return;
                    int kind = operationKind.getInt(operation);
                    Object input = input(param);
                    Object output = output(param);
                    if (rowDispatchLogs.get() <= 5)
                        XposedBridge.log("FacebookGoodies: Messenger 585 row callback result="
                                + (output == null ? "null" : output.getClass().getName()));
                    if (kind != 239257522 || !itemState.isInstance(input)
                            || !renderedItem.isInstance(output)) return;
                    if (rowEventLogs.incrementAndGet() <= 3)
                        XposedBridge.log("FacebookGoodies: Messenger 585 row event mapped candidate");
                    Object pair = itemPairField.get(input);
                    if (pair == null || !itemPair.isInstance(pair)) {
                        if (rowEventLogs.get() <= 3)
                            XposedBridge.log("FacebookGoodies: Messenger 585 row pair missing");
                        return;
                    }
                    Object model = rowDataField.get(pair);
                    if (rowEventLogs.get() <= 3)
                        XposedBridge.log("FacebookGoodies: Messenger 585 row model="
                                + (model == null ? "null" : model.getClass().getName()));
                    Object component = componentField.get(output);
                    if (component == null) return;

                    String rowId = null;
                    if (messageRow.isInstance(model)) {
                        Object messageValue = rowMessageField.get(model);
                        if (messageValue != null) {
                            Object threadKey = messageThreadField.get(messageValue);
                            long messageTime = messageTimestampField.getLong(messageValue);
                            String id = (String) messageIdField.get(messageValue);
                            if (id == null || id.isEmpty()) id = (String) fallbackMessageIdField.get(messageValue);
                            Object wrapper = rowRenderableField.get(model);
                            Object nativeMessage = renderable.isInstance(wrapper)
                                    ? renderableMessage.get(wrapper) : null;
                            if (threadKey != null && id != null && !id.isEmpty() && messageTime > 0
                                    && nativeMessage != null
                                    && messageOutgoing.getDeclaringClass().isInstance(nativeMessage)) {
                                boolean incoming = !((Boolean) messageOutgoing.invoke(nativeMessage));
                                rowId = id;
                                synchronized (rows) {
                                    Row prior = rows.get(id);
                                    boolean tombstone = deletedMessage.isInstance(nativeMessage)
                                            || messageUnsentField.getLong(messageValue) > 0
                                            || prior != null && prior.tombstone && prior.time == messageTime
                                            && prior.thread.equals(threadKey.toString());
                                    rows.put(id, new Row(threadKey.toString(), messageTime, incoming, tombstone));
                                }
                                reads.observeRenderedMessage(threadKey, messageTime, incoming);
                            }
                        }
                    }
                    rowIdByComponent.put(component, rowId == null ? "" : rowId);
                    refresh();
                    reads.requestChatScan();
                    if (observedRows.incrementAndGet() <= 3)
                        XposedBridge.log("FacebookGoodies: Messenger 585 row model mapped");
                } catch (ReflectiveOperationException | RuntimeException failure) {
                    logFailure("585 row model", failure);
                }
            }
        };
        if (operationDispatch) XposedBridge.hookAllConstructors(rowItem, rowHook);
        else XposedBridge.hookMethod(hostMethod(section, "A0U", sectionOperation, Object.class), rowHook);
        AtomicInteger sectionLayoutLogs = new AtomicInteger();
        Method sectionLayout = hostMethod(section, "A0T",
                hostType("X.262"));
        XposedBridge.hookMethod(sectionLayout, new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                if (param.getThrowable() == null && sectionLayoutLogs.incrementAndGet() <= 5)
                    XposedBridge.log("FacebookGoodies: Messenger 585 section layout built result="
                            + (param.getResult() == null ? "null" : param.getResult().getClass().getName()));
            }
        });
        try { installPresenterTrace585(); }
        catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            logFailure("585 presenter trace", failure);
        }
        installedHooks++;
        XposedBridge.log("FacebookGoodies: Messenger 585 row model hook installed");
    }

    private void installPresenterTrace585() throws ReflectiveOperationException {
        Class<?> presenter = hostType("X.7Fp");
        Class<?> threadKey = hostType("com.facebook.messaging.model.threadkey.ThreadKey");
        Class<?> capabilities = hostType("com.facebook.xapp.messaging.capability.vector.Capabilities");
        Class<?> immutableList = hostType("com.google.common.collect.ImmutableList");
        Class<?> threadView = hostType("com.facebook.threadview.ThreadViewMessagesRecyclerView");
        Field recyclerField = field(presenter, "A0O");
        Method getAdapter = adapterGetter(threadView);
        Method updateMessages = hostMethod(presenter, "A08", threadKey, capabilities,
                immutableList, Runnable.class, boolean.class, boolean.class);
        AtomicInteger updateLogs = new AtomicInteger();
        XposedBridge.hookMethod(updateMessages, new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                if (param.getThrowable() != null || updateLogs.incrementAndGet() > 10) return;
                try {
                    Object rows = param.args[2];
                    StringBuilder first = new StringBuilder();
                    if (rows instanceof List<?>) {
                        List<?> values = (List<?>) rows;
                        for (int index = 0; index < Math.min(3, values.size()); index++) {
                            if (index > 0) first.append(',');
                            Object value = values.get(index);
                            first.append(value == null ? "null" : value.getClass().getName());
                        }
                    }
                    Object recycler = recyclerField.get(param.thisObject);
                    Object adapter = recycler == null || getAdapter == null ? null
                            : getAdapter.invoke(recycler);
                    Object count = adapter == null ? "null" : adapter.getClass()
                            .getMethod("getItemCount").invoke(adapter);
                    XposedBridge.log("FacebookGoodies: Messenger 585 presenter update size="
                            + (rows instanceof List<?> ? ((List<?>) rows).size() : -1)
                            + " first=" + first + " adapter="
                            + (adapter == null ? "null" : adapter.getClass().getName())
                            + " count=" + count);
                } catch (ReflectiveOperationException | RuntimeException failure) {
                    logFailure("585 presenter data trace", failure);
                }
            }
        });
    }

    private void installRowMount585() throws ReflectiveOperationException {
        Class<?> componentHost = hostType("com.facebook.litho.ComponentHost");
        Class<?> mountItem = hostType("X.2dp");
        Class<?> renderNode = hostType("com.facebook.rendercore.RenderTreeNode");
        Class<?> lithoRenderUnit = hostType("X.1Pc");
        Field itemNode = field(mountItem, "A01");
        Field itemContent = field(mountItem, "A05");
        Field nodeRenderUnit = field(renderNode, "A07");
        Field nodeParent = field(renderNode, "A06");
        Field nodeData = field(renderNode, "A08");
        // 586 moved the Litho component off A03 (now the scoped-mount SparseArray) onto A04.
        Field renderComponent = field(lithoRenderUnit,
                build.is586() ? "A04" : "A03");
        AtomicInteger mountCallbacks = new AtomicInteger();
        AtomicInteger unmatchedMountLogs = new AtomicInteger();
        AtomicInteger mappedMountLogs = new AtomicInteger();
        XC_MethodHook mountHook = new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                if (param.getThrowable() != null) return;
                if (mountCallbacks.incrementAndGet() <= 200)
                    XposedBridge.log("FacebookGoodies: Messenger 585 mount callback");
                bindMountedRow585(param.args[0], itemNode, itemContent, nodeRenderUnit,
                        renderComponent, lithoRenderUnit, nodeParent, nodeData,
                        unmatchedMountLogs, mappedMountLogs);
            }
        };
        XposedBridge.hookMethod(hostMethod(componentHost, "A0T", mountItem), mountHook);
        installedHooks++;
        XposedBridge.hookMethod(hostMethod(componentHost, "A0U", mountItem, int.class), mountHook);
        installedHooks++;
        XposedBridge.log("FacebookGoodies: Messenger 585 row mount hooks installed");
        try {
            Class<?> threadView = hostType("com.facebook.threadview.ThreadViewMessagesRecyclerView");
            Method getAdapter = adapterGetter(threadView);
            AtomicInteger threadViewLogs = new AtomicInteger();
            XposedBridge.hookMethod(hostMethod(threadView, "onAttachedToWindow"),
                    new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam param) {
                    try {
                        Object adapter = getAdapter == null ? null : getAdapter.invoke(param.thisObject);
                        if (threadViewLogs.incrementAndGet() <= 5)
                            XposedBridge.log("FacebookGoodies: Messenger 585 thread adapter="
                                    + (adapter == null ? "null" : adapter.getClass().getName()));
                    } catch (ReflectiveOperationException | RuntimeException failure) {
                        logFailure("585 thread adapter trace", failure);
                    }
                }
            });
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            logFailure("585 ThreadView adapter trace", failure);
        }
    }

    /** The host repackages RecyclerView; 586 obfuscates the public adapter getter. */
    private static Method adapterGetter(Class<?> threadView) {
        try { return threadView.getMethod("getAdapter"); }
        catch (NoSuchMethodException renamed) {
            for (Class<?> type = threadView; type != null && type != Object.class;
                 type = type.getSuperclass()) {
                for (Method method : type.getMethods())
                    if (method.getParameterCount() == 0
                            && method.getReturnType().getName().endsWith("RecyclerView$Adapter"))
                        return method;
            }
            return null;
        }
    }

    private void bindMountedRow585(Object item, Field itemNode, Field itemContent,
                                   Field nodeRenderUnit, Field renderComponent,
                                   Class<?> lithoRenderUnit, Field nodeParent, Field nodeData,
                                   AtomicInteger unmatchedMountLogs, AtomicInteger mappedMountLogs) {
        try {
            Object node = itemNode.get(item);
            Object renderUnit = node == null ? null : nodeRenderUnit.get(node);
            if (renderUnit == null) return;
            Object component = renderComponent.get(renderUnit);
            if (component == null) return;
            boolean mapped;
            String id;
            synchronized (rowIdByComponent) {
                mapped = rowIdByComponent.containsKey(component);
                id = rowIdByComponent.get(component);
            }
            if (!mapped) {
                if (unmatchedMountLogs.incrementAndGet() <= 50) {
                    StringBuilder ancestry = new StringBuilder();
                    Object current = node;
                    for (int depth = 0; current != null && depth < 8; depth++) {
                        Object ancestorUnit = nodeRenderUnit.get(current);
                        Object ancestorComponent = ancestorUnit != null && lithoRenderUnit.isInstance(ancestorUnit)
                                ? renderComponent.get(ancestorUnit) : null;
                        Object data = nodeData.get(current);
                        if (depth > 0) ancestry.append(" <- ");
                        ancestry.append(ancestorComponent == null ? "?" : ancestorComponent.getClass().getName())
                                .append('/')
                                .append(data == null ? "null" : data.getClass().getName());
                        current = nodeParent.get(current);
                    }
                    XposedBridge.log("FacebookGoodies: Messenger 585 mount ancestry " + ancestry);
                }
                return;
            }
            Object content = itemContent.get(item);
            if (!(content instanceof View)) return;
            View view = (View) content;
            if (mappedMountLogs.incrementAndGet() <= 200)
                XposedBridge.log("FacebookGoodies: Messenger 585 mount component mapped hasId="
                        + (id != null && !id.isEmpty()) + " content=" + content.getClass().getName());
            if (id == null || id.isEmpty()) unbind(view);
            else bindRow(view, id);
        } catch (ReflectiveOperationException | RuntimeException failure) {
            logFailure("585 row mount", failure);
        }
    }

    private void installRowModel() {
        try {
            Class<?> component = hostType("X.9TG");
            Class<?> renderContext = hostType("X.2DF");
            Class<?> renderable = hostType("X.7OV");
            Class<?> message = hostType("X.AXL");
            tombstoneClass = hostType("X.8bA");
            tombstoneUnrestored = field(tombstoneClass, "A02");
            tombstoneMissingSender = field(tombstoneClass, "A01");
            renderableField = field(component, "A0K");
            threadField = field(component, "A02");
            messageField = field(renderable, "A00");
            getId = hostMethod(message, "getId");
            getTime = hostMethod(message, "BdG");
            isOutgoing = hostMethod(message, "Bqp");
            installedHooks++;
            Method render = hostMethod(component, "render", renderContext);
            XposedBridge.hookMethod(render, new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) {
                    rememberRow(param.thisObject, renderableField, threadField);
                }
            });
            installedHooks++;
            Class<?> alternate = hostType("X.9lX");
            alternateRenderableField = field(alternate, "A05");
            alternateThreadField = field(alternate, "A02");
            XposedBridge.hookMethod(hostMethod(alternate, "render", renderContext), new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) {
                    rememberRow(param.thisObject, alternateRenderableField, alternateThreadField);
                }
            });
            installedHooks++;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            logFailure("row model", failure);
        }
    }

    private void installRowMount() {
        try {
            Class<?> binder = hostType("X.9VN");
            Class<?> mountContext = hostType("X.1TX");
            Class<?> action = hostType("X.Bho");
            Class<?> row = hostType("X.9VL");
            Class<?> key = hostType("X.9VK");
            binderActionField = field(binder, "A00");
            actionKindField = field(action, "$t");
            actionRowField = field(action, "A01");
            rowKeyField = field(row, "A02");
            keyIdField = field(key, "A00");
            Method mount = hostMethod(binder, "AD0", mountContext, Object.class, Object.class);
            XposedBridge.hookMethod(mount, new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam param) {
                    if (param.getThrowable() == null && param.args[1] instanceof View)
                        bind((View) param.args[1], param.thisObject);
                }
            });
            installedHooks++;

            Class<?> cleanup = hostType("X.AZF");
            cleanupKindField = field(cleanup, "$t");
            cleanupViewField = field(cleanup, "A00");
            XposedBridge.hookMethod(hostMethod(cleanup, "invoke"), new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) {
                    try {
                        if (cleanupKindField.getInt(param.thisObject) == 16
                                && cleanupViewField.get(param.thisObject) instanceof View)
                            unbind((View) cleanupViewField.get(param.thisObject));
                    } catch (IllegalAccessException failure) { logFailure("row cleanup", failure); }
                }
            });
            installedHooks++;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            logFailure("row mount", failure);
        }
    }

    private String visibleThreadFor(View root) {
        String selected = null;
        for (Map.Entry<View, Bound> entry : bound.entrySet()) {
            View view = entry.getKey();
            if (view == null || !view.isShown() || !inside(view, root)) continue;
            Row row;
            synchronized (rows) { row = rows.get(entry.getValue().id); }
            if (row == null) continue;
            if (selected != null && !selected.equals(row.thread)) return null;
            selected = row.thread;
        }
        if (selected != null && resolvedThreads.incrementAndGet() <= 3)
            XposedBridge.log("FacebookGoodies: Messenger visible thread resolved from mounted rows");
        return selected;
    }

    private static boolean inside(View child, View root) {
        View current = child;
        for (int depth = 0; depth < 64; depth++) {
            if (current == root) return true;
            ViewParent parent = current.getParent();
            if (!(parent instanceof View)) return false;
            current = (View) parent;
        }
        return false;
    }

    private void rememberRow(Object component, Field source, Field threadSource) {
        try {
            Object renderable = source.get(component);
            if (renderable == null) return;
            Object message = messageField.get(renderable);
            Object thread = threadSource.get(component);
            if (message == null || thread == null) return;
            String id = (String) getId.invoke(message);
            if (id == null || id.isEmpty()) return;
            boolean tombstone = tombstoneClass.isInstance(message);
            Row row = new Row(thread.toString(),
                    ((Number) getTime.invoke(message)).longValue(),
                    !((Boolean) isOutgoing.invoke(message)), tombstone);
            reads.observeRenderedMessage(thread, row.time, row.incoming);
            synchronized (rows) { rows.put(id, row); }
            if (tombstone) {
                boolean unrestored = tombstoneUnrestored.getBoolean(message);
                boolean missingSender = tombstoneMissingSender.getBoolean(message);
                if (tombstoneRows.incrementAndGet() <= 3)
                    XposedBridge.log("FacebookGoodies: Messenger tombstone row observed (incoming="
                            + row.incoming + ", unrestored=" + unrestored
                            + ", missingSender=" + missingSender + ")");
            }
            if (observedRows.incrementAndGet() <= 3)
                XposedBridge.log("FacebookGoodies: Messenger message row observed (incoming="
                        + row.incoming + ", timestamp=" + (row.time > 0) + ")");
            refresh();
            reads.requestChatScan();
        } catch (ReflectiveOperationException | RuntimeException failure) {
            logFailure("row identity", failure);
        }
    }

    private void bind(View view, Object binder) {
        try {
            Object action = binderActionField.get(binder);
            if (action == null || actionKindField.getInt(action) != 2) return;
            Object row = actionRowField.get(action);
            Object key = row == null ? null : rowKeyField.get(row);
            String id = key == null ? null : (String) keyIdField.get(key);
            if (id == null) return;
            bindRow(view, id);
        } catch (ReflectiveOperationException | RuntimeException failure) {
            logFailure("row bind", failure);
        }
    }

    private void bindRow(View view, String id) {
        unbind(view);
        Bound binding = new Bound(id, view);
        bound.put(view, binding);
        view.post(() -> attachMarker(view, binding));
        reads.requestChatScan();
        if (mountedRows.incrementAndGet() <= 3)
            XposedBridge.log("FacebookGoodies: Messenger message row mounted");
    }

    private void attachMarker(View row, Bound binding) {
        if (bound.get(row) != binding || binding.marker != null) return;
        ViewParent parent = row.getParent();
        if (!(parent instanceof ViewGroup)) return;
        ViewGroup host = (ViewGroup) parent;
        binding.host = new WeakReference<>(host);
        binding.marker = new StatusMarker();
        binding.layoutListener = (v, left, top, right, bottom,
                oldLeft, oldTop, oldRight, oldBottom) -> positionMarker(binding);
        row.addOnLayoutChangeListener(binding.layoutListener);
        update(binding);
        reads.requestChatScan();
        if (mountedMarkers.incrementAndGet() <= 3)
            XposedBridge.log("FacebookGoodies: Messenger message status marker attached");
    }

    private void positionMarker(Bound binding) {
        View row = binding.row.get();
        ViewGroup host = binding.host.get();
        if (row == null || host == null || binding.marker == null) return;
        int[] rowPoint = new int[2];
        int[] hostPoint = new int[2];
        row.getLocationOnScreen(rowPoint);
        host.getLocationOnScreen(hostPoint);
        float density = row.getResources().getDisplayMetrics().density;
        int size = Math.round(16 * density);
        int gap = Math.round(5 * density);
        int height = binding.style == 3 ? size * 2 + Math.round(2 * density) : size;
        int left = rowPoint[0] - hostPoint[0];
        int x = Math.max(gap, left - size / 2);
        int contentTop = 0;
        int contentHeight = row.getHeight();
        if (row instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) row;
            for (int index = 0; index < group.getChildCount(); index++) {
                View child = group.getChildAt(index);
                if (child.getVisibility() == View.VISIBLE && child.getLeft() == 0
                        && child.getWidth() == row.getWidth() && child.getHeight() > 0
                        && child.getTop() >= 0 && child.getBottom() <= row.getHeight()) {
                    contentTop = child.getTop();
                    contentHeight = child.getHeight();
                    break;
                }
            }
        }
        int y = rowPoint[1] - hostPoint[1]
                + contentTop + (contentHeight - height) / 2;
        binding.marker.setBounds(x, y, x + size, y + height);
    }

    private void unbind(View view) {
        Bound binding = bound.remove(view);
        if (binding == null) return;
        ViewGroup host = binding.host.get();
        if (host != null && binding.marker != null) host.getOverlay().remove(binding.marker);
        if (binding.layoutListener != null) view.removeOnLayoutChangeListener(binding.layoutListener);
    }

    void refresh() {
        if (!refreshQueued.compareAndSet(false, true)) return;
        main.post(() -> {
            try {
                for (Bound binding : bound.values()) {
                    try { update(binding); }
                    catch (RuntimeException failure) { logFailure("marker refresh", failure); }
                }
            } finally { refreshQueued.set(false); }
        });
    }

    private void update(Bound binding) {
        Row row;
        synchronized (rows) { row = rows.get(binding.id); }
        int style = 0;
        if (markersEnabled.getAsBoolean() && row != null) {
            if (row.incoming && row.tombstone) style |= 2;
            if (reads.shouldOutlineReceived(row.thread, row.time, row.incoming)) style |= 1;
        }
        if (row != null && markerStyleLogs.incrementAndGet() <= 5)
            XposedBridge.log("FacebookGoodies: Messenger 585 marker style enabled="
                    + markersEnabled.getAsBoolean() + " incoming=" + row.incoming
                    + " style=" + style);
        int previousStyle = binding.style;
        binding.style = style;
        ViewGroup host = binding.host.get();
        if (host == null || binding.marker == null) return;
        if (previousStyle == style && binding.visible == (style != 0)) return;
        host.getOverlay().remove(binding.marker);
        binding.visible = false;
        if (style == 0) return;
        binding.marker.style(style);
        positionMarker(binding);
        host.getOverlay().add(binding.marker);
        binding.visible = true;
    }

    private Class<?> hostType(String name) throws ClassNotFoundException {
        return Class.forName(MessengerHostBindings.typeName(build, name), false, loader);
    }

    private Method hostMethod(Class<?> owner, String name, Class<?>... parameters)
            throws NoSuchMethodException {
        return MessengerHostBindings.method(build, owner, name, parameters);
    }

    private static Field field(Class<?> owner, String name) throws NoSuchFieldException {
        Field value = owner.getDeclaredField(name);
        value.setAccessible(true);
        return value;
    }

    private void logFailure(String point, Throwable failure) {
        if (errors.incrementAndGet() <= 3)
            XposedBridge.log("FacebookGoodies: Messenger status " + point + " failed: "
                    + failure.getClass().getSimpleName());
    }

    private static final class Row {
        final String thread;
        final long time;
        final boolean incoming;
        final boolean tombstone;

        Row(String thread, long time, boolean incoming, boolean tombstone) {
            this.thread = thread;
            this.time = time;
            this.incoming = incoming;
            this.tombstone = tombstone;
        }
    }

    private static final class Bound {
        final String id;
        final WeakReference<View> row;
        WeakReference<ViewGroup> host = new WeakReference<>(null);
        StatusMarker marker;
        View.OnLayoutChangeListener layoutListener;
        int style;
        boolean visible;

        Bound(String id, View row) {
            this.id = id;
            this.row = new WeakReference<>(row);
        }
    }

    private static final class StatusMarker extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path eye = new Path();
        private final RectF chat = new RectF(3, 4, 21, 19);
        private int mode;
        private int alpha = 255;

        StatusMarker() {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            eye.moveTo(2, 12);
            eye.cubicTo(5, 8, 8.5f, 6, 12, 6);
            eye.cubicTo(15.5f, 6, 19, 8, 22, 12);
            eye.cubicTo(19, 16, 15.5f, 18, 12, 18);
            eye.cubicTo(8.5f, 18, 5, 16, 2, 12);
        }

        void style(int style) { mode = style; invalidateSelf(); }

        @Override public void draw(Canvas canvas) {
            Rect bounds = getBounds();
            if (bounds.isEmpty() || mode == 0) return;
            int save = canvas.save();
            canvas.translate(bounds.left, bounds.top);
            canvas.scale(bounds.width() / 24f, bounds.width() / 24f);
            paint.setAlpha(alpha);
            if ((mode & 1) != 0) {
                paint.setColor(Color.rgb(117, 160, 255));
                paint.setAlpha(alpha);
                canvas.drawPath(eye, paint);
                canvas.drawCircle(12, 12, 2.5f, paint);
                canvas.drawLine(4, 4, 20, 20, paint);
            }
            if ((mode & 2) != 0) {
                if ((mode & 1) != 0) canvas.translate(0, bounds.height() * 24f / bounds.width() - 24f);
                paint.setColor(Color.rgb(230, 107, 46));
                paint.setAlpha(alpha);
                canvas.drawRoundRect(chat, 3, 3, paint);
                canvas.drawLine(9, 9, 15, 15, paint);
                canvas.drawLine(15, 9, 9, 15, paint);
            }
            canvas.restoreToCount(save);
        }

        @Override public void setAlpha(int value) { alpha = value; invalidateSelf(); }
        @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}
