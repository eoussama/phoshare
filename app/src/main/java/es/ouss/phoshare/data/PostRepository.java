package es.ouss.phoshare.data;

import android.net.Uri;

import androidx.lifecycle.LiveData;

import java.io.IOException;
import java.util.List;

import es.ouss.phoshare.data.db.AppDatabase;
import es.ouss.phoshare.data.db.Comment;
import es.ouss.phoshare.data.db.CommentItem;
import es.ouss.phoshare.data.db.Favorite;
import es.ouss.phoshare.data.db.Post;
import es.ouss.phoshare.data.db.PostItem;

/** Posts, their comments and favorites. Lists are live: they update on screen as soon as anything changes. */
public final class PostRepository {

    private final AppDatabase db;
    private final PhotoStore photos;
    private final Background background;

    public PostRepository(AppDatabase db, PhotoStore photos, Background background) {
        this.db = db;
        this.photos = photos;
        this.background = background;
    }

    public LiveData<List<PostItem>> feed(long viewerId) {
        return db.posts().feed(viewerId);
    }

    public LiveData<List<PostItem>> favorites(long viewerId) {
        return db.posts().favorites(viewerId);
    }

    public LiveData<List<PostItem>> byAuthor(long authorId, long viewerId) {
        return db.posts().byAuthor(authorId, viewerId);
    }

    public LiveData<PostItem> item(long postId, long viewerId) {
        return db.posts().item(postId, viewerId);
    }

    public LiveData<List<CommentItem>> comments(long postId) {
        return db.comments().forPost(postId);
    }

    /** Imports the photo and posts it; returns the new post's id. */
    public void publish(long authorId, Uri photo, String caption, Result.Callback<Long> callback) {
        background.run(() -> {
            String name;
            try {
                name = photos.importPhoto(photo);
            } catch (IOException | RuntimeException e) {
                return Result.fail(Problem.PHOTO);
            } finally {
                photos.clearCamera();
            }

            Post post = new Post();
            post.authorId = authorId;
            post.photo = name;
            post.caption = caption.trim();
            post.createdAt = System.currentTimeMillis();
            try {
                return Result.ok(db.posts().insert(post));
            } catch (RuntimeException e) {
                photos.deletePhoto(name);
                throw e;
            }
        }, callback);
    }

    /** Deletes a post and its photo, only for its author. */
    public void delete(long postId, long requesterId, Result.Callback<Void> callback) {
        background.run(() -> {
            Post post = db.posts().find(postId);
            if (post == null || post.authorId != requesterId) {
                return Result.fail(Problem.UNEXPECTED);
            }
            db.posts().delete(postId);
            photos.deletePhoto(post.photo);
            return Result.ok(null);
        }, callback);
    }

    public void setFavorite(long userId, long postId, boolean favorite) {
        background.fire(() -> {
            if (favorite) {
                db.favorites().insert(new Favorite(userId, postId, System.currentTimeMillis()));
            } else {
                db.favorites().delete(userId, postId);
            }
        });
    }

    public void comment(long postId, long authorId, String text, Result.Callback<Void> callback) {
        background.run(() -> {
            Comment comment = new Comment();
            comment.postId = postId;
            comment.authorId = authorId;
            comment.text = text.trim();
            comment.createdAt = System.currentTimeMillis();
            db.comments().insert(comment);
            return Result.ok(null);
        }, callback);
    }

    /** Deletes a comment, only for its author. */
    public void deleteComment(long commentId, long authorId) {
        background.fire(() -> db.comments().delete(commentId, authorId));
    }
}
