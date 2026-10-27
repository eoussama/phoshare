package es.ouss.phoshare.data.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * Everything PhoShare knows, on this device only. Room turns SQLite's foreign keys on, so deleting a user or a post
 * cascades to what hangs off it. The schema of each version is exported to app/schemas for future migrations.
 */
@Database(entities = {User.class, Post.class, Comment.class, Favorite.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {

    public static final String NAME = "phoshare.db";

    public abstract UserDao users();

    public abstract PostDao posts();

    public abstract CommentDao comments();

    public abstract FavoriteDao favorites();

    public static AppDatabase create(Context context) {
        return Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, NAME).build();
    }
}
