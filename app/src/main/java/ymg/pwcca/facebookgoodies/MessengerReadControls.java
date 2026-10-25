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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

final class MessengerReadControls {
    private final Context app;
    private final ClassLoader loader;
    private final BooleanSupplier enabled;
    private final SharedPreferences localReads;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final AtomicInteger blocked = new AtomicInteger();
    private final AtomicInteger dividerBlocks = new AtomicInteger();
    private final AtomicInteger installedGroups = new AtomicInteger();
    private final AtomicInteger errors = new AtomicInteger();
    private final Map<String, String> threadBySdk = boundedMap();
    private final Map<String, Pending> pending = boundedMap();
    private final Map<String, WeakReference<Object>> liveSummaries = boundedMap();
    private final Map<String, Long> lastMessageByThread = boundedMap();
    private final AtomicBoolean scanPending = new AtomicBoolean();
    private WeakReference<EditText> currentComposer = new WeakReference<>(null);
    private WeakReference<View> currentEmoji = new WeakReference<>(null);
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

    MessengerReadControls(Context app, ClassLoader loader, BooleanSupplier enabled) {
        this.app = app;
        this.loader = loader;
        this.enabled = enabled;
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

    private void installReadState() throws ReflectiveOperationException {
        Class<?> threadKey = Class.forName("com.facebook.messaging.model.threadkey.ThreadKey", false, loader);
        Class<?> converter = Class.forName("X.7gJ", false, loader);
        Method convert = converter.getDeclaredMethod("A01", threadKey);
        XposedBridge.hookMethod(convert, new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                if (!enabled.getAsBoolean() || !(param.getResult() instanceof String) || param.args[0] == null) return;
                String key = param.args[0].toString();
                String sdkId = (String) param.getResult();
                threadBySdk.put(sdkId, key);
            }
        });

