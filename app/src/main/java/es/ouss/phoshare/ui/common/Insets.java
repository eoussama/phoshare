package es.ouss.phoshare.ui.common;

import android.view.View;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * The app draws edge to edge (Android 15 and later insist): views that sit at the screen's edges move their content out
 * from under the status bar, the navigation bar, display cutouts and the keyboard, on top of their own padding.
 */
public final class Insets {

    private Insets() {
    }

    /** Pads every side, keyboard included: for a screen without an app bar. */
    public static void all(View view) {
        pad(view, true, true);
    }

    /** Pads the sides and the top: for app bars. */
    public static void top(View view) {
        pad(view, true, false);
    }

    /** Pads the sides and the bottom, keyboard included: for the content of screens without the bottom navigation. */
    public static void bottom(View view) {
        pad(view, false, true);
    }

    /** Pads the sides only: for the content between an app bar and the bottom navigation. */
    public static void sides(View view) {
        pad(view, false, false);
    }

    private static void pad(View view, boolean top, boolean bottom) {
        int left = view.getPaddingLeft();
        int right = view.getPaddingRight();
        int start = view.getPaddingTop();
        int end = view.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            int types = WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout();
            androidx.core.graphics.Insets bars = insets.getInsets(types);
            int keyboard = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            v.setPadding(
                    left + bars.left,
                    start + (top ? bars.top : 0),
                    right + bars.right,
                    end + (bottom ? Math.max(bars.bottom, keyboard) : 0));
            return insets;
        });
        ViewCompat.requestApplyInsets(view);
    }
}
