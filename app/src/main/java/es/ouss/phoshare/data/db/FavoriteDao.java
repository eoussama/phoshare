package es.ouss.phoshare.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface FavoriteDao {

    /** Favoriting twice changes nothing. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(Favorite favorite);

    @Query("DELETE FROM favorites WHERE userId = :userId AND postId = :postId")
    int delete(long userId, long postId);
}
