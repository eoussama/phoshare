package es.ouss.phoshare.security;

/** Lowercase hex, because {@code java.util.Base64} needs API 26 and {@code android.util.Base64} is not in JVM tests. */
final class Hex {

    private static final char[] DIGITS = "0123456789abcdef".toCharArray();

    private Hex() {
    }

    static String encode(byte[] bytes) {
        char[] out = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            out[i * 2] = DIGITS[(bytes[i] >> 4) & 0xf];
            out[i * 2 + 1] = DIGITS[bytes[i] & 0xf];
        }
        return new String(out);
    }

    /** @throws IllegalArgumentException when the text is not an even number of hex digits */
    static byte[] decode(String hex) {
        if (hex.length() % 2 != 0) {
            throw new IllegalArgumentException("odd length");
        }

        byte[] out = new byte[hex.length() / 2];
        for (int i = 0; i < out.length; i++) {
            int high = Character.digit(hex.charAt(i * 2), 16);
            int low = Character.digit(hex.charAt(i * 2 + 1), 16);
            if (high < 0 || low < 0) {
                throw new IllegalArgumentException("not hex");
            }
            out[i] = (byte) ((high << 4) | low);
        }
        return out;
    }
}
