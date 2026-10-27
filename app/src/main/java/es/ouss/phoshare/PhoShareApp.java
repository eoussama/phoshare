package es.ouss.phoshare;

import android.app.Application;
import android.content.Context;

import es.ouss.phoshare.data.Background;
import es.ouss.phoshare.data.PhotoStore;
import es.ouss.phoshare.data.PostRepository;
import es.ouss.phoshare.data.Session;
import es.ouss.phoshare.data.UserRepository;
import es.ouss.phoshare.data.db.AppDatabase;
import es.ouss.phoshare.security.PasswordHasher;

/** The app's single instances, built once and shared by every screen. */
public class PhoShareApp extends Application {

    private Session session;
    private PhotoStore photos;
    private UserRepository users;
    private PostRepository posts;

    @Override
    public void onCreate() {
        super.onCreate();

        AppDatabase db = AppDatabase.create(this);
        Background background = new Background();
        session = new Session(this);
        photos = new PhotoStore(this);
        users = new UserRepository(db, new PasswordHasher(), photos, background);
        posts = new PostRepository(db, photos, background);
    }

    public static PhoShareApp from(Context context) {
        return (PhoShareApp) context.getApplicationContext();
    }

    public Session session() {
        return session;
    }

    public PhotoStore photos() {
        return photos;
    }

    public UserRepository users() {
        return users;
    }

    public PostRepository posts() {
        return posts;
    }
}
