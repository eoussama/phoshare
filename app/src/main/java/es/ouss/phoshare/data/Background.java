package es.ouss.phoshare.data;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Where the repositories do their work: one background thread, so writes happen in the order they were asked for, and
 * results come back on the main thread.
 */
public final class Background {

    private static final String TAG = "PhoShare";

    private final Executor work;
    private final Executor main;

    public Background() {
        this(newWorker(), new MainThread());
    }

    public Background(Executor work, Executor main) {
        this.work = work;
        this.main = main;
    }

    /** A unit of work that produces a result (or throws, which becomes {@link Problem#UNEXPECTED}). */
    public interface Task<T> {
        @NonNull
        Result<T> run() throws Exception;
    }

    /** Runs the task in the background and hands its result to the callback on the main thread. */
    public <T> void run(Task<T> task, Result.Callback<T> callback) {
        work.execute(() -> {
            Result<T> result;
            try {
                result = task.run();
            } catch (Exception e) {
                Log.e(TAG, "Background task failed", e);
                result = Result.fail(Problem.UNEXPECTED);
            }
            Result<T> delivered = result;
            main.execute(() -> callback.onResult(delivered));
        });
    }

    /** Runs a write whose outcome nobody waits for. */
    public void fire(Runnable task) {
        work.execute(() -> {
            try {
                task.run();
            } catch (RuntimeException e) {
                Log.e(TAG, "Background write failed", e);
            }
        });
    }

    private static ExecutorService newWorker() {
        return Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "phoshare-io");
            thread.setPriority(Thread.NORM_PRIORITY - 1);
            return thread;
        });
    }

    private static final class MainThread implements Executor {
        private final Handler handler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(Runnable command) {
            handler.post(command);
        }
    }
}
