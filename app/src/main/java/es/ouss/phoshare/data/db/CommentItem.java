package es.ouss.phoshare.data.db;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

/** A comment with its author, as the post screen shows it. */
public class CommentItem {

    public long id;
    public long postId;
    public long authorId;
    @NonNull
    public String text = "";
    public long createdAt;
    @NonNull
    public String authorName = "";
    @Nullable
    public String authorAvatar;

    public boolean sameContent(CommentItem other) {
        return id == other.id
                && text.equals(other.text)
                && createdAt == other.createdAt
                && authorName.equals(other.authorName)
                && Objects.equals(authorAvatar, other.authorAvatar);
    }
}
