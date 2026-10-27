package es.ouss.phoshare.data.db;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface UserDao {

    /** @throws android.database.sqlite.SQLiteConstraintException when the username is taken */
    @Insert
    long insert(User user);

    /** Case-insensitive, like the column. */
    @Nullable
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    User findByUsername(String username);

    @Nullable
    @Query("SELECT * FROM users WHERE id = :id")
    User find(long id);

    @Query("SELECT * FROM users WHERE id = :id")
    LiveData<User> observe(long id);

    @Query("UPDATE users SET passwordHash = :passwordHash WHERE id = :id")
    int updatePassword(long id, String passwordHash);

    @Query("UPDATE users SET bio = :bio, avatar = :avatar WHERE id = :id")
    int updateProfile(long id, String bio, @Nullable String avatar);

    /** Deletes the account; its posts, comments and favorites go with it (foreign keys cascade). */
    @Query("DELETE FROM users WHERE id = :id")
    int delete(long id);
}
