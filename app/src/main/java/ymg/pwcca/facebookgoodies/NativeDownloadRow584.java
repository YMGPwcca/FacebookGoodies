package ymg.pwcca.facebookgoodies;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/** A host Litho image/click/column, so Download participates in the action rail layout. */
final class NativeDownloadRow584 {
    private final ClassLoader loader;
    private final Method wrapper, image, column, append, build, buildColumn, description, click, width, height, setKey;
    private final Constructor<?> dispatchInfo, event;
    private final Field androidContext, dispatchType;
    private final Class<?> dispatcher, owner;
    private final Runnable download;
    NativeDownloadRow584(ClassLoader loader, Runnable download) throws ReflectiveOperationException {
        this.loader = loader; this.download = download;
        Class<?> context = loader.loadClass("X.3tD");
        Class<?> component = loader.loadClass("X.3SE");
        Class<?> builder = loader.loadClass("X.3T3");
        Class<?> handler = loader.loadClass("X.enB");
        wrapper = loader.loadClass("X.2BG").getDeclaredMethod("A01", context);
        image = loader.loadClass("X.LjP").getDeclaredMethod("A1I", Drawable.class, ImageView.ScaleType.class, loader.loadClass("X.2BI"));
        column = loader.loadClass("X.2UN").getDeclaredMethod("A00", context);
        append = loader.loadClass("X.3zx").getDeclaredMethod("A2E", component);
        build = builder.getDeclaredMethod("A0x");
        buildColumn = loader.loadClass("X.3zx").getDeclaredMethod("A0x");
        description = builder.getDeclaredMethod("A1y", CharSequence.class);
        click = builder.getDeclaredMethod("A1Y", handler);
        width = builder.getDeclaredMethod("A1Q", int.class);
        height = builder.getDeclaredMethod("A1K", int.class);
        setKey = component.getDeclaredMethod("setKey", String.class);
        dispatcher = loader.loadClass("X.hVN"); owner = loader.loadClass("X.hVO");
        dispatchInfo = loader.loadClass("X.1Jo").getConstructor(context, owner);
        event = handler.getConstructor(loader.loadClass("X.1Jo"), loader.loadClass("X.1Jl"), Object[].class, int.class);
        androidContext = context.getDeclaredField("A0C");
        dispatchType = loader.loadClass("X.1Jl").getDeclaredField("A03");
    }
    Object aboveMore(Object context, Object more) throws ReflectiveOperationException {
        Object builder = column.invoke(null, context);
        append.invoke(builder, row(context));
        append.invoke(builder, more);
        Object result = buildColumn.invoke(builder);
        setKey.invoke(result, "FacebookGoodies.DownloadAboveMore");
        return result;
    }
    private Object row(Object context) throws ReflectiveOperationException {
        Object builder = wrapper.invoke(null, context);
        Context app = (Context) androidContext.get(context);
        float density = app.getResources().getDisplayMetrics().density;
        image.invoke(null, DownloadButton.icon(Math.round(24 * density)), ImageView.ScaleType.CENTER_INSIDE, builder);
        width.invoke(builder, Math.round(48 * density)); height.invoke(builder, Math.round(48 * density));
        description.invoke(builder, "Download current video at highest quality");
        click.invoke(builder, handler(context));
        Object result = build.invoke(builder);
        setKey.invoke(result, "FacebookGoodies.Download");
        return result;
    }
    private Object handler(Object context) throws ReflectiveOperationException {
        Object dispatch = Proxy.newProxyInstance(loader, new Class<?>[]{dispatcher}, (proxy, method, args) -> {
            if ("AeM".equals(method.getName())) { download.run(); return null; }
            return objectMethod(proxy, method, args);
        });
        Object source = Proxy.newProxyInstance(loader, new Class<?>[]{owner}, (proxy, method, args) ->
                "BPj".equals(method.getName()) ? dispatch : objectMethod(proxy, method, args));
        Object info = dispatchInfo.newInstance(context, source);
        return event.newInstance(info, dispatchType.get(null), null, 0x4647444c);
    }
    private static Object objectMethod(Object proxy, Method method, Object[] args) {
        if ("equals".equals(method.getName())) return proxy == args[0];
        if ("hashCode".equals(method.getName())) return System.identityHashCode(proxy);
        if ("toString".equals(method.getName())) return "FacebookGoodies.DownloadHandler";
        return null;
    }
}
