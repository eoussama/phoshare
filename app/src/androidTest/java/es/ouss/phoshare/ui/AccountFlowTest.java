package es.ouss.phoshare.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Context;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import es.ouss.phoshare.Await;
import es.ouss.phoshare.PhoShareApp;
import es.ouss.phoshare.R;

/**
 * The whole account story on a device: sign up, land on the feed, log out, log back in, forget the password, reset it
 * with the security question (answered in another case), and log in with the new one.
 */
@RunWith(AndroidJUnit4.class)
public class AccountFlowTest {

    private Context context;

    @Before
    public void loggedOut() {
        context = ApplicationProvider.getApplicationContext();
        PhoShareApp.from(context).session().logOut();
    }

    @Test
    public void signUpLogInAndResetThePassword() {
        String username = "u" + (System.currentTimeMillis() % 1_000_000_000L);
        String question = context.getResources().getStringArray(R.array.security_questions)[0];

        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            // Not logged in: the login screen.
            Await.displayed(withId(R.id.create_account));
            onView(withId(R.id.create_account)).perform(scrollTo(), click());

            onView(withId(R.id.username)).perform(replaceText(username));
            onView(withId(R.id.password)).perform(replaceText("first password"));
            onView(withId(R.id.confirmation)).perform(replaceText("first password"));
            onView(withId(R.id.question)).perform(replaceText(question), closeSoftKeyboard());
            onView(withId(R.id.answer)).perform(scrollTo(), replaceText("Light Blue"), closeSoftKeyboard());
            onView(withId(R.id.terms)).perform(scrollTo(), click());
            onView(withId(R.id.sign_up)).perform(scrollTo(), click());

            // Signed up and in: the feed, with the bottom navigation.
            Await.displayed(withId(R.id.bottom_nav));
            logOut();

            logIn(username, "first password");
            logOut();

            // Forgot the password: the username carries over from the login form.
            onView(withId(R.id.username)).perform(replaceText(username));
            onView(withId(R.id.forgot_password)).perform(scrollTo(), click());
            onView(withId(R.id.username_next)).perform(click());
            Await.displayed(withText(question));
            onView(withId(R.id.answer)).perform(replaceText("  LIGHT blue "), closeSoftKeyboard());
            onView(withId(R.id.answer_next)).perform(click());
            Await.displayed(withId(R.id.reset));
            onView(withId(R.id.password)).perform(replaceText("second password"));
            onView(withId(R.id.confirmation)).perform(replaceText("second password"), closeSoftKeyboard());
            onView(withId(R.id.reset)).perform(click());

            // Back on the login: the old password is refused, the new one works.
            Await.displayed(withId(R.id.log_in));
            onView(withId(R.id.username)).perform(replaceText(username));
            onView(withId(R.id.password)).perform(replaceText("first password"), closeSoftKeyboard());
            onView(withId(R.id.log_in)).perform(scrollTo(), click());
            Await.displayed(withText(R.string.error_wrong_password));

            logIn(username, "second password");
            onView(withId(R.id.bottom_nav)).check(matches(isDisplayed()));
        }
    }

    private void logIn(String username, String password) {
        Await.displayed(withId(R.id.log_in));
        onView(withId(R.id.username)).perform(replaceText(username));
        onView(withId(R.id.password)).perform(replaceText(password), closeSoftKeyboard());
        onView(withId(R.id.log_in)).perform(scrollTo(), click());
        Await.displayed(withId(R.id.bottom_nav));
    }

    private void logOut() {
        onView(withId(R.id.profile)).perform(click());
        Await.displayed(withContentDescription(androidx.appcompat.R.string.abc_action_menu_overflow_description));
        onView(withContentDescription(androidx.appcompat.R.string.abc_action_menu_overflow_description)).perform(click());
        onView(withText(R.string.profile_log_out)).perform(click());
        onView(withId(android.R.id.button1)).perform(click());
        Await.displayed(withId(R.id.log_in));
    }
}
