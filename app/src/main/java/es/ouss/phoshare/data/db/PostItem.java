package es.ouss.phoshare.data.db;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

/** A post as a screen shows it: with its author, its counts, and whether the viewer favorited it. */
public class PostItem {

    public long id;
    public long authorId;
    @NonNull
    public String photo = "";
    @NonNull
    public String caption = "";
    public long createdAt;
    @NonNull
    public String authorName = "";
    @Nullable
    public String authorAvatar;
    public int commentCount;
    public int favoriteCount;
    public boolean favorite;

    /** Whether the two rows would look the same on screen (for list diffing). */
    public boolean sameContent(PostItem other) {
        return id == other.id
                && authorId == other.authorId
                && photo.equals(other.photo)
                && caption.equals(other.caption)
                && createdAt == other.createdAt
                && authorName.equals(other.authorName)
                && Objects.equals(authorAvatar, other.authorAvatar)
                && commentCount == other.commentCount
                && favoriteCount == other.favoriteCount
                && favorite == other.favorite;
    }
}
