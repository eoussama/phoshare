package es.ouss.phoshare.data.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import es.ouss.phoshare.Await;

@RunWith(AndroidJUnit4.class)
public class DatabaseTest {

    private AppDatabase db;

    @Before
    public void open() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class).allowMainThreadQueries().build();
    }

    @After
    public void close() {
        db.close();
    }

    private long user(String name) {
        User user = new User();
        user.username = name;
        user.passwordHash = "hash";
        user.securityAnswerHash = "hash";
        user.joinedAt = 1;
        return db.users().insert(user);
    }

    private long post(long author, long at) {
        Post post = new Post();
        post.authorId = author;
        post.photo = "photo-" + at + ".jpg";
        post.caption = "caption " + at;
        post.createdAt = at;
        return db.posts().insert(post);
    }

    private void comment(long post, long author) {
        Comment comment = new Comment();
        comment.postId = post;
        comment.authorId = author;
        comment.text = "nice";
        comment.createdAt = 5;
        db.comments().insert(comment);
    }

    @Test(expected = SQLiteConstraintException.class)
    public void usernamesAreUniqueWhateverTheirCase() {
        user("Alice");
        user("alice");
    }

    @Test
    public void findsUsernamesWhateverTheirCase() {
        long id = user("Alice");

        User found = db.users().findByUsername("ALICE");
        assertNotNull(found);
        assertEquals(id, found.id);
        assertEquals("Alice", found.username);
        assertNull(db.users().findByUsername("bob"));
    }

    @Test
    public void feedIsNewestFirstWithCountsAndTheViewersFavorites() throws Exception {
        long alice = user("alice");
        long bob = user("bob");
        long old = post(alice, 100);
        long recent = post(bob, 200);
        comment(old, bob);
        comment(old, alice);
        db.favorites().insert(new Favorite(bob, old, 300));
        db.favorites().insert(new Favorite(alice, old, 301));

        List<PostItem> seenByBob = Await.value(db.posts().feed(bob));

        assertEquals(2, seenByBob.size());
        assertEquals(recent, seenByBob.get(0).id);
        assertEquals("bob", seenByBob.get(0).authorName);
        assertEquals(0, seenByBob.get(0).commentCount);
        assertFalse(seenByBob.get(0).favorite);
        assertEquals(old, seenByBob.get(1).id);
        assertEquals(2, seenByBob.get(1).commentCount);
        assertEquals(2, seenByBob.get(1).favoriteCount);
        assertTrue(seenByBob.get(1).favorite);
    }

    @Test
    public void favoritingTwiceChangesNothing() throws Exception {
        long alice = user("alice");
        long post = post(alice, 100);

        db.favorites().insert(new Favorite(alice, post, 1));
        db.favorites().insert(new Favorite(alice, post, 2));

        assertEquals(1, Await.value(db.posts().item(post, alice)).favoriteCount);
        assertEquals(1, Await.value(db.posts().favorites(alice)).size());

        db.favorites().delete(alice, post);
        assertEquals(0, Await.value(db.posts().favorites(alice)).size());
    }

    @Test
    public void deletingAUserDeletesWhatTheyPostedAndDid() throws Exception {
        long alice = user("alice");
        long bob = user("bob");
        long alicePost = post(alice, 100);
        long bobPost = post(bob, 200);
        comment(bobPost, alice);
        comment(alicePost, bob);
        db.favorites().insert(new Favorite(alice, bobPost, 1));

        db.users().delete(alice);

        List<PostItem> feed = Await.value(db.posts().feed(bob));
        assertEquals(1, feed.size());
        assertEquals(bobPost, feed.get(0).id);
        assertEquals(0, feed.get(0).commentCount);
        assertEquals(0, feed.get(0).favoriteCount);
        assertNull(db.posts().find(alicePost));
    }

    @Test
    public void deletingAPostDeletesItsComments() throws Exception {
        long alice = user("alice");
        long post = post(alice, 100);
        comment(post, alice);

        db.posts().delete(post);

        assertTrue(Await.value(db.comments().forPost(post)).isEmpty());
        assertNull(Await.value(db.posts().item(post, alice)));
    }

    @Test
    public void onlyTheAuthorDeletesAComment() throws Exception {
        long alice = user("alice");
        long bob = user("bob");
        long post = post(alice, 100);
        comment(post, alice);
        long id = Await.value(db.comments().forPost(post)).get(0).id;

        assertEquals(0, db.comments().delete(id, bob));
        assertEquals(1, db.comments().delete(id, alice));
    }
}
