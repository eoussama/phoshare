package es.ouss.phoshare.data;

import android.database.sqlite.SQLiteConstraintException;
import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import java.io.IOException;
import java.util.List;

import es.ouss.phoshare.data.db.AppDatabase;
import es.ouss.phoshare.data.db.User;
import es.ouss.phoshare.security.Credentials;
import es.ouss.phoshare.security.PasswordHasher;

/**
 * Accounts: signing up, logging in, the security-question recovery, and the profile. Passwords and answers are hashed
 * in the background (on purpose slowly), and the callers have already checked the fields with {@link Credentials}.
 */
public final class UserRepository {

    private final AppDatabase db;
    private final PasswordHasher hasher;
    private final PhotoStore photos;
    private final Background background;

    public UserRepository(AppDatabase db, PasswordHasher hasher, PhotoStore photos, Background background) {
        this.db = db;
        this.hasher = hasher;
        this.photos = photos;
        this.background = background;
    }

    public LiveData<User> observe(long id) {
        return db.users().observe(id);
    }

    /** Whether the account still exists (it may have been deleted on this device since the session started). */
    public void exists(long id, Result.Callback<Boolean> callback) {
        background.run(() -> Result.ok(db.users().find(id) != null), callback);
    }

    public void signUp(String username, String password, int question, String answer, Result.Callback<User> callback) {
        background.run(() -> {
            String name = Credentials.normalizeUsername(username);
            if (db.users().findByUsername(name) != null) {
                return Result.fail(Problem.USERNAME_TAKEN);
            }

            User user = new User();
            user.username = name;
            user.passwordHash = hasher.hash(password);
            user.securityQuestion = question;
            user.securityAnswerHash = hasher.hash(Credentials.normalizeAnswer(answer));
            user.joinedAt = System.currentTimeMillis();

            try {
                user.id = db.users().insert(user);
            } catch (SQLiteConstraintException e) {
                // Someone took the name between the check and the insert.
                return Result.fail(Problem.USERNAME_TAKEN);
            }
            return Result.ok(user);
        }, callback);
    }

    public void logIn(String username, String password, Result.Callback<User> callback) {
        background.run(() -> {
            User user = db.users().findByUsername(Credentials.normalizeUsername(username));
            if (user == null) {
                return Result.fail(Problem.UNKNOWN_USER);
            }
            if (!hasher.verify(password, user.passwordHash)) {
                return Result.fail(Problem.WRONG_PASSWORD);
            }
            return Result.ok(user);
        }, callback);
    }

    /** The security question's index for an account, to start a password reset. */
    public void securityQuestion(String username, Result.Callback<Integer> callback) {
        background.run(() -> {
            User user = db.users().findByUsername(Credentials.normalizeUsername(username));
            return user == null ? Result.fail(Problem.UNKNOWN_USER) : Result.ok(user.securityQuestion);
        }, callback);
    }

    public void checkAnswer(String username, String answer, Result.Callback<Void> callback) {
        background.run(() -> {
            User user = db.users().findByUsername(Credentials.normalizeUsername(username));
            if (user == null) {
                return Result.fail(Problem.UNKNOWN_USER);
            }
            return answers(user, answer) ? Result.ok(null) : Result.fail(Problem.WRONG_ANSWER);
        }, callback);
    }

    /** Sets a new password; the answer is checked again, so the reset cannot skip the question. */
    public void resetPassword(String username, String answer, String password, Result.Callback<Void> callback) {
        background.run(() -> {
            User user = db.users().findByUsername(Credentials.normalizeUsername(username));
            if (user == null) {
                return Result.fail(Problem.UNKNOWN_USER);
            }
            if (!answers(user, answer)) {
                return Result.fail(Problem.WRONG_ANSWER);
            }
            db.users().updatePassword(user.id, hasher.hash(password));
            return Result.ok(null);
        }, callback);
    }

    public void changePassword(long userId, String current, String password, Result.Callback<Void> callback) {
        background.run(() -> {
            User user = db.users().find(userId);
            if (user == null) {
                return Result.fail(Problem.UNKNOWN_USER);
            }
            if (!hasher.verify(current, user.passwordHash)) {
                return Result.fail(Problem.WRONG_PASSWORD);
            }
            db.users().updatePassword(userId, hasher.hash(password));
            return Result.ok(null);
        }, callback);
    }

    /**
     * Saves the bio and the avatar: a new photo replaces the old one, {@code removeAvatar} drops it, and neither keeps it.
     */
    public void updateProfile(long userId, String bio, @Nullable Uri newAvatar, boolean removeAvatar, Result.Callback<Void> callback) {
        background.run(() -> {
            User user = db.users().find(userId);
            if (user == null) {
                return Result.fail(Problem.UNKNOWN_USER);
            }

            String avatar = user.avatar;
            if (newAvatar != null) {
                try {
                    avatar = photos.importAvatar(newAvatar);
                } catch (IOException | RuntimeException e) {
                    return Result.fail(Problem.PHOTO);
                }
            } else if (removeAvatar) {
                avatar = null;
            }

            db.users().updateProfile(userId, bio.trim(), avatar);
            if (user.avatar != null && !user.avatar.equals(avatar)) {
                photos.deleteAvatar(user.avatar);
            }
            return Result.ok(null);
        }, callback);
    }

    /** Deletes the account with everything it posted, after checking the password; the files go too. */
    public void deleteAccount(long userId, String password, Result.Callback<Void> callback) {
        background.run(() -> {
            User user = db.users().find(userId);
            if (user == null) {
                return Result.fail(Problem.UNKNOWN_USER);
            }
            if (!hasher.verify(password, user.passwordHash)) {
                return Result.fail(Problem.WRONG_PASSWORD);
            }

            List<String> posted = db.posts().photosBy(userId);
            db.users().delete(userId);
            for (String photo : posted) {
                photos.deletePhoto(photo);
            }
            photos.deleteAvatar(user.avatar);
            return Result.ok(null);
        }, callback);
    }

    private boolean answers(User user, String answer) {
        return hasher.verify(Credentials.normalizeAnswer(answer), user.securityAnswerHash);
    }
}
