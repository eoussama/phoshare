package es.ouss.phoshare.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * Hashes passwords and security answers for storage, as {@code pbkdf2-sha256$<iterations>$<salt>$<hash>} with the salt
 * and hash in hex. The iteration count travels with each hash, so it can be raised later without breaking old accounts.
 * Hashing is slow on purpose: call it off the main thread.
 */
public final class PasswordHasher {

    /** OWASP's recommendation for PBKDF2-HMAC-SHA256. */
    public static final int DEFAULT_ITERATIONS = 310_000;

    private static final String SCHEME = "pbkdf2-sha256";
    private static final int SALT_BYTES = 16;
    private static final int KEY_BYTES = 32;

    private final int iterations;
    private final SecureRandom random = new SecureRandom();

    public PasswordHasher() {
        this(DEFAULT_ITERATIONS);
    }

    public PasswordHasher(int iterations) {
        if (iterations < 1) {
            throw new IllegalArgumentException("iterations must be positive");
        }
        this.iterations = iterations;
    }

    /** Hashes a secret with a new random salt. */
    public String hash(String secret) {
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        byte[] key = Pbkdf2.hmacSha256(secret.getBytes(StandardCharsets.UTF_8), salt, iterations, KEY_BYTES);

        return SCHEME + "$" + iterations + "$" + Hex.encode(salt) + "$" + Hex.encode(key);
    }

    /** Whether the secret is the one that was hashed; false for anything that is not a hash this class made. */
    public boolean verify(String secret, String encoded) {
        String[] parts = encoded.split("\\$", -1);
        if (parts.length != 4 || !SCHEME.equals(parts[0])) {
            return false;
        }

        try {
            int rounds = Integer.parseInt(parts[1]);
            byte[] salt = Hex.decode(parts[2]);
            byte[] expected = Hex.decode(parts[3]);
            if (rounds < 1 || salt.length == 0 || expected.length == 0) {
                return false;
            }

            byte[] actual = Pbkdf2.hmacSha256(secret.getBytes(StandardCharsets.UTF_8), salt, rounds, expected.length);

            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
