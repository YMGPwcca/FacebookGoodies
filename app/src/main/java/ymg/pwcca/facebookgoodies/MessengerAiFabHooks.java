package ymg.pwcca.facebookgoodies;

import java.lang.reflect.Method;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

final class MessengerAiFabHooks {
    private MessengerAiFabHooks() { }

    static void install(ClassLoader loader) throws ReflectiveOperationException {
        Class<?> component = Class.forName("X.6if", false, loader);
        Class<?> context = Class.forName("X.2DF", false, loader);
        Method render = component.getDeclaredMethod("render", context);
        if (render.getReturnType() != Class.forName("X.1J7", false, loader))
            throw new NoSuchMethodException("Messenger AI FAB render contract changed");
        XposedBridge.hookMethod(render, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam param) {
                param.setResult(null);
            }
        });
    }
}
