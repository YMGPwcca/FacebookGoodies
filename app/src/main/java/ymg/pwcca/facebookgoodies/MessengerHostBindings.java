package ymg.pwcca.facebookgoodies;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/** Host class and method identities that differ between Messenger builds. */
final class MessengerHostBindings {
    private MessengerHostBindings() { }

    private static final Map<String, String> RELEASE_586 = new HashMap<>();
    private static final Map<String, String> REBUILD_586 = new HashMap<>();

    static {
        // 585 base name -> 586 (347215273)
        RELEASE_586.put("X.AQ8", "X.AM8");
        RELEASE_586.put("X.2ED", "X.2Cb");
        RELEASE_586.put("X.5OQ", "X.5P8");
        RELEASE_586.put("X.6mm", "X.6ma");
        RELEASE_586.put("X.5Qj", "X.5RP");
        RELEASE_586.put("X.AS6", "X.AOb");
        RELEASE_586.put("X.1Tx", "X.1Xx");
        RELEASE_586.put("X.DDG", "X.KQQ");
        RELEASE_586.put("X.AS4", "X.AOZ");
        RELEASE_586.put("X.AS3", "X.AOY");
        RELEASE_586.put("X.BST", "X.BW0");
        RELEASE_586.put("X.BIO", "X.BQK");
        RELEASE_586.put("X.515", "X.51k");
        RELEASE_586.put("X.2Rg", "X.2fo");
        RELEASE_586.put("X.BIP", "X.BQL");
        RELEASE_586.put("X.BHx", "X.BPs");
        RELEASE_586.put("X.2Qp", "X.2fY");
        RELEASE_586.put("X.D9j", "X.5uN");
        RELEASE_586.put("X.262", "X.26p");
        RELEASE_586.put("X.7Fp", "X.7RJ");
        RELEASE_586.put("X.2dp", "X.2Rr");
        RELEASE_586.put("X.1Pc", "X.1NO");
        RELEASE_586.put("X.4lW", "X.4mE");
        RELEASE_586.put("X.AaM", "X.AeP");
        RELEASE_586.put("X.BLI", "X.BGc");
        RELEASE_586.put("X.BBe", "X.BGp");
        RELEASE_586.put("X.50R", "X.517");
        RELEASE_586.put("X.Gmd", "X.F4U");
        RELEASE_586.put("X.Ak3", "X.Aid");
        RELEASE_586.put("X.3hT", "X.3hq");
        RELEASE_586.put("X.K4W", "X.PQS");
        RELEASE_586.put("X.VIG", "X.PJA");
        RELEASE_586.put("X.75Q", "X.6wY");
        RELEASE_586.put("X.1IG", "X.1Gl");
        RELEASE_586.put("X.4LQ", "X.4gP");

        // 585 base name -> 586 rebuild (347215343)
        REBUILD_586.put("X.AQ8", "X.APh");
        REBUILD_586.put("X.2ED", "X.2Ca");
        REBUILD_586.put("X.5OQ", "X.5QT");
        REBUILD_586.put("X.6mm", "X.6pX");
        REBUILD_586.put("X.5Qj", "X.5Sk");
        REBUILD_586.put("X.AS6", "X.ASA");
        REBUILD_586.put("X.1Tx", "X.1Xw");
        REBUILD_586.put("X.DDG", "X.DKQ");
        REBUILD_586.put("X.AS4", "X.AS8");
        REBUILD_586.put("X.AS3", "X.AS7");
        REBUILD_586.put("X.BST", "X.BZX");
        REBUILD_586.put("X.BIO", "X.BTr");
        REBUILD_586.put("X.515", "X.535");
        REBUILD_586.put("X.2Rg", "X.OvW");
        REBUILD_586.put("X.BIP", "X.BTs");
        REBUILD_586.put("X.BHx", "X.BTP");
        REBUILD_586.put("X.2Qp", "X.2fX");
        REBUILD_586.put("X.D9j", "X.DGp");
        REBUILD_586.put("X.262", "X.26o");
        REBUILD_586.put("X.7Fp", "X.7U1");
        REBUILD_586.put("X.2dp", "X.2Rq");
        REBUILD_586.put("X.1Pc", "X.1NN");
        REBUILD_586.put("X.4lW", "X.4nZ");
        REBUILD_586.put("X.AaM", "X.Ahz");
        REBUILD_586.put("X.BLI", "X.BK9");
        REBUILD_586.put("X.BBe", "X.BKM");
        REBUILD_586.put("X.50R", "X.52S");
        REBUILD_586.put("X.Gmd", "X.HgE");
        REBUILD_586.put("X.Ak3", "X.AmA");
        REBUILD_586.put("X.3hT", "X.3if");
        REBUILD_586.put("X.K4W", "X.6X4");
        REBUILD_586.put("X.VIG", "X.PVi");
        REBUILD_586.put("X.75Q", "X.6zF");
        REBUILD_586.put("X.1IG", "X.1Gk");
        REBUILD_586.put("X.4LQ", "X.4hk");
    }

