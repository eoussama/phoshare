package es.ouss.phoshare.data.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** A comment on a post; it goes with the post or with its author. */
@Entity(
        tableName = "comments",
        foreignKeys = {
                @ForeignKey(entity = Post.class, parentColumns = "id", childColumns = "postId", onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = User.class, parentColumns = "id", childColumns = "authorId", onDelete = ForeignKey.CASCADE)},
        indices = {@Index("postId"), @Index("authorId")})
public class Comment {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long postId;

    public long authorId;

    @NonNull
    public String text = "";

    public long createdAt;
}
