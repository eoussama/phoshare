package es.ouss.phoshare.data.db;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** An account on this device. Usernames are unique regardless of case. */
@Entity(tableName = "users", indices = {@Index(value = "username", unique = true)})
public class User {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    @ColumnInfo(collate = ColumnInfo.NOCASE)
    public String username = "";

    /** {@link es.ouss.phoshare.security.PasswordHasher}'s encoding. */
    @NonNull
    public String passwordHash = "";

    /** The question's position in the {@code security_questions} array, so it follows the app's language. */
    public int securityQuestion;

    /** The normalized answer, hashed like the password. */
    @NonNull
    public String securityAnswerHash = "";

    /** The avatar's file name in the avatars folder, if any. */
    @Nullable
    public String avatar;

    @NonNull
    public String bio = "";

    /** When the account was created, in epoch milliseconds. */
    public long joinedAt;
}
