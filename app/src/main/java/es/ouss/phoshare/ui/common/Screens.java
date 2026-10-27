package es.ouss.phoshare.ui.common;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.snackbar.Snackbar;

import es.ouss.phoshare.PhoShareApp;
import es.ouss.phoshare.R;
import es.ouss.phoshare.databinding.ViewAppBarBinding;
import es.ouss.phoshare.ui.MainActivity;
import es.ouss.phoshare.ui.auth.AuthActivity;

/** What screens share: their app bar, their messages, and moving between the logged-out and logged-in app. */
public final class Screens {

    private Screens() {
    }

    /** Sets up the app bar: out from under the status bar, titled, and with a back arrow on screens below a tab. */
    public static void appBar(Fragment fragment, ViewAppBarBinding bar, @StringRes int title, boolean up) {
        Insets.top(bar.appBar);
        bar.toolbar.setTitle(title);
        if (up) {
            bar.toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
            bar.toolbar.setNavigationContentDescription(R.string.navigate_up);
            bar.toolbar.setNavigationOnClickListener(v -> NavHostFragment.findNavController(fragment).navigateUp());
        }
    }

    /** Shows the app bar's progress bar while a slow call runs. */
    public static void busy(ViewAppBarBinding bar, boolean busy) {
        bar.progress.setVisibility(busy ? View.VISIBLE : View.INVISIBLE);
    }

    public static void message(Fragment fragment, @StringRes int message) {
        View view = fragment.getView();
        if (view != null) {
            Snackbar.make(view, message, Snackbar.LENGTH_LONG).show();
        }
    }

    /** A message that outlives the screen (the screen is about to close). */
    public static void toast(Activity activity, CharSequence message) {
        Toast.makeText(activity.getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    /** Remembers the account and opens the app on its feed, closing the login screens. */
    public static void enterApp(Activity activity, long userId, @Nullable CharSequence greeting) {
        PhoShareApp.from(activity).session().logIn(userId);
        if (greeting != null) {
            toast(activity, greeting);
        }
        activity.startActivity(new Intent(activity, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        activity.finish();
    }

    /** Forgets the account and opens the login screen (or the sign-up form, to make another account). */
    public static void logOut(Activity activity, boolean signUp) {
        PhoShareApp.from(activity).session().logOut();
        activity.startActivity(new Intent(activity, AuthActivity.class)
                .putExtra(AuthActivity.EXTRA_SIGN_UP, signUp)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        activity.finish();
    }
}
