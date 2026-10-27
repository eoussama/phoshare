package es.ouss.phoshare.security;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import es.ouss.phoshare.R;

public class CredentialsTest {

    @Test
    public void checksUsernames() {
        assertEquals(Credentials.VALID, Credentials.checkUsername("eoussama"));
        assertEquals(Credentials.VALID, Credentials.checkUsername("me.myself_and.i"));
        assertEquals(Credentials.VALID, Credentials.checkUsername("  abc  "));
        assertEquals(R.string.error_username_empty, Credentials.checkUsername("   "));
        assertEquals(R.string.error_username_length, Credentials.checkUsername("ab"));
        assertEquals(R.string.error_username_length, Credentials.checkUsername("a2345678901234567890x"));
        assertEquals(R.string.error_username_characters, Credentials.checkUsername("no spaces"));
        assertEquals(R.string.error_username_characters, Credentials.checkUsername("émile"));
        assertEquals(R.string.error_username_characters, Credentials.checkUsername("bob'--"));
    }

    @Test
    public void trimsUsernames() {
        assertEquals("abc", Credentials.normalizeUsername("  abc\n"));
    }

    @Test
    public void checksPasswords() {
        assertEquals(Credentials.VALID, Credentials.checkPassword("12345678"));
        assertEquals(Credentials.VALID, Credentials.checkPassword("        "));
        assertEquals(R.string.error_password_empty, Credentials.checkPassword(""));
        assertEquals(R.string.error_password_short, Credentials.checkPassword("1234567"));
        assertEquals(R.string.error_password_long, Credentials.checkPassword("x".repeat(129)));
    }

    @Test
    public void checksTheConfirmation() {
        assertEquals(Credentials.VALID, Credentials.checkConfirmation("password", "password"));
        assertEquals(R.string.error_passwords_differ, Credentials.checkConfirmation("password", "Password"));
    }

    @Test
    public void checksAnswers() {
        assertEquals(Credentials.VALID, Credentials.checkAnswer("Blue"));
        assertEquals(R.string.error_answer_empty, Credentials.checkAnswer("  \t "));
        assertEquals(R.string.error_answer_long, Credentials.checkAnswer("x".repeat(61)));
    }

    @Test
    public void foldsAnswers() {
        assertEquals("light blue", Credentials.normalizeAnswer("  Light   BLUE "));
        assertEquals(Credentials.normalizeAnswer("light blue"), Credentials.normalizeAnswer("Ｌｉｇｈｔ\tBlue"));
        assertEquals(Credentials.normalizeAnswer("café"), Credentials.normalizeAnswer("café"));
    }
}
