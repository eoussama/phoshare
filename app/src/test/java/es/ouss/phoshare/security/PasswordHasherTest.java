package es.ouss.phoshare.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PasswordHasherTest {

    // Few iterations: these tests are about the format, not the cost.
    private final PasswordHasher hasher = new PasswordHasher(1000);

    @Test
    public void verifiesTheRightPasswordOnly() {
        String hash = hasher.hash("correct horse battery staple");

        assertTrue(hasher.verify("correct horse battery staple", hash));
        assertFalse(hasher.verify("correct horse battery stapler", hash));
        assertFalse(hasher.verify("", hash));
    }

    @Test
    public void saltsEveryHash() {
        assertNotEquals(hasher.hash("same password"), hasher.hash("same password"));
    }

    @Test
    public void writesTheSchemeIterationsSaltAndKey() {
        String[] parts = hasher.hash("password").split("\\$");

        assertEquals(4, parts.length);
        assertEquals("pbkdf2-sha256", parts[0]);
        assertEquals("1000", parts[1]);
        assertEquals(32, parts[2].length());
        assertEquals(64, parts[3].length());
    }

    /** The iteration count travels with the hash: a hasher set to another count still verifies it. */
    @Test
    public void readsTheIterationsFromTheHash() {
        String hash = new PasswordHasher(1234).hash("password");

        assertTrue(hasher.verify("password", hash));
    }

    @Test
    public void handlesUnicode() {
        String hash = hasher.hash("pässwörd 🔒");

        assertTrue(hasher.verify("pässwörd 🔒", hash));
        assertFalse(hasher.verify("passwort 🔒", hash));
    }

    @Test
    public void rejectsWhatIsNotItsHash() {
        assertFalse(hasher.verify("password", ""));
        assertFalse(hasher.verify("password", "password"));
        assertFalse(hasher.verify("password", "md5$1$00$00"));
        assertFalse(hasher.verify("password", "pbkdf2-sha256$0$00$00"));
        assertFalse(hasher.verify("password", "pbkdf2-sha256$x$00$00"));
        assertFalse(hasher.verify("password", "pbkdf2-sha256$1$zz$00"));
        assertFalse(hasher.verify("password", "pbkdf2-sha256$1$$"));
    }

    @Test
    public void rejectsATamperedHash() {
        String hash = hasher.hash("password");
        char last = hash.charAt(hash.length() - 1);
        String tampered = hash.substring(0, hash.length() - 1) + (last == '0' ? '1' : '0');

        assertFalse(hasher.verify("password", tampered));
    }
}
