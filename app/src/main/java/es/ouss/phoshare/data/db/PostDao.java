package es.ouss.phoshare.data.db;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PostDao {

    /** A post with its author and counts, seen by the user :viewerId. */
    String ITEM = "SELECT p.id, p.authorId, p.photo, p.caption, p.createdAt,"
            + " u.username AS authorName, u.avatar AS authorAvatar,"
            + " (SELECT COUNT(*) FROM comments c WHERE c.postId = p.id) AS commentCount,"
            + " (SELECT COUNT(*) FROM favorites f WHERE f.postId = p.id) AS favoriteCount,"
            + " EXISTS (SELECT 1 FROM favorites f WHERE f.postId = p.id AND f.userId = :viewerId) AS favorite"
            + " FROM posts p JOIN users u ON u.id = p.authorId";

    @Insert
    long insert(Post post);

    @Nullable
    @Query("SELECT * FROM posts WHERE id = :id")
    Post find(long id);

    @Query("DELETE FROM posts WHERE id = :id")
    int delete(long id);

    /** The photo files of a user's posts, to delete with the account. */
    @Query("SELECT photo FROM posts WHERE authorId = :authorId")
    List<String> photosBy(long authorId);

    /** Everyone's posts, newest first. */
    @Query(ITEM + " ORDER BY p.createdAt DESC, p.id DESC")
    LiveData<List<PostItem>> feed(long viewerId);

    /** The viewer's favorites, the most recently favorited first. */
    @Query(ITEM + " JOIN favorites mine ON mine.postId = p.id AND mine.userId = :viewerId"
            + " ORDER BY mine.createdAt DESC, p.id DESC")
    LiveData<List<PostItem>> favorites(long viewerId);

    @Query(ITEM + " WHERE p.authorId = :authorId ORDER BY p.createdAt DESC, p.id DESC")
    LiveData<List<PostItem>> byAuthor(long authorId, long viewerId);

    @Query(ITEM + " WHERE p.id = :postId")
    LiveData<PostItem> item(long postId, long viewerId);
}
