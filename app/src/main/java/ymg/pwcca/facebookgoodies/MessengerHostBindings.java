package ymg.pwcca.facebookgoodies;

import java.lang.reflect.Method;

/** Host class and method identities that differ between Messenger builds. */
final class MessengerHostBindings {
    private MessengerHostBindings() { }

    static String typeName(MessengerBuild build, String name) throws ClassNotFoundException {
        if (build != MessengerBuild.MESSENGER_586 || !name.startsWith("X.")) return name;
        switch (name) {
            case "X.AQ8": return "X.AM8";
            case "X.2ED": return "X.2Cb";
            case "X.5OQ": return "X.5P8";
            case "X.6mm": return "X.6ma";
            case "X.5Qj": return "X.5RP";
            case "X.AS6": return "X.AOb";
            case "X.1Tx": return "X.1Xx";
            case "X.DDG": return "X.KQQ";
            case "X.AS4": return "X.AOZ";
            case "X.AS3": return "X.AOY";
            case "X.BST": return "X.BW0";
            case "X.BIO": return "X.BQK";
            case "X.515": return "X.51k";
            case "X.2Rg": return "X.2fo";
            case "X.BIP": return "X.BQL";
            case "X.BHx": return "X.BPs";
            case "X.2Qp": return "X.2fY";
            case "X.D9j": return "X.5uN";
            case "X.262": return "X.26p";
            case "X.7Fp": return "X.7RJ";
            case "X.2dp": return "X.2Rr";
            case "X.1Pc": return "X.1NO";
            default: throw new ClassNotFoundException("Unmapped Messenger 586 type: " + name);
        }
    }

    static Method method(MessengerBuild build, Class<?> owner, String name, Class<?>... parameters)
            throws NoSuchMethodException {
        return owner.getDeclaredMethod(methodName(build, owner.getName(), name), parameters);
    }

    static String methodName(MessengerBuild build, String owner, String name) {
        if (build != MessengerBuild.MESSENGER_586) return name;
        switch (owner) {
            case "X.6ma":
            case "X.5uN":
                if ("Cha".equals(name)) return "Cml";
                if ("C2G".equals(name)) return "C5z";
                if ("D9Z".equals(name)) return "DFU";
                break;
            case "X.AOb": if ("AMG".equals(name)) return "AMs"; break;
            case "X.BQK":
                if ("A0U".equals(name)) return "A0V";
                if ("A0T".equals(name)) return "A0U";
                break;
            case "com.facebook.litho.ComponentHost":
                if ("A0T".equals(name)) return "A0S";
                if ("A0U".equals(name)) return "A0T";
                break;
        }
        return name;
    }
}
