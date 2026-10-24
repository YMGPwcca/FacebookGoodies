package ymg.pwcca.facebookgoodies;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

final class DownloadSidebarHooks584 {
    private final HostState state;
    private final NativeDownloadRow584 row;
    private final Field icon, iconType, renderContext, builderComponent, handlerId, children;
    private final Class<?> iconModel, flex;
    private final Field dynamicModel, dynamicIcon, dynamicIconType, dynamicSurface;
    private final Class<?> dynamicIconModel;
    private final ThreadLocal<Scope> scope = new ThreadLocal<>();
    DownloadSidebarHooks584(ClassLoader loader, HostState state, Runnable download) throws ReflectiveOperationException {
        this.state = state; row = new NativeDownloadRow584(loader, download);
        icon = loader.loadClass("X.93B").getDeclaredField("A03");
        iconModel = loader.loadClass("X.93E"); iconType = iconModel.getDeclaredField("A00");
        renderContext = loader.loadClass("X.28P").getDeclaredField("A0D");
        builderComponent = loader.loadClass("X.3T3").getDeclaredField("A00");
        handlerId = loader.loadClass("X.enB").getDeclaredField("A01");
        flex = loader.loadClass("X.460"); children = flex.getDeclaredField("A0A");
        children.setAccessible(true);
        dynamicModel = loader.loadClass("X.37P").getDeclaredField("A01");
        dynamicIcon = loader.loadClass("X.36k").getDeclaredField("A02");
        dynamicIconModel = loader.loadClass("X.36j"); dynamicIconType = dynamicIconModel.getDeclaredField("A00");
        dynamicSurface = loader.loadClass("X.37P").getDeclaredField("A04");
    }
    void install(ClassLoader loader) throws ReflectiveOperationException {
        hook(loader.loadClass("X.93B").getDeclaredMethod("render", loader.loadClass("X.28P"), loader.loadClass("X.2Ae")), reelsCallback());
        hook(loader.loadClass("X.3T3").getDeclaredMethod("A1Y", loader.loadClass("X.enB")), captureMoreCallback());
        hook(loader.loadClass("X.ThN").getDeclaredMethod("A0l", loader.loadClass("X.3tD")), videoCallback());
        hook(loader.loadClass("X.37P").getDeclaredMethod("render", loader.loadClass("X.28P"), loader.loadClass("X.2Ae")), dynamicCallback());
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
                } catch (ReflectiveOperationException | RuntimeException failure) { state.failure("download-sidebar-reels"); }
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
                } catch (ReflectiveOperationException | RuntimeException failure) { state.failure("download-sidebar-dynamic"); }
            }
        };
    }
    private XC_MethodHook captureMoreCallback() {
        return new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                Scope active = scope.get();
                if (active == null || param.hasThrowable() || param.args[0] == null) return;
                try {
                    if (handlerId.getInt(param.args[0]) == 36369512) active.more = builderComponent.get(param.thisObject);
                } catch (IllegalAccessException failure) { state.failure("download-sidebar-more"); }
            }
        };
    }
    private XC_MethodHook videoCallback() {
        return new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam param) {
                Scope active = new Scope(param.args[0], scope.get()); scope.set(active);
                param.setObjectExtra("goodies-sidebar", active);
            }
            @Override protected void afterHookedMethod(MethodHookParam param) {
                Scope active = (Scope) param.getObjectExtra("goodies-sidebar");
                if (active == null) return;
                try {
                    if (!param.hasThrowable() && state.options.downloads && active.more != null && insert(param.getResult(), active, 0))
                        state.downloadSidebarRows.incrementAndGet();
                } catch (ReflectiveOperationException | RuntimeException failure) { state.failure("download-sidebar-video"); }
                finally { if (active.previous == null) scope.remove(); else scope.set(active.previous); }
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
                updated.set(i, row.aboveMore(active.context, child)); children.set(component, updated); return true;
            }
            if (insert(child, active, depth + 1)) return true;
        }
        return false;
    }
    private void hook(Method method, XC_MethodHook callback) {
        XposedBridge.hookMethod(method, callback); state.hooks.incrementAndGet(); state.downloadHooks.incrementAndGet();
    }
    private static final class Scope {
        final Object context;
        final Scope previous;
        Object more;
        Scope(Object context, Scope previous) { this.context = context; this.previous = previous; }
    }
}
