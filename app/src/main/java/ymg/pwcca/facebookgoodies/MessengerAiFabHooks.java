package ymg.pwcca.facebookgoodies;

import java.lang.reflect.Method;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

final class MessengerAiFabHooks {
    private MessengerAiFabHooks() { }

    static void install(ClassLoader loader, MessengerBuild build) throws ReflectiveOperationException {
        String componentName;
        String contextName;
        String resultName;
        if (build == MessengerBuild.MESSENGER_585) {
            componentName = "X.75Q";
            contextName = "X.2ED";
            resultName = "X.1IG";
        } else if (build == MessengerBuild.MESSENGER_586) {
            componentName = "X.6wY";
            contextName = "X.2Cb";
            resultName = "X.1Gl";
        } else {
            componentName = "X.6if";
            contextName = "X.2DF";
            resultName = "X.1J7";
        }
        Class<?> component = Class.forName(componentName, false, loader);
        Class<?> context = Class.forName(contextName, false, loader);
        Method render = component.getDeclaredMethod("render", context);
        if (render.getReturnType() != Class.forName(resultName, false, loader))
            throw new NoSuchMethodException("Messenger AI FAB render contract changed");
        XposedBridge.hookMethod(render, new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam param) {
                param.setResult(null);
            }
        });
    }
}
