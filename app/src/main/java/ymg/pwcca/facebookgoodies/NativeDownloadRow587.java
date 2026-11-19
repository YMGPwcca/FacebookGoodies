package ymg.pwcca.facebookgoodies;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/** A host Litho image, click handler, and column for the video action rail. */
final class NativeDownloadRow587 {
    private final FacebookHostBindings bindings;
    private final Method wrapper, appendImage, column, append, build, buildColumn, description, click, width, height, setKey;
    private final Constructor<?> dispatchInfo, event, image;
    private final Field androidContext, dispatchType, imageAlpha;
    private final Class<?> dispatcher, owner;
    private final Runnable download;

    NativeDownloadRow587(FacebookHostBindings loader, Runnable download) throws ReflectiveOperationException {
        this.bindings = loader;
        this.download = download;
        Class<?> context = loader.type("X.01Mw");
        Class<?> component = loader.type("X.01Kp");
        Class<?> builder = loader.type("X.03H1");
        Class<?> handler = loader.type("X.05yk");
        Class<?> componentBuilder = loader.type("X.02JY");
        Class<?> columnBuilder = loader.type("X.02mF");
        wrapper = loader.method(loader.type("X.02JV"), "A00", context);
        Class<?> alpha = loader.type("X.01PI");
        image = loader.type("X.02M7").getConstructor(Drawable.class, ImageView.ScaleType.class, alpha, boolean.class);
        imageAlpha = loader.field(alpha, "A02");
        appendImage = loader.method(componentBuilder, "A28", component);
        column = loader.method(loader.type("X.02mE"), "A00", context);
        append = loader.method(columnBuilder, "A28", component);
        build = loader.method(builder, "A0D");
        buildColumn = loader.method(columnBuilder, "A0D");
        description = loader.method(builder, "A1p", CharSequence.class);
        click = loader.method(builder, "A19", handler);
        width = loader.method(builder, "A0p", int.class);
        height = loader.method(builder, "A0j", int.class);
        setKey = loader.method(component, "setKey", String.class);
        dispatcher = loader.type("X.07U1");
        owner = loader.type("X.07U2");
        dispatchInfo = loader.type("X.01Kx").getConstructor(context, owner);
        event = handler.getConstructor(loader.type("X.01Kx"), loader.type("X.01Kv"), Object[].class, int.class);
        androidContext = loader.field(context, "A0C");
        dispatchType = loader.field(loader.type("X.01Kv"), "A03");
        image.setAccessible(true);
        imageAlpha.setAccessible(true);
        androidContext.setAccessible(true);
        dispatchType.setAccessible(true);
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
        appendImage.invoke(builder, image.newInstance(DownloadButton.icon(Math.round(24 * density)),
                ImageView.ScaleType.CENTER_INSIDE, imageAlpha.get(null), true));
        width.invoke(builder, Math.round(48 * density));
        height.invoke(builder, Math.round(48 * density));
        description.invoke(builder, "Download current video at highest quality");
        click.invoke(builder, handler(context));
        Object result = build.invoke(builder);
        setKey.invoke(result, "FacebookGoodies.Download");
        return result;
    }

    private Object handler(Object context) throws ReflectiveOperationException {
        Object dispatch = Proxy.newProxyInstance(bindings.loader(), new Class<?>[]{dispatcher}, (proxy, method, args) -> {
            if (bindings.methodName(dispatcher, "BWY").equals(method.getName())) { download.run(); return null; }
            return objectMethod(proxy, method, args);
        });
        Object source = Proxy.newProxyInstance(bindings.loader(), new Class<?>[]{owner}, (proxy, method, args) ->
                bindings.methodName(owner, "DEU").equals(method.getName()) ? dispatch : objectMethod(proxy, method, args));
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
