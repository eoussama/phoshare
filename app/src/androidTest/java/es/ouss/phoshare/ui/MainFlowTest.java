package es.ouss.phoshare.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.intent.matcher.IntentMatchers.isInternal;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.net.Uri;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import es.ouss.phoshare.Await;
import es.ouss.phoshare.PhoShareApp;
import es.ouss.phoshare.R;
import es.ouss.phoshare.Screenshots;
import es.ouss.phoshare.data.Result;
import es.ouss.phoshare.data.db.User;

/**
 * The logged-in app on a device: post a photo (the photo picker answers with a fixture), favorite it, comment on it,
 * share it (the share sheet is only expected), and find it in the favorites and on the profile, in light and dark.
 * Screenshots of each step land in test-results/android/flow.
 */
@RunWith(AndroidJUnit4.class)
public class MainFlowTest {

    private static final String CAPTION = "Me, myself and I, at the beach";
    private static final String COMMENT = "Great shot, me.";

    private Context context;

    @Before
    public void loggedIn() throws Exception {
        context = ApplicationProvider.getApplicationContext();
        PhoShareApp app = PhoShareApp.from(context);
        String username = "me_" + (System.currentTimeMillis() % 100_000_000L);

        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Result<User>> signedUp = new AtomicReference<>();
        app.users().signUp(username, "a fine password", 0, "yellow", result -> {
            signedUp.set(result);
            done.countDown();
        });
        if (!done.await(30, TimeUnit.SECONDS) || !signedUp.get().isOk()) {
            throw new AssertionError("Could not sign up " + username);
        }
        app.session().logIn(signedUp.get().value.id);

        Intents.init();
        // Every activity of another app answers at once: the photo picker with the fixture, the share sheet with nothing.
        Intent picked = new Intent().setData(fixture());
        intending(not(isInternal())).respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, picked));
    }

    @After
    public void tearDown() {
        Intents.release();
        Screenshots.shell("cmd uimode night no");
    }

    /** A sunny gradient with a sun, as the photo to post. */
    private Uri fixture() throws IOException {
        Bitmap bitmap = Bitmap.createBitmap(1200, 1200, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint sky = new Paint();
        sky.setShader(new LinearGradient(0, 0, 0, 1200, Color.rgb(255, 214, 102), Color.rgb(240, 84, 84), Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, 1200, 1200, sky);
        Paint sun = new Paint(Paint.ANTI_ALIAS_FLAG);
        sun.setColor(Color.rgb(251, 208, 0));
        canvas.drawCircle(600, 520, 220, sun);
        Paint sea = new Paint();
        sea.setColor(Color.rgb(55, 71, 79));
        canvas.drawRect(0, 860, 1200, 1200, sea);

        File file = new File(context.getCacheDir(), "fixture.jpg");
        try (OutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
        }
        bitmap.recycle();
        return Uri.fromFile(file);
    }

    @Test
    public void postFavoriteCommentShareAndFindItAgain() {
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            Await.displayed(withId(R.id.new_post));
            Screenshots.take("01-feed");

            // A new post, from the "gallery".
            onView(withId(R.id.new_post)).perform(click());
            onView(withId(R.id.gallery)).perform(click());
            Await.displayed(withId(R.id.preview));
            onView(withId(R.id.caption)).perform(replaceText(CAPTION), closeSoftKeyboard());
            Screenshots.take("02-new-post");
            onView(withId(R.id.publish)).perform(scrollTo(), click());

            // On the feed, favorited.
            Await.displayed(allOf(withId(R.id.caption), withText(CAPTION)));
            onView(allOf(withId(R.id.favorite), isDescendantOfA(withId(R.id.list)))).perform(click());
            Await.displayed(allOf(withId(R.id.favorite), withText("1")));
            Screenshots.take("03-feed-posted");

            // Its own screen: a comment, and the share sheet.
            onView(allOf(withId(R.id.caption), withText(CAPTION))).perform(click());
            Await.displayed(withId(R.id.comment));
            onView(withId(R.id.comment)).perform(replaceText(COMMENT), closeSoftKeyboard());
            onView(withId(R.id.send)).perform(click());
            Await.displayed(withText(COMMENT));
            Screenshots.take("04-post");
            onView(withId(R.id.share)).perform(click());
            intended(hasAction(Intent.ACTION_CHOOSER));
            pressBack();

            // In the favorites, and on the profile.
            Await.displayed(withId(R.id.favorites));
            onView(withId(R.id.favorites)).perform(click());
            Await.displayed(allOf(withId(R.id.caption), withText(CAPTION)));
            Screenshots.take("05-favorites");
            onView(withId(R.id.profile)).perform(click());
            Await.displayed(withText(context.getResources().getQuantityString(R.plurals.profile_post_count, 1, 1)));
            Screenshots.take("06-profile");

            // The same in the dark.
            Screenshots.shell("cmd uimode night yes");
            Await.displayed(withText(context.getResources().getQuantityString(R.plurals.profile_post_count, 1, 1)));
            Screenshots.take("07-profile-dark");
            onView(withId(R.id.feed)).perform(click());
            Await.displayed(allOf(withId(R.id.caption), withText(CAPTION)));
            Screenshots.take("08-feed-dark");
        }
    }
}
