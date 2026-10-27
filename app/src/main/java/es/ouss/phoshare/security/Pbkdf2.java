package es.ouss.phoshare.security;

import java.security.GeneralSecurityException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * PBKDF2 with HMAC-SHA256 (RFC 8018), built on {@link Mac} because Android's {@code PBKDF2WithHmacSHA256} key factory
 * only exists from API 26 and the app runs from API 24.
 */
public final class Pbkdf2 {

    private Pbkdf2() {
    }

    /**
     * Derives a key.
     *
     * @param password   the secret, as bytes
     * @param salt       the salt
     * @param iterations how many times HMAC is applied per block (at least 1)
     * @param length     the key's length in bytes
     * @return the derived key
     */
    public static byte[] hmacSha256(byte[] password, byte[] salt, int iterations, int length) {
        if (iterations < 1 || length < 1) {
            throw new IllegalArgumentException("iterations and length must be positive");
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            // HMAC pads short keys with zeros, so an empty key is the same as one zero byte, which SecretKeySpec accepts.
            mac.init(new SecretKeySpec(password.length == 0 ? new byte[1] : password, "HmacSHA256"));

            int size = mac.getMacLength();
            int blocks = (length + size - 1) / size;
            byte[] key = new byte[length];
            byte[] u = new byte[size];
            byte[] t = new byte[size];

            for (int block = 1; block <= blocks; block++) {
                mac.update(salt);
                mac.update(new byte[]{(byte) (block >>> 24), (byte) (block >>> 16), (byte) (block >>> 8), (byte) block});
                mac.doFinal(u, 0);
                System.arraycopy(u, 0, t, 0, size);

                for (int i = 1; i < iterations; i++) {
                    mac.update(u);
                    mac.doFinal(u, 0);
                    for (int j = 0; j < size; j++) {
                        t[j] ^= u[j];
                    }
                }

                int offset = (block - 1) * size;
                System.arraycopy(t, 0, key, offset, Math.min(size, length - offset));
            }

            return key;
        } catch (GeneralSecurityException e) {
            // HmacSHA256 is a required algorithm on every Java and Android runtime.
            throw new IllegalStateException(e);
        }
    }
}
