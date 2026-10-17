package ymg.pwcca.facebookgoodies;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import java.util.concurrent.atomic.AtomicBoolean;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class FacebookModule implements IXposedHookLoadPackage {
    private static final AtomicBoolean started = new AtomicBoolean();
    @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam packageInfo) {
        if (!"com.facebook.katana".equals(packageInfo.packageName) || !"com.facebook.katana".equals(packageInfo.processName)) return;
        XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                if (!started.compareAndSet(false, true)) return;
                Context app = (Application) param.thisObject;
                initialize(app);
            }
        });
    }
    private static void initialize(Context app) {
        FacebookBuild build = hostBuild(app);
        HostState state = new HostState(); state.supported = build != FacebookBuild.UNSUPPORTED; state.start(app);
        if (!state.supported) { XposedBridge.log("FacebookGoodies: unsupported host build; hooks disabled"); return; }
        ClassLoader loader = app.getClassLoader();
        if (build == FacebookBuild.FACEBOOK_582) install582((Application) app, loader, state);
        else install583((Application) app, loader, state);
        try { PrivacyHooks.install(loader, state); } catch (RuntimeException | LinkageError failure) { state.failure("privacy-install"); }
        XposedBridge.log("FacebookGoodies: initialized hooks=" + state.hooks.get());
    }
    private static void install582(Application app, ClassLoader loader, HostState state) {
        try { AdHooks.install(loader, state); } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) { state.failure("feed-install"); }
        VideoAdHooks.install(loader, state);
        FeedContentHooks.install(loader, state);
        VideoDownloadHooks.install(app, loader, state);
    }
    private static void install583(Application app, ClassLoader loader, HostState state) {
        try { AdHooks583.install(loader, state); } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) { state.failure("feed-install"); }
        VideoAdHooks583.install(loader, state);
        FeedContentHooks583.install(loader, state);
        VideoDownloadHooks583.install(app, loader, state);
    }
    private static FacebookBuild hostBuild(Context app) {
        try {
            PackageInfo info = app.getPackageManager().getPackageInfo("com.facebook.katana", 0);
            return FacebookBuild.identify(info.versionName, info.getLongVersionCode());
        } catch (android.content.pm.PackageManager.NameNotFoundException failure) { return FacebookBuild.UNSUPPORTED; }
    }
}
