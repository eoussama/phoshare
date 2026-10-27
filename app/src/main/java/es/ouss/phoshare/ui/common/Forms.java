package es.ouss.phoshare.ui.common;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.StringRes;

import com.google.android.material.textfield.TextInputLayout;

/** Small helpers every form uses: read a field, show its error, submit from the keyboard. */
public final class Forms {

    private Forms() {
    }

    public static String text(TextView field) {
        CharSequence text = field.getText();
        return text == null ? "" : text.toString();
    }

    /**
     * Shows the message under the field, or clears it for {@code 0}.
     *
     * @return whether the field is valid
     */
    public static boolean check(TextInputLayout layout, @StringRes int message) {
        layout.setError(message == 0 ? null : layout.getContext().getString(message));
        return message == 0;
    }

    /** Clears the fields' errors as soon as they are edited. */
    public static void clearErrorsOnEdit(TextInputLayout... layouts) {
        for (TextInputLayout layout : layouts) {
            EditText field = layout.getEditText();
            if (field == null) {
                continue;
            }
            field.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    layout.setError(null);
                }
            });
        }
    }

    /** Runs the action when the keyboard's "done" key is pressed in the field. */
    public static void onDone(EditText field, Runnable action) {
        field.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                action.run();
                return true;
            }
            return false;
        });
    }

    public static void hideKeyboard(View view) {
        InputMethodManager keyboard = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (keyboard != null) {
            keyboard.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public static void showKeyboard(View view) {
        view.requestFocus();
        InputMethodManager keyboard = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (keyboard != null) {
            keyboard.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        }
    }
}
