package es.ouss.phoshare.data.db;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;

/** A user's favorite post: at most one per user and post. */
@Entity(
        tableName = "favorites",
        primaryKeys = {"userId", "postId"},
        foreignKeys = {
                @ForeignKey(entity = User.class, parentColumns = "id", childColumns = "userId", onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = Post.class, parentColumns = "id", childColumns = "postId", onDelete = ForeignKey.CASCADE)},
        indices = {@Index("postId")})
public class Favorite {

    public long userId;

    public long postId;

    public long createdAt;

    public Favorite() {
    }

    @Ignore
    public Favorite(long userId, long postId, long createdAt) {
        this.userId = userId;
        this.postId = postId;
        this.createdAt = createdAt;
    }
}
