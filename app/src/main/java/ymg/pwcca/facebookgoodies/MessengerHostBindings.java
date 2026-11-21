package ymg.pwcca.facebookgoodies;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/** Host class and method identities that differ between Messenger builds. */
final class MessengerHostBindings {
    private MessengerHostBindings() { }

    private static final Map<String, String> RELEASE_586 = new HashMap<>();
    private static final Map<String, String> REBUILD_586 = new HashMap<>();
    private static final Map<String, String> RELEASE_588 = new HashMap<>();

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

        RELEASE_588.put("X.4lW", "X.4k4");
        RELEASE_588.put("X.AaM", "X.A0I");
        RELEASE_588.put("X.BLI", "X.BSa");
        RELEASE_588.put("X.BBe", "X.BSk");
        RELEASE_588.put("X.50R", "X.4yu");
        RELEASE_588.put("X.4LQ", "X.4RX");
        RELEASE_588.put("X.AQ8", "X.AaM");
        RELEASE_588.put("X.2ED", "X.28I");
        RELEASE_588.put("X.5OQ", "X.5Ms");
        RELEASE_588.put("X.6mm", "X.HVp");
        RELEASE_588.put("X.5Qj", "X.5P8");
        RELEASE_588.put("X.AS6", "X.Acg");
        RELEASE_588.put("X.1Tx", "X.1JV");
        RELEASE_588.put("X.DDG", "X.Fca");
        RELEASE_588.put("X.AS4", "X.Ace");
        RELEASE_588.put("X.AS3", "X.Acd");
        RELEASE_588.put("X.BST", "X.BaH");
        RELEASE_588.put("X.BIO", "X.BQ2");
        RELEASE_588.put("X.515", "X.4zX");
        RELEASE_588.put("X.2Rg", "X.2dB");
        RELEASE_588.put("X.BIP", "X.BQ3");
        RELEASE_588.put("X.BHx", "X.BPY");
        RELEASE_588.put("X.2Qp", "X.2cv");
        RELEASE_588.put("X.D9j", "X.FYI");
        RELEASE_588.put("X.262", "X.22A");
        RELEASE_588.put("X.7Fp", "X.7Jx");
        RELEASE_588.put("X.2dp", "X.2PN");
        RELEASE_588.put("X.1Pc", "X.1Hi");
    }

    static java.util.Set<String> baseNames() { return java.util.Collections.unmodifiableSet(RELEASE_586.keySet()); }

    static java.util.Set<String> rebuildBaseNames() { return java.util.Collections.unmodifiableSet(REBUILD_586.keySet()); }

    static String typeName(MessengerBuild build, String baseName) throws ClassNotFoundException {
        if (!baseName.startsWith("X.")) return baseName;
        Map<String, String> table = build == MessengerBuild.MESSENGER_586 ? RELEASE_586
                : build == MessengerBuild.MESSENGER_586_REBUILD ? REBUILD_586
                : build.is588() ? RELEASE_588 : null;
        if (table == null) return baseName;
        String mapped = table.get(baseName);
        if (mapped == null) throw new ClassNotFoundException("Unmapped Messenger type: " + baseName);
        return build.is588() ? type588(build, mapped) : mapped;
    }

    /** Resolves verified 588 base-build types on the 588.1 build. */
    static String type588(MessengerBuild build, String name) throws ClassNotFoundException {
        if (build != MessengerBuild.MESSENGER_588_1) return name;
        return switch (name) {
            case "X.4k4" -> "X.4ro";
            case "X.A0I" -> "X.Atx";
            case "X.BSa" -> "X.BoE";
            case "X.BSk" -> "X.Btr";
            case "X.4yu" -> "X.4rE";
            case "X.4RX" -> "X.4Sw";
            case "X.AaM" -> "X.ApL";
            case "X.28I" -> "X.2Gk";
            case "X.5Ms" -> "X.5Zo";
            case "X.HVp" -> "X.6Bf";
            case "X.5P8" -> "X.4qe";
            case "X.Acg" -> "X.Arn";
            case "X.1JV" -> "X.1RF";
            case "X.Fca" -> "X.Fvn";
            case "X.Ace" -> "X.Arl";
            case "X.Acd" -> "X.Ark";
            case "X.BaH" -> "X.BxD";
            case "X.BQ2" -> "X.BlC";
            case "X.4zX" -> "X.5nA";
            case "X.2dB" -> "X.2lE";
            case "X.BQ3" -> "X.BlD";
            case "X.BPY" -> "X.Bkg";
            case "X.2cv" -> "X.2ks";
            case "X.FYI" -> "X.4w8";
            case "X.22A" -> "X.2Ae";
            case "X.7Jx" -> "X.7eX";
            case "X.2PN" -> "X.2az";
            case "X.1Hi" -> "X.1OW";
            case "X.6rC" -> "X.7HG";
            case "X.1Fd" -> "X.1Jx";
            case "X.FYO" -> "X.Bwq";
            case "X.59h" -> "X.5Ub";
            case "X.Cno" -> "X.Hqr";
            case "X.3gn" -> "X.3jo";
            case "X.6S3" -> "X.BD7";
            case "X.SSw" -> "X.PiT";
            case "X.SJL" -> "X.Paj";
            case "X.AlY" -> "X.Ayy";
            case "X.Ba5" -> "X.BFb";
            case "X.HVf" -> "X.Ho4";
            case "X.2Cr" -> "X.2Pc";
            case "X.35F" -> "X.382";
            default -> throw new ClassNotFoundException("Unmapped Messenger 588.1 type: " + name);
        };
    }

    /** Thread list menu controller; 584 keeps a distinct base name. */
    static String menuController(MessengerBuild build) throws ClassNotFoundException {
        if (build == MessengerBuild.MESSENGER_584_1) return "X.4Ts";
        return typeName(build, "X.4LQ");
    }

    /** In-thread AI summary floating pill handler. */
    static String summaryPillHandler(MessengerBuild build) {
        if (build == MessengerBuild.MESSENGER_588_1) return "X.AsJ";
        if (build == MessengerBuild.MESSENGER_588) return "X.AYC";
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
        if (build == MessengerBuild.MESSENGER_588_1) return release5881Method(owner, name);
        if (build == MessengerBuild.MESSENGER_588) return release588Method(owner, name);
        if (build == MessengerBuild.MESSENGER_586) return releaseMethod(owner, name);
        if (build == MessengerBuild.MESSENGER_586_REBUILD) return rebuildMethod(owner, name);
        return name;
    }

    private static String release5881Method(String owner, String name) {
        return switch (owner + "." + name) {
            case "X.BoE.AY8" -> "Ae8";
            case "X.6Bf.Cha", "X.4w8.Cha" -> "DIX";
            case "X.6Bf.D9Z", "X.4w8.D9Z" -> "DuZ";
            case "X.Arn.AMG" -> "ARi";
            case "X.2ks.CQ1" -> "CrC";
            case "X.BlC.A0T" -> "A0H";
            case "X.BlC.A0U" -> "A0I";
            case "X.Ayy.Afu" -> "Afv";
            case "X.BFb.BTL" -> "BTQ";
            case "X.Paj.EFm" -> "EFv";
            default -> name;
        };
    }

    private static String release588Method(String owner, String name) {
        switch (owner) {
            case "X.BSa": if ("AY8".equals(name)) return "Ae7"; break;
            case "X.HVp":
            case "X.FYI":
                if ("Cha".equals(name)) return "DIM";
                if ("D9Z".equals(name)) return "DuN";
                break;
            case "X.Acg": if ("AMG".equals(name)) return "ARj"; break;
            case "X.2cv": if ("CQ1".equals(name)) return "Cr4"; break;
            case "X.BQ2":
                if ("A0T".equals(name)) return "A0Q";
                if ("A0U".equals(name)) return "A0R";
                break;
            case "com.facebook.litho.ComponentHost":
                if ("A0T".equals(name)) return "A0U";
                if ("A0U".equals(name)) return "A0V";
                break;
        }
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
