package ymg.pwcca.facebookgoodies;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/** A host Litho image, click handler, and column for the video action rail. */
final class NativeDownloadRow585 {
    private final ClassLoader loader;
    private final Method wrapper, image, column, append, build, buildColumn, description, click, width, height, setKey;
    private final Constructor<?> dispatchInfo, event;
    private final Field androidContext, dispatchType;
    private final Class<?> dispatcher, owner;
    private final Runnable download;

    NativeDownloadRow585(ClassLoader loader, Runnable download) throws ReflectiveOperationException {
        this.loader = loader;
        this.download = download;
        Class<?> context = loader.loadClass("X.01PR");
        Class<?> component = loader.loadClass("X.01JC");
        Class<?> builder = loader.loadClass("X.03G2");
        Class<?> handler = loader.loadClass("X.05v0");
        Class<?> componentBuilder = loader.loadClass("X.02Ie");
        Class<?> columnBuilder = loader.loadClass("X.02bu");
        wrapper = loader.loadClass("X.02Id").getDeclaredMethod("A01", context);
        image = loader.loadClass("X.0IoH").getDeclaredMethod("A1A", Drawable.class, ImageView.ScaleType.class,
                componentBuilder, boolean.class);
        column = loader.loadClass("X.02bt").getDeclaredMethod("A00", context);
        append = columnBuilder.getDeclaredMethod("A27", component);
        build = builder.getDeclaredMethod("A0B");
        buildColumn = columnBuilder.getDeclaredMethod("A0B");
        description = builder.getDeclaredMethod("A1n", CharSequence.class);
        click = builder.getDeclaredMethod("A17", handler);
        width = builder.getDeclaredMethod("A0n", int.class);
        height = builder.getDeclaredMethod("A0h", int.class);
        setKey = component.getDeclaredMethod("setKey", String.class);
        dispatcher = loader.loadClass("X.07V7");
        owner = loader.loadClass("X.07V8");
        dispatchInfo = loader.loadClass("X.01JK").getConstructor(context, owner);
        event = handler.getConstructor(loader.loadClass("X.01JK"), loader.loadClass("X.01JI"), Object[].class, int.class);
        androidContext = context.getDeclaredField("A0C");
        dispatchType = loader.loadClass("X.01JI").getDeclaredField("A03");
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
        image.invoke(null, DownloadButton.icon(Math.round(24 * density)), ImageView.ScaleType.CENTER_INSIDE, builder, true);
        width.invoke(builder, Math.round(48 * density));
        height.invoke(builder, Math.round(48 * density));
        description.invoke(builder, "Download current video at highest quality");
        click.invoke(builder, handler(context));
        Object result = build.invoke(builder);
        setKey.invoke(result, "FacebookGoodies.Download");
        return result;
    }

    private Object handler(Object context) throws ReflectiveOperationException {
        Object dispatch = Proxy.newProxyInstance(loader, new Class<?>[]{dispatcher}, (proxy, method, args) -> {
            if ("BQe".equals(method.getName())) { download.run(); return null; }
            return objectMethod(proxy, method, args);
        });
        Object source = Proxy.newProxyInstance(loader, new Class<?>[]{owner}, (proxy, method, args) ->
                "D1L".equals(method.getName()) ? dispatch : objectMethod(proxy, method, args));
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
