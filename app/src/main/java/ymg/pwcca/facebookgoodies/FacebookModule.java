package ymg.pwcca.facebookgoodies;

import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.pm.PackageInfo;
import java.util.concurrent.atomic.AtomicBoolean;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class FacebookModule implements IXposedHookLoadPackage {
    private static final String FACEBOOK = "com.facebook.katana";
    private static final String MESSENGER = "com.facebook.orca";
    private static final AtomicBoolean started = new AtomicBoolean();
    @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam packageInfo) {
        String target = packageInfo.packageName;
        if ((!FACEBOOK.equals(target) && !MESSENGER.equals(target)) || !target.equals(packageInfo.processName)) return;
        // Application.attach runs before LoadedApk publishes the Application instance.
        // Host apps can start workers in that window; wait until the framework calls onCreate.
        XposedHelpers.findAndHookMethod(Instrumentation.class, "callApplicationOnCreate", Application.class, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam param) {
                Application app = (Application) param.args[0];
                if (!target.equals(app.getPackageName())) return;
                if (app.getApplicationContext() == null) {
                    XposedBridge.log("FacebookGoodies: application context unavailable; hooks disabled");
                    return;
                }
                if (!started.compareAndSet(false, true)) return;
                try {
                    if (MESSENGER.equals(target)) initializeMessenger(app);
                    else initialize(app);
                }
                catch (RuntimeException | LinkageError failure) { XposedBridge.log("FacebookGoodies: startup incomplete"); }
            }
        });
    }
    private static void initialize(Application app) {
        FacebookBuild build = hostBuild(app);
        HostState state = new HostState(); state.supported = build != FacebookBuild.UNSUPPORTED;
        if (!state.supported) {
            state.start(app);
            XposedBridge.log("FacebookGoodies: unsupported host build; hooks disabled");
            return;
        }
        ClassLoader loader = app.getClassLoader();
        if (build == FacebookBuild.FACEBOOK_582) install582(app, loader, state);
        else if (build == FacebookBuild.FACEBOOK_583) install583(app, loader, state);
        else if (build == FacebookBuild.FACEBOOK_585) install585(app, loader, state, build);
        else if (build == FacebookBuild.FACEBOOK_586) install586(app, loader, state, build);
        else install584(app, loader, state, build);
        try { PrivacyHooks.install(loader, state); } catch (RuntimeException | LinkageError failure) { state.failure("privacy-install"); }
        state.start(app);
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
    private static void install584(Application app, ClassLoader loader, HostState state, FacebookBuild build) {
        try { AdHooks584.install(loader, state); } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) { state.failure("feed-install"); }
        VideoAdHooks584.install(loader, state);
        FeedContentHooks584.install(loader, state, build);
        VideoDownloadHooks584.install(app, loader, state);
    }
    private static void install585(Application app, ClassLoader loader, HostState state, FacebookBuild build) {
        try { AdHooks585.install(loader, state); } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) { state.failure("feed-install"); }
        VideoAdHooks585.install(loader, state);
        FeedContentHooks585.install(loader, state, build);
        VideoDownloadHooks585.install(app, loader, state);
    }
    private static void install586(Application app, ClassLoader loader, HostState state, FacebookBuild build) {
        try { AdHooks586.install(loader, state); } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) { state.failure("feed-install"); }
        VideoAdHooks586.install(loader, state);
        FeedContentHooks586.install(loader, state, build);
        VideoDownloadHooks586.install(app, loader, state);
    }
    private static FacebookBuild hostBuild(Context app) {
        try {
            PackageInfo info = app.getPackageManager().getPackageInfo(FACEBOOK, 0);
            return FacebookBuild.identify(info.versionName, info.getLongVersionCode());
        } catch (android.content.pm.PackageManager.NameNotFoundException failure) { return FacebookBuild.UNSUPPORTED; }
    }
    private static void initializeMessenger(Application app) {
        MessengerBuild build;
        try {
            PackageInfo info = app.getPackageManager().getPackageInfo(MESSENGER, 0);
            build = MessengerBuild.identify(info.versionName, info.getLongVersionCode());
            if (build == MessengerBuild.UNSUPPORTED) {
                XposedBridge.log("FacebookGoodies: unsupported Messenger build; hooks disabled");
                return;
            }
        } catch (android.content.pm.PackageManager.NameNotFoundException | RuntimeException failure) {
            XposedBridge.log("FacebookGoodies: Messenger build unavailable");
            return;
        }
        boolean aiFabInstalled = false;
        boolean hideAi = true;
        try {
            android.os.Bundle config = app.getContentResolver().call(SettingsProvider.URI, "config", null, null);
            if (config != null) hideAi = config.getBoolean("messenger_hide_ai", true);
        } catch (RuntimeException ignored) { }
        if (hideAi) {
            try {
                MessengerAiFabHooks.install(app.getClassLoader(), build);
                aiFabInstalled = true;
                XposedBridge.log("FacebookGoodies: Messenger AI FAB hook installed");
            } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
                XposedBridge.log("FacebookGoodies: Messenger AI FAB hook unavailable");
            }
        }
        MessengerPrivacyHooks.install(app, app.getClassLoader(), aiFabInstalled, build);
    }
}
