package es.ouss.phoshare;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;

import android.os.SystemClock;
import android.view.View;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.test.platform.app.InstrumentationRegistry;

import org.hamcrest.Matcher;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Waiting in tests. Espresso only waits for the main thread, and the app hashes passwords on its own background thread
 * (slowly, on purpose), so the UI tests poll for what they expect instead.
 */
public final class Await {

    private static final long TIMEOUT_MS = 30_000;

    private Await() {
    }

    /** Waits until a view matching the matcher is on screen. */
    public static void displayed(Matcher<View> matcher) {
        long deadline = SystemClock.uptimeMillis() + TIMEOUT_MS;
        while (true) {
            try {
                onView(matcher).check(matches(isDisplayed()));
                return;
            } catch (RuntimeException | AssertionError e) {
                if (SystemClock.uptimeMillis() > deadline) {
                    throw e;
                }
                SystemClock.sleep(250);
            }
        }
    }

    /** The first value a LiveData holds (Room computes it on its own thread), observed on the main thread. */
    public static <T> T value(LiveData<T> data) throws InterruptedException {
        AtomicReference<T> value = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Observer<T> observer = new Observer<>() {
            @Override
            public void onChanged(T changed) {
                value.set(changed);
                latch.countDown();
                data.removeObserver(this);
            }
        };
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> data.observeForever(observer));
        if (!latch.await(10, TimeUnit.SECONDS)) {
            throw new AssertionError("The LiveData never got a value");
        }
        return value.get();
    }
}