        Class<?> summary = Class.forName("com.facebook.messaging.model.threads.ThreadSummary", false, loader);
        summaryThread = summary.getDeclaredField("A0e");
        summaryReadTime = summary.getDeclaredField("A0C");
        summaryLastMessage = summary.getDeclaredField("A0A");
        summaryThread.setAccessible(true);
        summaryReadTime.setAccessible(true);
        summaryLastMessage.setAccessible(true);
        XposedBridge.hookAllConstructors(summary, new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                if (!enabled.getAsBoolean()) return;
                try {
                    Object key = summaryThread.get(param.thisObject);
                    if (key != null) {
                        String thread = key.toString();
                        liveSummaries.put(thread, new WeakReference<>(param.thisObject));
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

    private void installSend() throws ReflectiveOperationException {
        Class<?> sender = Class.forName("X.9iU", false, loader);
        function0 = Class.forName("kotlin.jvm.functions.Function0", false, loader);
        Method invoke = function0.getDeclaredMethod("invoke");
        sendMethod = sender.getDeclaredMethod("A00", Long.class, String.class, String.class, function0, function0);
        if (sendMethod.getReturnType() != void.class) throw new NoSuchMethodException("Messenger mark-read contract changed");
        XposedBridge.hookMethod(sendMethod, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam param) {
                if (!enabled.getAsBoolean()) return;
                String sdkId = (String) param.args[1];
                Long readTime = (Long) param.args[0];
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
                String key = threadBySdk.get(sdkId);
                if (key != null) rememberLocalRead(key, readTime);
                try {
                    invoke.invoke(param.args[3]);
                } catch (ReflectiveOperationException | RuntimeException failure) {
                    logFailure("mark-read completion", failure);
                }
                param.setResult(null);
                main.post(MessengerReadControls.this::updateButtonVisibility);
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
        XposedBridge.log("FacebookGoodies: Messenger unread divider hook installed");
    }

    private void observe(Activity activity) {
        FrameLayout root = activity.findViewById(android.R.id.content);
        if (root == null) return;
        Activity old = currentActivity.get();
        if (old != null && old != activity) stopObserving(old);
        if (old == activity && layoutListener != null && root.getViewTreeObserver().isAlive())
            root.getViewTreeObserver().removeOnGlobalLayoutListener(layoutListener);
        currentActivity = new WeakReference<>(activity);
        layoutListener = () -> {
            if (scanPending.compareAndSet(false, true)) root.postDelayed(() -> {
                scanPending.set(false);
                if (currentActivity.get() == activity) scanChat(root);
            }, 120);
        };
        root.getViewTreeObserver().addOnGlobalLayoutListener(layoutListener);
        root.post(() -> scanChat(root));
    }

    private void stopObserving(Activity activity) {
        FrameLayout root = activity.findViewById(android.R.id.content);
        if (root != null && layoutListener != null && root.getViewTreeObserver().isAlive())
            root.getViewTreeObserver().removeOnGlobalLayoutListener(layoutListener);
        layoutListener = null;
        chatVisible = false;
        ambiguousChat = false;
        activeSdkId = null;
        bindComposer(null);
        updateButtonVisibility();
    }

    private void scanChat(FrameLayout root) {
        EditText editor = findComposer(root);
        boolean composer = editor != null;
        if (composer && !chatVisible) {
            boolean recent = SystemClock.elapsedRealtime() - recentBlockedAt < 30000;
            activeSdkId = recent ? recentSdkId : null;
            ambiguousChat = false;
        } else if (!composer) {
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
        if (!"com.facebook.litho.ComponentHost".equals(host.getClass().getName())
                || host.getChildCount() != 2 || host.getChildAt(0) != editor) return null;
        View emoji = host.getChildAt(1);
        return "com.facebook.litho.ComponentHost".equals(emoji.getClass().getName())
                && Math.abs(emoji.getLeft() - editor.getRight()) <= dp(editor.getContext(), 2) ? emoji : null;
    }

    private void setNoticeVisible(View emoji, boolean visible) {
        if (!visible) {
            if (pendingBadge != null) {
                emoji.getOverlay().remove(pendingBadge);
                pendingBadge = null;
                emoji.setLongClickable(originalEmojiLongClickable);
            }
            return;
        }
        if (pendingBadge == null) {
            originalEmojiLongClickable = emoji.isLongClickable();
            GradientDrawable badge = new GradientDrawable();
            badge.setShape(GradientDrawable.OVAL);
            EditText editor = currentComposer.get();
            badge.setColor(editor == null ? 0xff276cff : editor.getHighlightColor() | 0xff000000);
            badge.setStroke(dp(emoji.getContext(), 1), Color.WHITE);
            pendingBadge = badge;
            emoji.getOverlay().add(badge);
        }
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
                toast("Chưa có trạng thái đã xem để gửi");
                return;
            }
            if (!request.sending.compareAndSet(false, true)) return;
            Object success = callback(request, true);
            Object failure = callback(request, false);
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
            toast("Không gửi được trạng thái đã xem");
        }
    }

    private Object callback(Pending request, boolean success) {
        return Proxy.newProxyInstance(loader, new Class<?>[]{function0}, (proxy, method, args) -> {
            if ("invoke".equals(method.getName())) {
                if (success) rememberSent(request);
                else {
                    request.sending.set(false);
                    pending.put(request.sdkId, request);
                }
                main.post(() -> {
                    updateButtonVisibility();
                    toast(success ? "Đã gửi trạng thái đã xem" : "Không gửi được trạng thái đã xem");
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
        try {
            Object key = summaryThread.get(summary);
            if (key == null) return;
            long local = localReads.getLong(prefKey(key.toString()), 0L);
            if (local > summaryReadTime.getLong(summary)) summaryReadTime.setLong(summary, local);
        } catch (ReflectiveOperationException | RuntimeException failure) {
            logFailure("local read model", failure);
        }
    }

    private void rememberLocalRead(String key, Long readTime) {
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

    private void rememberSent(Pending request) {
        Long readTime = request.watermark;
        if (request.sdkId != null && readTime != null && readTime > 0) {
            String pref = "sent_" + prefKey(request.sdkId);
            if (readTime > localReads.getLong(pref, 0L)) localReads.edit().putLong(pref, readTime).apply();
        }
        Pending current = pending.get(request.sdkId);
        if (current != null && (current == request || alreadySent(current.sdkId, current.watermark)))
            pending.remove(request.sdkId, current);
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

    private static <T> Map<String, T> boundedMap() {
        return Collections.synchronizedMap(new LinkedHashMap<String, T>(64, .75f, true) {
            @Override protected boolean removeEldestEntry(Map.Entry<String, T> eldest) { return size() > 64; }
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
