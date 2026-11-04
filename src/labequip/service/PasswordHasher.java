package labequip.service;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.MessageDigest;
import java.util.HexFormat;

/** Хеширование паролей: PBKDF2-HMAC-SHA256 со случайной солью. Формат: соль:хеш (hex). */
public final class PasswordHasher {
    private static final int ITERATIONS = 20_000;

    private PasswordHasher() {
    }

    public static String hash(String password) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return HexFormat.of().formatHex(salt) + ":" + HexFormat.of().formatHex(derive(password, salt));
    }

    public static boolean matches(String password, String stored) {
        String[] parts = stored.split(":");
        if (parts.length != 2) {
            return false;
        }
        byte[] expected = HexFormat.of().parseHex(parts[1]);
        return MessageDigest.isEqual(expected, derive(password, HexFormat.of().parseHex(parts[0])));
    }

    private static byte[] derive(String password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, 256);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
