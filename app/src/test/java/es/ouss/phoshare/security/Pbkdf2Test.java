package es.ouss.phoshare.security;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Random;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class Pbkdf2Test {

    private static byte[] derive(String password, String salt, int iterations, int length) {
        return Pbkdf2.hmacSha256(password.getBytes(StandardCharsets.UTF_8), salt.getBytes(StandardCharsets.UTF_8), iterations, length);
    }

    /** RFC 7914, section 11: the PBKDF2-HMAC-SHA256 test vectors. */
    @Test
    public void matchesRfc7914() {
        assertEquals(
                "55ac046e56e3089fec1691c22544b605f94185216dde0465e68b9d57c20dacbc"
                        + "49ca9cccf179b645991664b39d77ef317c71b845b1e30bd509112041d3a19783",
                Hex.encode(derive("passwd", "salt", 1, 64)));
        assertEquals(
                "4ddcd8f60b98be21830cee5ef22701f9641a4418d04c0414aeff08876b34ab56"
                        + "a1d425a1225833549adb841b51c9b3176a272bdebba1d078478f62b397f33c8d",
                Hex.encode(derive("Password", "NaCl", 80000, 64)));
    }

    /** The JVM has the key factory Android lacks below API 26: both must agree, whatever the lengths. */
    @Test
    public void matchesTheJdk() throws Exception {
        Random random = new Random(42);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

        for (int i = 0; i < 20; i++) {
            char[] password = new char[1 + random.nextInt(40)];
            for (int c = 0; c < password.length; c++) {
                password[c] = (char) (' ' + random.nextInt(95));
            }
            byte[] salt = new byte[1 + random.nextInt(32)];
            random.nextBytes(salt);
            int iterations = 1 + random.nextInt(2000);
            int length = 1 + random.nextInt(80);

            byte[] expected = factory.generateSecret(new PBEKeySpec(password, salt, iterations, length * 8)).getEncoded();
            byte[] actual = Pbkdf2.hmacSha256(new String(password).getBytes(StandardCharsets.UTF_8), salt, iterations, length);
            assertArrayEquals(expected, actual);
        }
    }

    /** HMAC pads keys with zeros, so an empty password is the one-zero-byte key. */
    @Test
    public void acceptsAnEmptyPassword() {
        assertArrayEquals(
                Pbkdf2.hmacSha256(new byte[1], "salt".getBytes(StandardCharsets.UTF_8), 3, 32),
                Pbkdf2.hmacSha256(new byte[0], "salt".getBytes(StandardCharsets.UTF_8), 3, 32));
    }

    @Test(expected = IllegalArgumentException.class)
    public void refusesZeroIterations() {
        derive("password", "salt", 0, 32);
    }
}
