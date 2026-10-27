package es.ouss.phoshare;

import android.os.ParcelFileDescriptor;
import android.os.SystemClock;

import androidx.test.platform.app.InstrumentationRegistry;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Screenshots of the UI tests, for people to look at: taken by the shell user into a folder that outlives the app
 * (Gradle uninstalls it after the tests), which tools/android/device.sh pulls into test-results/android/flow.
 */
public final class Screenshots {

    public static final String FOLDER = "/data/local/tmp/phoshare-shots";

    private Screenshots() {
    }

    public static void take(String name) {
        // Let animations and the image loader settle.
        SystemClock.sleep(800);
        shell("mkdir -p " + FOLDER);
        shell("screencap -p " + FOLDER + "/" + name + ".png");
    }

    /** Runs a shell command as the shell user and waits for it to finish. */
    public static void shell(String command) {
        ParcelFileDescriptor output = InstrumentationRegistry.getInstrumentation().getUiAutomation().executeShellCommand(command);
        try (InputStream in = new FileInputStream(output.getFileDescriptor())) {
            byte[] buffer = new byte[4096];
            //noinspection StatementWithEmptyBody
            while (in.read(buffer) != -1) {
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            try {
                output.close();
            } catch (IOException ignored) {
                // Nothing left to read from it.
            }
        }
    }
}
