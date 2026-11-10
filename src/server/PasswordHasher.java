package server;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Băm mật khẩu bằng PBKDF2-HMAC-SHA256 kèm salt ngẫu nhiên cho từng người dùng.
 * Không bao giờ lưu mật khẩu thô. Chuỗi lưu trong CSDL có dạng:
 *     pbkdf2$<số vòng lặp>$<salt base64>$<hash base64>
 * Lưu số vòng lặp trong chuỗi để sau này tăng độ khó mà vẫn đăng nhập được với tài khoản cũ.
 */
public final class PasswordHasher {
    private PasswordHasher() {}

    private static final int ITERATIONS = 120_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] key = derive(password, salt, ITERATIONS);
        return "pbkdf2$" + ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(key);
    }

    public static boolean verify(String password, String stored) {
        try {
            String[] p = stored.split("\\$");
            if (p.length != 4 || !p[0].equals("pbkdf2")) return false;
            int iterations = Integer.parseInt(p[1]);
            byte[] salt = Base64.getDecoder().decode(p[2]);
            byte[] expected = Base64.getDecoder().decode(p[3]);
            byte[] actual = derive(password, salt, iterations);
            return MessageDigest.isEqual(expected, actual); // so sánh thời gian không đổi, chống tấn công đo thời gian
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] derive(String password, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Không có thuật toán PBKDF2WithHmacSHA256", e);
        }
    }
}
