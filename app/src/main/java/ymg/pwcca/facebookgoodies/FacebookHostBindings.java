package ymg.pwcca.facebookgoodies;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Resolves the shared Facebook hooks against the exact 587 and 588 host builds. */
final class FacebookHostBindings {
    private final ClassLoader loader;
    private final boolean release588;

    FacebookHostBindings(ClassLoader loader, FacebookBuild build) {
        this.loader = loader;
        this.release588 = build == FacebookBuild.FACEBOOK_588;
    }

    ClassLoader loader() { return loader; }
    boolean is588() { return release588; }

    Class<?> type(String name) throws ClassNotFoundException {
        return Class.forName(typeName(name), false, loader);
    }

    String typeName(String name) {
        if (!release588) return name;
        return switch (name) {
            case "X.01Kp" -> "X.01I1";
            case "X.01Kv" -> "X.01IF";
            case "X.01Kx" -> "X.01IG";
            case "X.01LV" -> "X.01Mz";
            case "X.01Mw" -> "X.01Of";
            case "X.01PI" -> "X.01SY";
            case "X.01Xh" -> "X.01Yk";
            case "X.01r0" -> "X.01sn";
            case "X.01th" -> "X.01tb";
            case "X.027e" -> "X.02EJ";
            case "X.027i" -> "X.02EL";
            case "X.02Cy" -> "X.02Bi";
            case "X.02FH" -> "X.02E7";
            case "X.02IJ" -> "X.02GT";
            case "X.02JV" -> "X.02H2";
            case "X.02JY" -> "X.02H3";
            case "X.02M7" -> "X.02It";
            case "X.02Xb" -> "X.02Ys";
            case "X.02cj" -> "X.02dY";
            case "X.02cx" -> "X.02dc";
            case "X.02fU" -> "X.02vd";
            case "X.02kG" -> "X.02iL";
            case "X.02mE" -> "X.02gC";
            case "X.02mF" -> "X.02gF";
            case "X.02y1" -> "X.03AF";
            case "X.03H1" -> "X.03Dk";
            case "X.03HA" -> "X.03Dt";
            case "X.03HH" -> "X.03Dz";
            case "X.03Ix" -> "X.03G1";
            case "X.03RY" -> "X.03Qm";
            case "X.03Wk" -> "X.03WG";
            case "X.03Zl" -> "X.03ZL";
            case "X.03jf" -> "X.03Av";
            case "X.03jg" -> "X.03Ax";
            case "X.03kZ" -> "X.03lG";
            case "X.03ka" -> "X.03lJ";
            case "X.03uq" -> "X.02GL";
            case "X.04CT" -> "X.04I3";
            case "X.04Ip" -> "X.04Pk";
            case "X.04gF" -> "X.04gC";
            case "X.04tY" -> "X.05yF";
            case "X.04wh" -> "X.05x2";
            case "X.055L" -> "X.051I";
            case "X.055a" -> "X.051W";
            case "X.05M9" -> "X.05mn";
            case "X.05tK" -> "X.0cPe";
            case "X.05uz" -> "X.0cSD";
            case "X.05yk" -> "X.0cT8";
            case "X.05z2" -> "X.03Qo";
            case "X.065q" -> "X.06Eq";
            case "X.06I4" -> "X.06On";
            case "X.06I5" -> "X.06Oo";
            case "X.07U1" -> "X.0f5m";
            case "X.07U2" -> "X.0f5n";
            case "X.07ed" -> "X.07Z6";
            case "X.07gR" -> "X.07al";
            case "X.08NE" -> "X.07xr";
            case "X.08re" -> "X.09Lm";
            case "X.08sS" -> "X.094a";
            case "X.0A5B" -> "X.09w7";
            case "X.0Biy" -> "X.0CXR";
            case "X.0CPN" -> "X.09Hd";
            case "X.0CPQ" -> "X.09Hf";
            case "X.0D8k" -> "X.0A2o";
            case "X.0Lkl" -> "X.0RM0";
            case "X.0UaS" -> "X.0QS7";
            case "X.0Udn" -> "X.0QYw";
            case "X.0Uk0" -> "X.0Qkz";
            case "X.0Uk1" -> "X.0Ql0";
            case "X.0VDC" -> "X.0RM4";
            case "X.0Vrn" -> "X.0NF3";
            case "X.0Vro" -> "X.0NF4";
            case "X.0W4Q" -> "X.0SV3";
            case "X.0W4U" -> "X.0SVA";
            case "X.0Wcj" -> "X.0TAw";
            case "X.0dCf" -> "X.0aWZ";
            case "X.108j" -> "X.11LB";
            default -> name;
        };
    }

    String methodName(Class<?> owner, String name) {
        if (!release588) return name;
        return switch (owner.getName() + "." + name) {
            case "X.02EL.Bkr", "X.0NF3.Bkr", "X.0NF4.Bkr" -> "BrX";
            case "X.02EJ.AAb" -> "ABx";
            case "X.03ZL.CVJ" -> "Cga";
            case "X.03Dz.CPz" -> "Can";
            case "X.03Ft.GF5" -> "Gsq";
            case "X.02dY.A19", "X.02dc.A19", "X.02us.A19", "X.0CXR.A19", "X.0QS7.A19",
                    "X.11LB.A19", "X.0RM4.A19", "X.0RM0.A19" -> "A1B";
            case "X.05x2.A0V", "X.06On.A0V", "X.06Oo.A0V" -> "A0W";
            case "X.03Dt.A0T", "X.06On.A0T", "X.06Oo.A0T" -> "A0U";
            case "X.051I.Ekk" -> "FD4";
            case "X.07Z6.AOs" -> "AQo";
            case "X.07al.AP7" -> "AR2";
            case "X.0cPe.DM3" -> "DeH";
            case "X.0cSD.CUj" -> "Cfz";
            case "X.051W.Cuo" -> "D9I";
            case "X.05mn.GqR" -> "HZ4";
            case "X.05mn.JVF" -> "KOL";
            case "X.02H2.A00" -> "A01";
            case "X.02H3.A28", "X.02gF.A28" -> "A1q";
            case "X.03Dk.A0D", "X.02gF.A0D" -> "A1o";
            case "X.03Dk.A1p" -> "A1Y";
            case "X.03Dk.A19" -> "A0x";
            case "X.03Dk.A0p" -> "A0V";
            case "X.03Dk.A0j" -> "A0c";
            case "X.0f5m.BWY" -> "Bcw";
            case "X.0f5n.DEU" -> "DVn";
            default -> name;
        };
    }

    Method method(Class<?> owner, String name, Class<?>... parameters) throws NoSuchMethodException {
        return owner.getDeclaredMethod(methodName(owner, name), parameters);
    }

    Field field(Class<?> owner, String name) throws NoSuchFieldException {
        if (release588) {
            name = switch (owner.getName() + "." + name) {
                case "X.03G1.A0A" -> "A0B";
                case "X.03lJ.A01" -> "A02";
                case "X.03lJ.A03" -> "A04";
                default -> name;
            };
        }
        return owner.getDeclaredField(name);
    }
}
