package es.ouss.phoshare.data;

import android.content.Context;
import android.content.SharedPreferences;

/** Who is logged in on this device, remembered until they log out. */
public final class Session {

    public static final long NOBODY = -1;

    private static final String PREFERENCES = "session";
    private static final String USER_ID = "user_id";

    private final SharedPreferences preferences;

    public Session(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    public long userId() {
        return preferences.getLong(USER_ID, NOBODY);
    }

    public boolean isLoggedIn() {
        return userId() != NOBODY;
    }

    public void logIn(long userId) {
        preferences.edit().putLong(USER_ID, userId).apply();
    }

    public void logOut() {
        preferences.edit().remove(USER_ID).apply();
    }
}
