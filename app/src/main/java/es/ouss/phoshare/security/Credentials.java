package es.ouss.phoshare.security;

import androidx.annotation.StringRes;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

import es.ouss.phoshare.R;

/**
 * The rules accounts follow. Each check returns the message to show under the field, or {@link #VALID}.
 */
public final class Credentials {

    public static final int VALID = 0;

    public static final int USERNAME_MIN = 3;
    public static final int USERNAME_MAX = 20;
    public static final int PASSWORD_MIN = 8;
    public static final int PASSWORD_MAX = 128;
    public static final int ANSWER_MAX = 60;

    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9._]+");
    private static final Pattern SPACES = Pattern.compile("\\s+");

    private Credentials() {
    }

    /** Usernames are stored as typed, without the spaces around them; they compare without case. */
    public static String normalizeUsername(String username) {
        return username.trim();
    }

    @StringRes
    public static int checkUsername(String username) {
        String name = normalizeUsername(username);
        if (name.isEmpty()) {
            return R.string.error_username_empty;
        }
        if (name.length() < USERNAME_MIN || name.length() > USERNAME_MAX) {
            return R.string.error_username_length;
        }
        if (!USERNAME.matcher(name).matches()) {
            return R.string.error_username_characters;
        }
        return VALID;
    }

    /** Passwords are taken exactly as typed, spaces included. */
    @StringRes
    public static int checkPassword(String password) {
        if (password.isEmpty()) {
            return R.string.error_password_empty;
        }
        if (password.length() < PASSWORD_MIN) {
            return R.string.error_password_short;
        }
        if (password.length() > PASSWORD_MAX) {
            return R.string.error_password_long;
        }
        return VALID;
    }

    @StringRes
    public static int checkConfirmation(String password, String confirmation) {
        return password.equals(confirmation) ? VALID : R.string.error_passwords_differ;
    }

    @StringRes
    public static int checkAnswer(String answer) {
        String normalized = normalizeAnswer(answer);
        if (normalized.isEmpty()) {
            return R.string.error_answer_empty;
        }
        if (normalized.length() > ANSWER_MAX) {
            return R.string.error_answer_long;
        }
        return VALID;
    }

    /**
     * Security answers are remembered loosely: "  Light  Blue" and "light blue" are the same answer. Unicode
     * compatibility forms fold too, so a full-width letter matches its plain one.
     */
    public static String normalizeAnswer(String answer) {
        String folded = Normalizer.normalize(answer, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT).trim();
        return SPACES.matcher(folded).replaceAll(" ");
    }
}
