package ymg.pwcca.facebookgoodies;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

final class MessengerReadControls {
    private final Context app;
    private final ClassLoader loader;
    private final BooleanSupplier enabled;
    private final BooleanSupplier localReadEnabled;
    private final BooleanSupplier markersEnabled;
    private final MessengerBuild build;
    private final SharedPreferences localReads;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final AtomicInteger blocked = new AtomicInteger();
    private final AtomicInteger dividerBlocks = new AtomicInteger();
    private final AtomicInteger installedGroups = new AtomicInteger();
    private final AtomicInteger errors = new AtomicInteger();
    private final AtomicInteger missingComposerScans = new AtomicInteger();
    private final AtomicInteger activityLogs = new AtomicInteger();
    private final AtomicInteger badgeMounts = new AtomicInteger();
    private final AtomicInteger mappedChatScans = new AtomicInteger();
    private final Map<String, String> threadBySdk = boundedMap(512);
    private final Map<String, String> sdkByThread = boundedMap(512);
    private final Map<String, Pending> pending = boundedMap();
    private final Map<String, WeakReference<Object>> liveSummaries = boundedMap();
    private final Map<String, Long> lastMessageByThread = boundedMap(512);
    private final Map<String, Long> liveReadWatermarks = boundedMap(512);
    private final MessengerLocalReadValues appliedLocalReads = new MessengerLocalReadValues();
    private final AtomicBoolean scanPending = new AtomicBoolean();
    private WeakReference<EditText> currentComposer = new WeakReference<>(null);
    private WeakReference<View> currentEmoji = new WeakReference<>(null);
    private WeakReference<View> badgeHost = new WeakReference<>(null);
    private Drawable pendingBadge;
    private boolean originalEmojiLongClickable;
    private WeakReference<Activity> currentActivity = new WeakReference<>(null);
    private ViewTreeObserver.OnGlobalLayoutListener layoutListener;
    private volatile boolean chatVisible;
    private volatile boolean ambiguousChat;
    private volatile String activeSdkId;
    private volatile String recentSdkId;
    private volatile long recentBlockedAt;
    private Method sendMethod;
    private Class<?> function0;
    private Field summaryThread;
    private Field summaryReadTime;
    private Field summaryLastMessage;
    private Method threadConverter;
    private volatile Object converterInstance;
    private Runnable borderRefresh = () -> { };
    private Function<View, String> visibleThread = ignored -> null;

    MessengerReadControls(Context app, ClassLoader loader, BooleanSupplier enabled,
                          BooleanSupplier localReadEnabled, BooleanSupplier markersEnabled,
                          MessengerBuild build) {
        this.app = app;
        this.loader = loader;
        this.enabled = enabled;
        this.localReadEnabled = localReadEnabled;
        this.markersEnabled = markersEnabled;
        this.build = build;
        this.localReads = app.getSharedPreferences("facebookgoodies_local_reads", Context.MODE_PRIVATE);
    }