    static java.util.Set<String> baseNames() { return java.util.Collections.unmodifiableSet(RELEASE_586.keySet()); }

    static java.util.Set<String> rebuildBaseNames() { return java.util.Collections.unmodifiableSet(REBUILD_586.keySet()); }

    static String typeName(MessengerBuild build, String baseName) throws ClassNotFoundException {
        if (!baseName.startsWith("X.")) return baseName;
        Map<String, String> table = build == MessengerBuild.MESSENGER_586 ? RELEASE_586
                : build == MessengerBuild.MESSENGER_586_REBUILD ? REBUILD_586 : null;
        if (table == null) return baseName;
        String mapped = table.get(baseName);
        if (mapped == null) throw new ClassNotFoundException("Unmapped Messenger type: " + baseName);
        return mapped;
    }

    /** Thread list menu controller; 584 keeps a distinct base name. */
    static String menuController(MessengerBuild build) throws ClassNotFoundException {
        if (build == MessengerBuild.MESSENGER_584_1) return "X.4Ts";
        return typeName(build, "X.4LQ");
    }

    /** In-thread AI summary floating pill handler. */
    static String summaryPillHandler(MessengerBuild build) {
        if (build == MessengerBuild.MESSENGER_584_1) return "X.9XY";
        if (build == MessengerBuild.MESSENGER_585) return "X.Ado";
        if (build == MessengerBuild.MESSENGER_586) return "X.AIu";
        return "X.AMT";
    }

    /** Top jump pill controller runnable that builds the unread summary label. */
    static String summaryJumpPill(MessengerBuild build) {
        if (build == MessengerBuild.MESSENGER_584_1) return "X.9T0";
        if (build == MessengerBuild.MESSENGER_585) return "X.AUP";
        if (build == MessengerBuild.MESSENGER_586) return "X.AIt";
        return "X.AMS";
    }

    static Method method(MessengerBuild build, Class<?> owner, String name, Class<?>... parameters)
            throws NoSuchMethodException {
        return owner.getDeclaredMethod(methodName(build, owner.getName(), name), parameters);
    }

    static String methodName(MessengerBuild build, String owner, String name) {
        if (build == MessengerBuild.MESSENGER_586) return releaseMethod(owner, name);
        if (build == MessengerBuild.MESSENGER_586_REBUILD) return rebuildMethod(owner, name);
        return name;
    }

    private static String releaseMethod(String owner, String name) {
        switch (owner) {
            case "X.6ma":
            case "X.5uN":
                if ("Cha".equals(name)) return "Cml";
                if ("C2G".equals(name)) return "C5z";
                if ("D9Z".equals(name)) return "DFU";
                break;
            case "X.AOb": if ("AMG".equals(name)) return "AMs"; break;
            case "X.BGc": if ("AY8".equals(name)) return "AYs"; break;
            case "X.PJA": if ("DTN".equals(name)) return "DZb"; break;
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

    private static String rebuildMethod(String owner, String name) {
        switch (owner) {
            case "X.6pX":
            case "X.DGp":
                if ("Cha".equals(name)) return "Cmb";
                if ("C2G".equals(name)) return "C5u";
                if ("D9Z".equals(name)) return "DFH";
                break;
            case "X.ASA": if ("AMG".equals(name)) return "AMv"; break;
            case "X.BK9": if ("AY8".equals(name)) return "AYt"; break;
            case "X.2fX": if ("CQ1".equals(name)) return "CPv"; break;
            case "X.PVi": if ("DTN".equals(name)) return "DZI"; break;
            case "X.BTr":
                if ("A0U".equals(name)) return "A0V";
                if ("A0T".equals(name)) return "A0U";
                break;
        }
        return name;
    }
}
