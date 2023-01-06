package udp;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashTest {

    public static byte[] getSHA256(String str) {
        MessageDigest md = null;

        try {
            md = MessageDigest.getInstance("SHA-256");
            md.update(str.getBytes());

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return md.digest();
    }
}
