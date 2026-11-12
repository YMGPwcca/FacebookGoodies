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
        if (build == MessengerBuild.MESSENGER_585 || build.is586()) {
            componentName = MessengerHostBindings.typeName(build, "X.75Q");
            contextName = MessengerHostBindings.typeName(build, "X.2ED");
            resultName = MessengerHostBindings.typeName(build, "X.1IG");
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
