package ymg.pwcca.facebookgoodies;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/** A host Litho image/click/column, so Download participates in the action rail layout. */
final class NativeDownloadRow583 {
    private final ClassLoader loader;
    private final Method wrapper, image, column, append, build, description, click, width, height, setKey;
    private final Constructor<?> dispatchInfo, event;
    private final Field androidContext, dispatchType;
    private final Class<?> dispatcher, owner;
    private final Runnable download;
    NativeDownloadRow583(ClassLoader loader, Runnable download) throws ReflectiveOperationException {
        this.loader = loader; this.download = download;
        Class<?> context = loader.loadClass("X.1UQ");
        Class<?> component = loader.loadClass("X.1Hb");
        Class<?> builder = loader.loadClass("X.5je");
        Class<?> handler = loader.loadClass("X.6q4");
        wrapper = loader.loadClass("X.2Dm").getDeclaredMethod("A00", context);
        image = loader.loadClass("X.RzF").getDeclaredMethod("A1H", Drawable.class, ImageView.ScaleType.class, loader.loadClass("X.2Do"), boolean.class);
        column = loader.loadClass("X.32p").getDeclaredMethod("A00", context);
        append = loader.loadClass("X.2dC").getDeclaredMethod("A2d", component);
        build = builder.getDeclaredMethod("A2X");
        description = builder.getDeclaredMethod("A2G", CharSequence.class);
        click = builder.getDeclaredMethod("A2Y", handler);
        width = builder.getDeclaredMethod("A1K", int.class);
        height = builder.getDeclaredMethod("A1E", int.class);
        setKey = component.getDeclaredMethod("setKey", String.class);
        dispatcher = loader.loadClass("X.oYV"); owner = loader.loadClass("X.73M");
        dispatchInfo = loader.loadClass("X.1Hn").getConstructor(context, owner);
        event = handler.getConstructor(loader.loadClass("X.1Hn"), loader.loadClass("X.1Hl"), Object[].class, int.class);
        androidContext = context.getDeclaredField("A0C");
        dispatchType = loader.loadClass("X.1Hl").getDeclaredField("A03");
    }
    Object aboveMore(Object context, Object more) throws ReflectiveOperationException {
        Object builder = column.invoke(null, context);
        append.invoke(builder, row(context));
        append.invoke(builder, more);
        Object result = build.invoke(builder);
        setKey.invoke(result, "FacebookGoodies.DownloadAboveMore");
        return result;
    }
    private Object row(Object context) throws ReflectiveOperationException {
        Object builder = wrapper.invoke(null, context);
        Context app = (Context) androidContext.get(context);
        float density = app.getResources().getDisplayMetrics().density;
        image.invoke(null, DownloadButton.icon(Math.round(24 * density)), ImageView.ScaleType.CENTER_INSIDE, builder, true);
        width.invoke(builder, Math.round(48 * density)); height.invoke(builder, Math.round(48 * density));
        description.invoke(builder, "Download current video at highest quality");
        click.invoke(builder, handler(context));
        Object result = build.invoke(builder);
        setKey.invoke(result, "FacebookGoodies.Download");
        return result;
    }
    private Object handler(Object context) throws ReflectiveOperationException {
        Object dispatch = Proxy.newProxyInstance(loader, new Class<?>[]{dispatcher}, (proxy, method, args) -> {
            if ("Aec".equals(method.getName())) { download.run(); return null; }
            return objectMethod(proxy, method, args);
        });
        Object source = Proxy.newProxyInstance(loader, new Class<?>[]{owner}, (proxy, method, args) ->
                "BPe".equals(method.getName()) ? dispatch : objectMethod(proxy, method, args));
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
