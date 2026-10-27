package es.ouss.phoshare.data.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** A photo and its caption. The photo is a file; the row keeps its name. Deleting the author deletes the post. */
@Entity(
        tableName = "posts",
        foreignKeys = @ForeignKey(entity = User.class, parentColumns = "id", childColumns = "authorId", onDelete = ForeignKey.CASCADE),
        indices = {@Index("authorId"), @Index("createdAt")})
public class Post {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long authorId;

    /** The photo's file name in the photos folder. */
    @NonNull
    public String photo = "";

    @NonNull
    public String caption = "";

    public long createdAt;
}
