package ymg.pwcca.facebookgoodies;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

final class DownloadSidebarHooks587 {
    private static final int VIDEO_MORE_EVENT_ID = 36369512;
    private final HostState state;
    private final NativeDownloadRow587 row;
    private final Field icon, iconType, renderContext, builderComponent, handlerId, children;
    private final Class<?> iconModel, flex;
    private final Field dynamicModel, dynamicIcon, dynamicIconType, dynamicSurface;
    private final Class<?> dynamicIconModel;
    private final ThreadLocal<Scope> scope = new ThreadLocal<>();

    DownloadSidebarHooks587(FacebookHostBindings loader, HostState state, Runnable download) throws ReflectiveOperationException {
        this.state = state;
        row = new NativeDownloadRow587(loader, download);
        icon = field(loader, loader.type("X.0CPN"), "A04", loader.type("X.0D8k"));
        iconModel = loader.type("X.0CPQ");
        iconType = field(loader, iconModel, "A00", loader.type("X.01Xh"));
        renderContext = field(loader, loader.type("X.02FH"), "A0D", loader.type("X.01Mw"));
        builderComponent = field(loader, loader.type("X.03H1"), "A00", loader.type("X.01LV"));
        handlerId = field(loader, loader.type("X.05yk"), "A01", int.class);
        flex = loader.type("X.03Ix");
        children = field(loader, flex, "A0A", List.class);

        Class<?> dynamic = loader.type("X.03ka");
        dynamicModel = field(loader, dynamic, "A01", loader.type("X.03jg"));
        dynamicIcon = field(loader, loader.type("X.03jg"), "A02", loader.type("X.04Ip"));
        dynamicIconModel = loader.type("X.03jf");
        dynamicIconType = field(loader, dynamicIconModel, "A00", loader.type("X.01Xh"));
        dynamicSurface = field(loader, dynamic, "A03", loader.type("X.03kZ"));
    }

    private static Field field(FacebookHostBindings loader, Class<?> owner, String name, Class<?> type) throws ReflectiveOperationException {
        Field field = loader.field(owner, name);
        if (field.getType() != type) throw new NoSuchFieldException(owner.getName() + "." + name + " type");
        field.setAccessible(true);
        return field;
    }

    void install(FacebookHostBindings loader) throws ReflectiveOperationException {
        Class<?> context = loader.type("X.02FH");
        Class<?> component = loader.type("X.02IJ");
        Class<?> renderContext = loader.type("X.01Mw");
        hook(loader.method(loader.type("X.0CPN"), "render", context, component), reelsCallback());
        hook(loader.method(loader.type("X.03H1"), "A19", loader.type("X.05yk")), captureMoreCallback());
        hook(loader.method(loader.type("X.0Lkl"), "A19", renderContext), videoCallback());
        hook(loader.method(loader.type("X.03ka"), "render", context, component), dynamicCallback());
    }

    private XC_MethodHook reelsCallback() {
        return new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                if (param.hasThrowable() || !state.options.downloads || param.getResult() == null) return;
                try {
                    Object model = icon.get(param.thisObject);
                    if (!iconModel.isInstance(model) || !SidebarAction.moreIcon(((Enum<?>) iconType.get(model)).name())) return;
                    param.setResult(row.aboveMore(renderContext.get(param.args[0]), param.getResult()));
                    state.downloadSidebarRows.incrementAndGet();
                } catch (ReflectiveOperationException | RuntimeException failure) {
                    state.failure("download-sidebar-reels");
                }
            }
        };
    }

    private XC_MethodHook dynamicCallback() {
        return new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                if (param.hasThrowable() || !state.options.downloads || param.getResult() == null) return;
                try {
                    Object model = dynamicModel.get(param.thisObject);
                    if (model == null) return;
                    Object glyph = dynamicIcon.get(model);
                    if (!dynamicIconModel.isInstance(glyph)) return;
                    String icon = ((Enum<?>) dynamicIconType.get(glyph)).name();
                    String surface = ((Enum<?>) dynamicSurface.get(param.thisObject)).name();
                    if (!SidebarAction.mediaMore(icon, surface)) return;
                    param.setResult(row.aboveMore(renderContext.get(param.args[0]), param.getResult()));
                    state.downloadSidebarRows.incrementAndGet();
                } catch (ReflectiveOperationException | RuntimeException failure) {
                    state.failure("download-sidebar-dynamic");
                }
            }
        };
    }

    private XC_MethodHook captureMoreCallback() {
        return new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                Scope active = scope.get();
                if (active == null || param.hasThrowable() || param.args[0] == null) return;
                try {
                    if (handlerId.getInt(param.args[0]) == VIDEO_MORE_EVENT_ID)
                        active.more = builderComponent.get(param.thisObject);
                } catch (IllegalAccessException failure) { state.failure("download-sidebar-more"); }
            }
        };
    }

    private XC_MethodHook videoCallback() {
        return new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam param) {
                Scope active = new Scope(param.args[0], scope.get());
                scope.set(active);
                param.setObjectExtra("goodies-sidebar", active);
            }

            @Override protected void afterHookedMethod(MethodHookParam param) {
                Scope active = (Scope) param.getObjectExtra("goodies-sidebar");
                if (active == null) return;
                try {
                    if (!param.hasThrowable() && state.options.downloads && active.more != null && insert(param.getResult(), active, 0))
                        state.downloadSidebarRows.incrementAndGet();
                } catch (ReflectiveOperationException | RuntimeException failure) {
                    state.failure("download-sidebar-video");
                } finally {
                    if (active.previous == null) scope.remove(); else scope.set(active.previous);
                }
            }
        };
    }

    private boolean insert(Object component, Scope active, int depth) throws ReflectiveOperationException {
        if (component == null || depth > 8 || !flex.isInstance(component)) return false;
        List<?> original = (List<?>) children.get(component);
        if (original == null || original.size() > 64) return false;
        for (int i = 0; i < original.size(); i++) {
            Object child = original.get(i);
            if (child == active.more) {
                List<Object> updated = new ArrayList<>(original);
                updated.set(i, row.aboveMore(active.context, child));
                children.set(component, updated);
                return true;
            }
            if (insert(child, active, depth + 1)) return true;
        }
        return false;
    }

    private void hook(Method method, XC_MethodHook callback) {
        XposedBridge.hookMethod(method, callback);
        state.hooks.incrementAndGet();
        state.downloadHooks.incrementAndGet();
    }

    private static final class Scope {
        final Object context;
        final Scope previous;
        Object more;

        Scope(Object context, Scope previous) { this.context = context; this.previous = previous; }
    }
}
