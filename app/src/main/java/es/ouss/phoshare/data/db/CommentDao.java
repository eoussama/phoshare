package es.ouss.phoshare.data.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface CommentDao {

    @Insert
    long insert(Comment comment);

    /** Deletes a comment, only if :authorId wrote it. */
    @Query("DELETE FROM comments WHERE id = :id AND authorId = :authorId")
    int delete(long id, long authorId);

    /** A post's comments, oldest first, like a conversation. */
    @Query("SELECT c.id, c.postId, c.authorId, c.text, c.createdAt, u.username AS authorName, u.avatar AS authorAvatar"
            + " FROM comments c JOIN users u ON u.id = c.authorId"
            + " WHERE c.postId = :postId ORDER BY c.createdAt, c.id")
    LiveData<List<CommentItem>> forPost(long postId);
}
