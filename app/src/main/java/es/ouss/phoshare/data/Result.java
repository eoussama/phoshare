package es.ouss.phoshare.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** What a repository call came to: a value, or the {@link Problem} that stopped it. */
public final class Result<T> {

    @Nullable
    public final T value;

    @Nullable
    public final Problem problem;

    private Result(@Nullable T value, @Nullable Problem problem) {
        this.value = value;
        this.problem = problem;
    }

    public static <T> Result<T> ok(@Nullable T value) {
        return new Result<>(value, null);
    }

    public static <T> Result<T> fail(@NonNull Problem problem) {
        return new Result<>(null, problem);
    }

    public boolean isOk() {
        return problem == null;
    }

    /** Receives a result, always on the main thread. */
    public interface Callback<T> {
        void onResult(@NonNull Result<T> result);
    }
}
