package es.ouss.phoshare.data;

import androidx.annotation.StringRes;

import es.ouss.phoshare.R;

/** Why a repository call failed, with the message the user sees. */
public enum Problem {
    UNKNOWN_USER(R.string.error_unknown_user),
    WRONG_PASSWORD(R.string.error_wrong_password),
    USERNAME_TAKEN(R.string.error_username_taken),
    WRONG_ANSWER(R.string.error_wrong_answer),
    PHOTO(R.string.error_photo),
    UNEXPECTED(R.string.error_unexpected);

    @StringRes
    public final int message;

    Problem(@StringRes int message) {
        this.message = message;
    }
}
