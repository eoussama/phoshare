package es.ouss.phoshare.ui.common;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import es.ouss.phoshare.data.Result;

/**
 * Runs a screen's repository calls so their results survive a rotation: the screen observes {@link #busy()} and
 * {@link #outcome()}, handles the outcome once, and calls {@link #consumed()}. One call at a time.
 */
public class TaskViewModel extends ViewModel {

    /** A finished call: which one (the screen's own constant) and its result. */
    public static final class Outcome {
        public final int what;
        @NonNull
        public final Result<?> result;

        Outcome(int what, @NonNull Result<?> result) {
            this.what = what;
            this.result = result;
        }

        /** The result, typed as the call that {@link #what} names returns it. */
        @SuppressWarnings("unchecked")
        public <T> Result<T> as() {
            return (Result<T>) result;
        }
    }

    /** Starts a repository call with the callback it must report to. */
    public interface Starter<T> {
        void start(Result.Callback<T> callback);
    }

    private final MutableLiveData<Boolean> busy = new MutableLiveData<>(false);
    private final MutableLiveData<Outcome> outcome = new MutableLiveData<>();

    public LiveData<Boolean> busy() {
        return busy;
    }

    public LiveData<Outcome> outcome() {
        return outcome;
    }

    public boolean isBusy() {
        return Boolean.TRUE.equals(busy.getValue());
    }

    /** Starts the call unless one is running already. */
    public <T> void run(int what, Starter<T> starter) {
        if (isBusy()) {
            return;
        }
        busy.setValue(true);
        starter.start(result -> {
            busy.setValue(false);
            outcome.setValue(new Outcome(what, result));
        });
    }

    public void consumed() {
        outcome.setValue(null);
    }

    /** The outcome to handle now, or null. */
    @Nullable
    public Outcome pending() {
        return outcome.getValue();
    }
}
