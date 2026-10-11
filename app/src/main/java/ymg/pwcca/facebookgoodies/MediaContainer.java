package ymg.pwcca.facebookgoodies;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/** Return a numeric container type, without exposing or retaining media bytes. */
final class MediaContainer {
    static int kind(File file) throws IOException {
        byte[] prefix = new byte[32]; int count;
        try(FileInputStream input = new FileInputStream(file)) { count = input.read(prefix); }
        if(count < 8) return 0;
        int kind=binaryKind(prefix);
        return kind==0?textKind(prefix,count):kind;
    }
    private static int binaryKind(byte[] prefix) {
        String type = new String(prefix,4,4,java.nio.charset.StandardCharsets.US_ASCII);
        if(java.util.Arrays.asList("ftyp","styp","moov","moof","free").contains(type))return 1;
        if((prefix[0]&255)==0x1a && (prefix[1]&255)==0x45 && (prefix[2]&255)==0xdf && (prefix[3]&255)==0xa3) return 2;
        return 0;
    }
    private static int textKind(byte[] prefix,int count) {
        for(int i=0;i<count;i++) {
            if(Character.isWhitespace(prefix[i])) continue;
            if(prefix[i]=='<') return 3;
            if(prefix[i]=='{' || prefix[i]=='[') return 4;
            return 0;
        }
        return 0;
    }
}
