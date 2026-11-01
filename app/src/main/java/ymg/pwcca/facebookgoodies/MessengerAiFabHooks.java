package ymg.pwcca.facebookgoodies;

import java.lang.reflect.Method;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

final class MessengerAiFabHooks {
    private MessengerAiFabHooks() { }

    static void install(ClassLoader loader, MessengerBuild build) throws ReflectiveOperationException {
        boolean messenger585 = build == MessengerBuild.MESSENGER_585;
        Class<?> component = Class.forName(messenger585 ? "X.75Q" : "X.6if", false, loader);
        Class<?> context = Class.forName(messenger585 ? "X.2ED" : "X.2DF", false, loader);
        Method render = component.getDeclaredMethod("render", context);
        String resultName = messenger585 ? "X.1IG" : "X.1J7";
        if (render.getReturnType() != Class.forName(resultName, false, loader))
            throw new NoSuchMethodException("Messenger AI FAB render contract changed");
        XposedBridge.hookMethod(render, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam param) {
                param.setResult(null);
            }
        });
    }
}