    void install() {
        try { installReadState(); installedGroups.incrementAndGet(); } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) { logFailure("local-read", failure); }
        try { installSend(); installedGroups.incrementAndGet(); } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) { logFailure("manual-seen", failure); }
        try { installActivityOverlay(); installedGroups.incrementAndGet(); } catch (RuntimeException | LinkageError failure) { logFailure("seen-button", failure); }
        try { installPillFilters(); installedGroups.incrementAndGet(); } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) { logFailure("unread-pills", failure); }
        try { installUnreadDivider(); installedGroups.incrementAndGet(); } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) { logFailure("unread-divider", failure); }
    }

    int installedGroups() { return installedGroups.get(); }
    int blockedCount() { return blocked.get(); }
    int errorCount() { return errors.get(); }

    void setBorderRefresh(Runnable refresh) { borderRefresh = refresh; }
    void setVisibleThreadResolver(Function<View, String> resolver) { visibleThread = resolver; }

    boolean shouldOutlineReceived(String thread, long messageTime, boolean incoming) {
        try {
            Long observed = liveReadWatermarks.get(thread);
            if (observed != null && markersEnabled.getAsBoolean())
                observeSyncedRead(thread, observed, true);
            return MessengerSeenBorderPolicy.show(markersEnabled.getAsBoolean(), incoming,
                    messageTime, confirmedReadFor(thread));
        } catch (RuntimeException failure) {
            logFailure("message border state", failure);
            return false;
        }
    }

    void observeRenderedMessage(Object threadKey, long messageTime, boolean incoming) {
        if (threadKey == null || !incoming || messageTime <= 0) return;
        String thread = threadKey.toString();
        Long previous = lastMessageByThread.get(thread);
        if (previous == null || messageTime > previous)
            lastMessageByThread.put(thread, messageTime);
        ensureSdkThread(threadKey, thread);
    }

    private void ensureSdkThread(Object threadKey, String thread) {
        if (sdkByThread.containsKey(thread) || threadConverter == null) return;
        try {
            Object converter = converterInstance;
            if (converter == null) {
                converter = threadConverter.getDeclaringClass().getDeclaredConstructor().newInstance();
                converterInstance = converter;
            }
            Object sdk = threadConverter.invoke(converter, threadKey);
            if (sdk instanceof String) {
                threadBySdk.put((String) sdk, thread);
                sdkByThread.put(thread, (String) sdk);
            }
        } catch (ReflectiveOperationException | RuntimeException failure) {
            logFailure("message thread identifier", failure);
        }
    }

    private void installReadState() throws ReflectiveOperationException {
        try { installThreadConverter(); }
        catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            logFailure("thread converter", failure);
        }
        if (build == MessengerBuild.MESSENGER_585 || build == MessengerBuild.MESSENGER_586) {
            try { installLiveRead585(); }
            catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
                logFailure("live read hook", failure);
            }
        }

        Class<?> summary = Class.forName("com.facebook.messaging.model.threads.ThreadSummary", false, loader);
        summaryThread = summary.getDeclaredField("A0e");
        summaryReadTime = summary.getDeclaredField("A0C");
        summaryLastMessage = summary.getDeclaredField("A0A");
        summaryThread.setAccessible(true);
        summaryReadTime.setAccessible(true);
        summaryLastMessage.setAccessible(true);
        XposedBridge.hookAllConstructors(summary, new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                if (!enabled.getAsBoolean() && !localReadEnabled.getAsBoolean()
                        && !markersEnabled.getAsBoolean()) return;
                try {
                    Object key = summaryThread.get(param.thisObject);
                    if (key != null) {
                        String thread = key.toString();
                        liveSummaries.put(thread, new WeakReference<>(param.thisObject));
                        observeInitialRead(thread, summaryReadTime.getLong(param.thisObject));
                        if ((build == MessengerBuild.MESSENGER_585 || build == MessengerBuild.MESSENGER_586)
                                && thread.startsWith("ADVANCED_CRYPTO_"))
                            observeSyncedRead(thread, summaryReadTime.getLong(param.thisObject), false);
                        long lastMessage = summaryLastMessage.getLong(param.thisObject);
                        Long previous = lastMessageByThread.get(thread);
                        if (lastMessage > 0 && (previous == null || lastMessage > previous))
                            lastMessageByThread.put(thread, lastMessage);
                    }
                    applyLocalRead(param.thisObject);
                } catch (IllegalAccessException failure) { logFailure("local read summary", failure); }
            }
        });
        XposedBridge.log("FacebookGoodies: Messenger local read model hook installed");
    }

    private void installThreadConverter() throws ReflectiveOperationException {
        Class<?> threadKey = Class.forName("com.facebook.messaging.model.threadkey.ThreadKey", false, loader);
        Class<?> converter = Class.forName(
                build == MessengerBuild.MESSENGER_586 ? "X.4mE"
                        : build == MessengerBuild.MESSENGER_585 ? "X.4lW" : "X.7gJ", false, loader);
        Method convert = converter.getDeclaredMethod(
                build == MessengerBuild.MESSENGER_584_1 ? "A01" : "A02", threadKey);
        threadConverter = convert;
        XposedBridge.hookMethod(convert, new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                converterInstance = param.thisObject;
                if (!(param.getResult() instanceof String) || param.args[0] == null) return;
                String key = param.args[0].toString();
                String sdkId = (String) param.getResult();
                threadBySdk.put(sdkId, key);
                sdkByThread.put(key, sdkId);
            }
        });
    }

    private void installLiveRead585() throws ReflectiveOperationException {
        Class<?> stream = Class.forName(build == MessengerBuild.MESSENGER_586 ? "X.517" : "X.50R", false, loader);
        Field streamThread = stream.getDeclaredField("A03");
        Field streamLatest = stream.getDeclaredField("A02");
        streamThread.setAccessible(true);
        streamLatest.setAccessible(true);
        XposedBridge.hookAllConstructors(stream, new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                if (param.getThrowable() != null) return;
                try {
                    Object key = streamThread.get(param.thisObject);
                    if (key == null) return;
                    String thread = key.toString();
                    if (thread.startsWith("ADVANCED_CRYPTO_")) {
                        long latest = streamLatest.getLong(param.thisObject);
                        liveReadWatermarks.put(thread, latest);
                        if (markersEnabled.getAsBoolean()) observeSyncedRead(thread, latest, true);
                    }
                } catch (IllegalAccessException | RuntimeException failure) {
                    logFailure("live read state", failure);
                }
            }
        });
    }

    private void installSend() throws ReflectiveOperationException {
        Class<?> sender = Class.forName(
                build == MessengerBuild.MESSENGER_586 ? "X.AeP"
                        : build == MessengerBuild.MESSENGER_585 ? "X.AaM" : "X.9iU", false, loader);
        function0 = Class.forName("kotlin.jvm.functions.Function0", false, loader);
        Method invoke = function0.getDeclaredMethod("invoke");
        sendMethod = sender.getDeclaredMethod("A00", Long.class, String.class, String.class, function0, function0);
        if (sendMethod.getReturnType() != void.class) throw new NoSuchMethodException("Messenger mark-read contract changed");
        XposedBridge.hookMethod(sendMethod, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam param) {
                String sdkId = (String) param.args[1];
                Long readTime = (Long) param.args[0];
                String key = threadBySdk.get(sdkId);
                if (key != null) rememberLocalRead(key, readTime);
                if (!enabled.getAsBoolean()) return;
                Long watermark = readTime != null ? readTime : lastMessageFor(sdkId);
                boolean previouslySent = alreadySent(sdkId, watermark);
                if (!previouslySent)
                    pending.put(sdkId, new Pending(param.thisObject, readTime, watermark,
                            sdkId, (String) param.args[2]));
                recentSdkId = sdkId;
                recentBlockedAt = SystemClock.elapsedRealtime();
                if (chatVisible && !ambiguousChat) {
                    if (activeSdkId == null || activeSdkId.equals(sdkId)) activeSdkId = sdkId;
                    else { activeSdkId = null; ambiguousChat = true; }
                }
                try {
                    invoke.invoke(param.args[3]);
                } catch (ReflectiveOperationException | RuntimeException failure) {
                    logFailure("mark-read completion", failure);
                }
                param.setResult(null);
                requestChatScan();
                main.post(borderRefresh);
                if (blocked.incrementAndGet() <= 3)
                    XposedBridge.log("FacebookGoodies: Messenger modular seen blocked (timestamp "
                            + (readTime == null ? "derived" : "present") + ", watermark "
                            + (watermark == null || watermark <= 0 ? "absent" : "present")
                            + ", already sent " + previouslySent + ")");
            }
        });
        XposedBridge.log("FacebookGoodies: Messenger modular seen hook installed");
    }

    private void installActivityOverlay() {
        XposedBridge.hookAllMethods(View.class, "performLongClick", new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam param) {
                if (param.thisObject != currentEmoji.get() || pendingBadge == null || !enabled.getAsBoolean()
                        || !chatVisible || ambiguousChat || activeSdkId == null || !pending.containsKey(activeSdkId)) return;
                sendForCurrentThread();
                param.setResult(true);
            }
        });
        ((Application) app).registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override public void onActivityCreated(Activity activity, android.os.Bundle state) { }
            @Override public void onActivityStarted(Activity activity) { }
            @Override public void onActivityResumed(Activity activity) {
                if ("com.facebook.orca".equals(activity.getPackageName())) observe(activity);
            }
            @Override public void onActivityPaused(Activity activity) {
                if (currentActivity.get() == activity) stopObserving(activity);
            }
            @Override public void onActivityStopped(Activity activity) { }
            @Override public void onActivitySaveInstanceState(Activity activity, android.os.Bundle state) { }
            @Override public void onActivityDestroyed(Activity activity) { }
        });
        XposedBridge.log("FacebookGoodies: Messenger emoji hold hook installed");
    }

    private void installPillFilters() throws ReflectiveOperationException {
        if (build == MessengerBuild.MESSENGER_584_1) {
            Class<?> controller = Class.forName("X.5ur", false, loader);
            Method show = controller.getDeclaredMethod("A02", controller);
            Method hide = controller.getDeclaredMethod("A01", controller);
            XposedBridge.hookMethod(show, new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) {
                    if (!enabled.getAsBoolean()) return;
                    try { hide.invoke(null, param.args[0]); } catch (ReflectiveOperationException | RuntimeException failure) { logFailure("hide unread pill", failure); }
                    param.setResult(null);
                }
            });
        }

        Class<?> unopened = Class.forName("com.facebook.messaging.threadview.jumppill.UnopenedContentPillController", false, loader);
        Class<?> immutableList = Class.forName("com.google.common.collect.ImmutableList", false, loader);
        Method showUnopened = unopened.getDeclaredMethod("show", immutableList);
        Method hideUnopened = unopened.getDeclaredMethod("hide");
        hideUnopened.setAccessible(true);
        XposedBridge.hookMethod(showUnopened, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam param) {
                if (!enabled.getAsBoolean()) return;
                try { hideUnopened.invoke(param.thisObject); } catch (ReflectiveOperationException | RuntimeException failure) { logFailure("hide unopened pill", failure); }
                param.setResult(null);
            }
        });
        XposedBridge.log("FacebookGoodies: Messenger unread pill hooks installed");
    }

    private void installUnreadDivider() throws ReflectiveOperationException {
        if (build == MessengerBuild.MESSENGER_584_1) {
            Class<?> component = Class.forName("X.9g1", false, loader);
            Class<?> context = Class.forName("X.2DF", false, loader);
            Method render = component.getDeclaredMethod("render", context);
            if (render.getReturnType() != Class.forName("X.1J7", false, loader))
                throw new NoSuchMethodException("Messenger unread divider render contract changed");
            XposedBridge.hookMethod(render, new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) {
                    if (!enabled.getAsBoolean()) return;
                    param.setResult(null);
                    if (dividerBlocks.incrementAndGet() <= 3)
                        XposedBridge.log("FacebookGoodies: Messenger unread divider removed");
                }
            });
        } else {
            boolean messenger586 = build == MessengerBuild.MESSENGER_586;
            Class<?> rows = Class.forName(messenger586 ? "X.BGc" : "X.BLI", false, loader);
            String createRowsName = messenger586 ? "AYs" : "AY8";
            Method createRows = rows.getDeclaredMethod(createRowsName);
            if (createRows.getReturnType() != ArrayList.class)
                throw new NoSuchMethodException("Messenger unread divider row contract changed");
            Class<?> divider = Class.forName(messenger586 ? "X.BGp" : "X.BBe", false, loader);
            XposedBridge.hookMethod(createRows, new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam param) {
                    if (!enabled.getAsBoolean() || !(param.getResult() instanceof ArrayList<?>)) return;
                    ArrayList<?> result = (ArrayList<?>) param.getResult();
                    int removed = 0;
                    for (int i = result.size() - 1; i >= 0; i--) {
                        if (divider.isInstance(result.get(i))) {
                            result.remove(i);
                            removed++;
                        }
                    }
                    if (removed > 0 && dividerBlocks.incrementAndGet() <= 3)
                        XposedBridge.log("FacebookGoodies: Messenger unread divider removed");
                }
            });
        }
        XposedBridge.log("FacebookGoodies: Messenger unread divider hook installed");
    }

    private void observe(Activity activity) {
        FrameLayout root = activity.findViewById(android.R.id.content);
        if (root == null) return;
        if (activityLogs.incrementAndGet() <= 5)
            XposedBridge.log("FacebookGoodies: Messenger resumed activity="
                    + activity.getClass().getName());
        Activity old = currentActivity.get();
        if (old != null && old != activity) stopObserving(old);
        if (old == activity && layoutListener != null && root.getViewTreeObserver().isAlive())
            root.getViewTreeObserver().removeOnGlobalLayoutListener(layoutListener);
        currentActivity = new WeakReference<>(activity);
        layoutListener = this::requestChatScan;
        root.getViewTreeObserver().addOnGlobalLayoutListener(layoutListener);
        Runnable initialScan = () -> {
            if (currentActivity.get() == activity) scanChat(root);
        };
        root.post(initialScan);
        root.postDelayed(initialScan, 200);
        root.postDelayed(initialScan, 800);
        root.postDelayed(initialScan, 1800);
    }

    void requestChatScan() {
        if (!scanPending.compareAndSet(false, true)) return;
        main.postDelayed(() -> {
            scanPending.set(false);
            rescanCurrentChat();
        }, 80);
    }

    private void rescanCurrentChat() {
        Activity activity = currentActivity.get();
        if (activity == null) return;
        FrameLayout root = activity.findViewById(android.R.id.content);
        if (root != null) scanChat(root);
    }

    private void stopObserving(Activity activity) {
        FrameLayout root = activity.findViewById(android.R.id.content);
        if (root != null && layoutListener != null && root.getViewTreeObserver().isAlive())
            root.getViewTreeObserver().removeOnGlobalLayoutListener(layoutListener);
        layoutListener = null;
        scanPending.set(false);
        chatVisible = false;
        ambiguousChat = false;
        activeSdkId = null;
        if (currentActivity.get() == activity) currentActivity = new WeakReference<>(null);
        bindComposer(null);
        updateButtonVisibility();
    }

    private void scanChat(FrameLayout root) {
        EditText editor = findComposer(root);
        boolean composer = editor != null;
        if (!composer && !pending.isEmpty() && missingComposerScans.incrementAndGet() <= 3)
            XposedBridge.log("FacebookGoodies: Messenger chat scan awaiting composer");
        if (composer) {
            String thread = visibleThread.apply(root);
            String mountedSdkId = thread == null ? null : sdkByThread.get(thread);
            if (mountedSdkId != null) {
                activeSdkId = mountedSdkId;
                ambiguousChat = false;
                if (mappedChatScans.incrementAndGet() <= 3)
                    XposedBridge.log("FacebookGoodies: Messenger visible chat mapped to read receipt");
            } else if (!chatVisible) {
                boolean recent = SystemClock.elapsedRealtime() - recentBlockedAt < 30000;
                activeSdkId = recent ? recentSdkId : null;
                ambiguousChat = false;
            }
        } else {
            activeSdkId = null;
            ambiguousChat = false;
        }
        chatVisible = composer;
        bindComposer(editor);
        updateButtonVisibility();
    }

    private EditText findComposer(View view) {
        if (!view.isShown()) return null;
        Rect bounds = new Rect();
        if (view instanceof EditText && view.getGlobalVisibleRect(bounds)
                && findEmoji((EditText) view) != null)
            return (EditText) view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                EditText editor = findComposer(group.getChildAt(i));
                if (editor != null) return editor;
            }
        }
        return null;
    }

    void updateButtonVisibility() {
        View emoji = currentEmoji.get();
        if (emoji != null) emoji.post(() -> {
            if (currentEmoji.get() != emoji) return;
            setNoticeVisible(emoji, enabled.getAsBoolean() && chatVisible && !ambiguousChat
                    && activeSdkId != null && pending.containsKey(activeSdkId));
        });
    }

    private void bindComposer(EditText editor) {
        View emoji = findEmoji(editor);
        View oldEmoji = currentEmoji.get();
        if (oldEmoji != emoji) {
            if (oldEmoji != null) setNoticeVisible(oldEmoji, false);
            currentEmoji = new WeakReference<>(emoji);
        }
        currentComposer = new WeakReference<>(editor);
    }

    private View findEmoji(EditText editor) {
        if (editor == null || !(editor.getParent() instanceof ViewGroup)) return null;
        ViewGroup host = (ViewGroup) editor.getParent();
        if (!"com.facebook.litho.ComponentHost".equals(host.getClass().getName())) return null;
        int tolerance = dp(editor.getContext(), 2);
        View closest = null;
        int closestGap = Integer.MAX_VALUE;
        for (int index = 0; index < host.getChildCount(); index++) {
            View candidate = host.getChildAt(index);
            if (candidate == editor
                    || !"com.facebook.litho.ComponentHost".equals(candidate.getClass().getName())) continue;
            int gap = candidate.getLeft() - editor.getRight();
            int distance = Math.abs(gap);
            if (distance <= tolerance && distance < closestGap) {
                closest = candidate;
                closestGap = distance;
            }
        }
        return closest;
    }

    private void setNoticeVisible(View emoji, boolean visible) {
        View host = badgeHost.get();
        if (!visible) {
            if (pendingBadge != null) {
                if (host != null) {
                    host.getOverlay().remove(pendingBadge);
                    host.setLongClickable(originalEmojiLongClickable);
                }
                pendingBadge = null;
                badgeHost = new WeakReference<>(null);
            }
            return;
        }
        if (pendingBadge != null && host != emoji) {
            if (host != null) {
                host.getOverlay().remove(pendingBadge);
                host.setLongClickable(originalEmojiLongClickable);
            }
            pendingBadge = null;
        }
        if (pendingBadge == null) {
            originalEmojiLongClickable = emoji.isLongClickable();
            GradientDrawable badge = new GradientDrawable();
            badge.setShape(GradientDrawable.OVAL);
            EditText editor = currentComposer.get();
            badge.setColor(editor == null ? 0xff276cff : editor.getHighlightColor() | 0xff000000);
            badge.setStroke(dp(emoji.getContext(), 1), Color.WHITE);
            pendingBadge = badge;
            badgeHost = new WeakReference<>(emoji);
            if (badgeMounts.incrementAndGet() <= 3)
                XposedBridge.log("FacebookGoodies: Messenger seen badge mounted");
        }
        // Messenger can reuse the composer view while clearing or replacing its overlay.
        // Reattaching makes this refresh safe when the view identity stays the same.
        emoji.getOverlay().remove(pendingBadge);
        emoji.getOverlay().add(pendingBadge);
        int size = dp(emoji.getContext(), 9);
        int inset = dp(emoji.getContext(), 1);
        pendingBadge.setBounds(emoji.getWidth() - size - inset, inset, emoji.getWidth() - inset, inset + size);
        emoji.setLongClickable(true);
        emoji.invalidate();
    }

    private void sendForCurrentThread() {
        Pending request = null;
        try {
            String sdkId = activeSdkId;
            request = sdkId == null || ambiguousChat ? null : pending.get(sdkId);
            if (request == null) {
                toast("No pending read receipt to send");
                return;
            }
            if (!request.sending.compareAndSet(false, true)) return;
            long sentWatermark = MessengerSeenBorderPolicy.sentWatermark(request.readTime,
                    request.watermark, lastMessageFor(request.sdkId));
            Object success = callback(request, true, sentWatermark);
            Object failure = callback(request, false, sentWatermark);
            pending.remove(request.sdkId, request);
            updateButtonVisibility();
            XposedBridge.invokeOriginalMethod(sendMethod, request.sender,
                    new Object[]{request.readTime, request.sdkId, request.mutationId, success, failure});
            XposedBridge.log("FacebookGoodies: Messenger manual seen requested");
        } catch (Throwable failure) {
            if (request != null) {
                request.sending.set(false);
                pending.put(request.sdkId, request);
            }
            updateButtonVisibility();
            logFailure("manual seen", failure);
            toast("Couldn't send read receipt");
        }
    }

    private Object callback(Pending request, boolean success, long sentWatermark) {
        return Proxy.newProxyInstance(loader, new Class<?>[]{function0}, (proxy, method, args) -> {
            if ("invoke".equals(method.getName())) {
                if (success) rememberSent(request, sentWatermark);
                else {
                    request.sending.set(false);
                    pending.put(request.sdkId, request);
                }
                main.post(() -> {
                    updateButtonVisibility();
                    borderRefresh.run();
                    toast(success ? "Read receipt sent" : "Couldn't send read receipt");
                });
                return null;
            }
            if ("getArity".equals(method.getName())) return 0;
            if ("hashCode".equals(method.getName())) return System.identityHashCode(proxy);
            if ("equals".equals(method.getName())) return args != null && args.length == 1 && proxy == args[0];
            return "FacebookGoodies manual seen callback";
        });
    }

    private void applyLocalRead(Object summary) {
        if (!localReadEnabled.getAsBoolean()) return;
        try {
            Object key = summaryThread.get(summary);
            if (key == null) return;
            String thread = key.toString();
            long local = localReads.getLong(prefKey(thread), 0L);
            if (local > summaryReadTime.getLong(summary)) {
                appliedLocalReads.record(thread, local);
                summaryReadTime.setLong(summary, local);
            }
        } catch (ReflectiveOperationException | RuntimeException failure) {
            logFailure("local read model", failure);
        }
    }

    private void rememberLocalRead(String key, Long readTime) {
        if (!localReadEnabled.getAsBoolean()) return;
        long now = System.currentTimeMillis();
        long time = readTime != null && readTime > 0 ? Math.min(readTime, now) : now;
        String pref = prefKey(key);
        if (time > localReads.getLong(pref, 0L)) localReads.edit().putLong(pref, time).apply();
        WeakReference<Object> reference = liveSummaries.get(key);
        if (reference != null && reference.get() != null) applyLocalRead(reference.get());
    }

    private Long lastMessageFor(String sdkId) {
        String thread = threadBySdk.get(sdkId);
        return thread == null ? null : lastMessageByThread.get(thread);
    }

    private boolean alreadySent(String sdkId, Long readTime) {
        return sdkId != null && readTime != null && readTime > 0
                && readTime <= localReads.getLong("sent_" + prefKey(sdkId), 0L);
    }

    private void rememberSent(Pending request, long readTime) {
        if (request.sdkId != null && readTime > 0) {
            synchronized (localReads) {
                String pref = "sent_" + prefKey(request.sdkId);
                if (readTime > localReads.getLong(pref, 0L)) localReads.edit().putLong(pref, readTime).apply();
                String thread = threadBySdk.get(request.sdkId);
                if (thread != null) {
                    String confirmed = "confirmed_" + prefKey(thread);
                    if (readTime > localReads.getLong(confirmed, 0L))
                        localReads.edit().putLong(confirmed, readTime).apply();
                }
            }
        }
        Pending current = pending.get(request.sdkId);
        if (current != null && (current == request || alreadySent(current.sdkId, current.watermark)))
            pending.remove(request.sdkId, current);
    }

    private long confirmedReadFor(String thread) {
        String key = prefKey(thread);
        long confirmed = localReads.getLong("confirmed_" + key, 0L);
        String sdkId = sdkByThread.get(thread);
        if (sdkId != null)
            confirmed = Math.max(confirmed, localReads.getLong("sent_" + prefKey(sdkId), 0L));
        return Math.max(confirmed, localReads.getLong("initial_" + key, 0L));
    }

    private void observeSyncedRead(String thread, long observed, boolean liveStream) {
        String key = prefKey(thread);
        String confirmed = "confirmed_" + key;
        synchronized (localReads) {
            long previous = localReads.getLong(confirmed, 0L);
            boolean localValue = appliedLocalReads.contains(thread, observed);
            long next = MessengerSeenBorderPolicy.syncedRead(observed, previous, liveStream, localValue);
            if (next <= previous) return;
            localReads.edit().putLong(confirmed, next).apply();
            appliedLocalReads.discardThrough(thread, next);
        }
        main.post(borderRefresh);
    }

    private void observeInitialRead(String thread, long observed) {
        try {
            String key = prefKey(thread);
            String initial = "initial_" + key;
            if (localReads.contains(initial)) return;
            long local = localReads.getLong(key, 0L);
            long next = MessengerSeenBorderPolicy.initialRead(observed, local, 0L);
            localReads.edit().putLong(initial, next).apply();
            if (next > 0) main.post(borderRefresh);
        } catch (RuntimeException failure) { logFailure("initial read watermark", failure); }
    }

    private static String prefKey(String thread) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(thread.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder("thread_");
            for (byte value : hash) out.append(Character.forDigit((value >>> 4) & 15, 16)).append(Character.forDigit(value & 15, 16));
            return out.toString();
        } catch (NoSuchAlgorithmException failure) { throw new IllegalStateException(failure); }
    }

    private static int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    private void toast(String message) { main.post(() -> Toast.makeText(app, message, Toast.LENGTH_SHORT).show()); }

    private void logFailure(String point, Throwable failure) {
        errors.incrementAndGet();
        XposedBridge.log("FacebookGoodies: Messenger " + point + " failed: " + failure.getClass().getSimpleName());
    }

    private static <T> Map<String, T> boundedMap() { return boundedMap(64); }

    private static <T> Map<String, T> boundedMap(int limit) {
        return Collections.synchronizedMap(new LinkedHashMap<String, T>(64, .75f, true) {
            @Override protected boolean removeEldestEntry(Map.Entry<String, T> eldest) { return size() > limit; }
        });
    }

    private static final class Pending {
        final Object sender;
        final Long readTime;
        final Long watermark;
        final String sdkId;
        final String mutationId;
        final AtomicBoolean sending = new AtomicBoolean();

        Pending(Object sender, Long readTime, Long watermark, String sdkId, String mutationId) {
            this.sender = sender;
            this.readTime = readTime;
            this.watermark = watermark;
            this.sdkId = sdkId;
            this.mutationId = mutationId;
        }
    }
}
