package ymg.pwcca.facebookgoodies;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

/**
 * Makes Dav1d declare the limited (video) range of its 8-bit YV12 output.
 *
 * Facebook ships {@code skipSetDataSpaceForSDR = true}, so the renderer never calls
 * ANativeWindow_setBuffersDataSpace for SDR and the YV12 buffer keeps the default
 * full-range sRGB tag. Hardware overlays then expand 16-235 to 0-255 and the video
 * looks washed out until a GPU-composited overlay (volume panel) forces the
 * limited-to-full conversion shader.
 */
final class VideoColorRangeHooks {
    private static final String SETTING = "exoplayer2.av1.src.Dav1dMediaCodecAdapterSetting";
    private static final String SKIP = "skipSetDataSpaceForSDR";

    static void install(ClassLoader loader, HostState state) {
        Class<?> setting;
        try { setting = Class.forName(SETTING, false, loader); }
        catch (ClassNotFoundException missing) { throw new IllegalStateException("Dav1d setting unavailable", missing); }
        XposedBridge.hookAllConstructors(setting, new XC_MethodHook() {
            private boolean reported;
            @Override protected void afterHookedMethod(MethodHookParam param) {
                if (!state.options.videoRange) return;
                try {
                    XposedHelpers.setBooleanField(param.thisObject, SKIP, false);
                    state.videoRange.incrementAndGet();
                } catch (Throwable failure) {
                    if (reported) return;
                    reported = true;
                    state.failure("video-range");
                }
            }
        });
        state.hooks.incrementAndGet();
    }
}
