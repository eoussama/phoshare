package es.ouss.phoshare.ui.auth;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.fragment.NavHostFragment;

import es.ouss.phoshare.R;

/** The login, the sign-up and the password reset, before anyone is logged in. */
public class AuthActivity extends AppCompatActivity {

    /** Open on the sign-up form rather than the login ("make another account"). */
    public static final String EXTRA_SIGN_UP = "sign_up";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_auth);

        if (savedInstanceState == null && getIntent().getBooleanExtra(EXTRA_SIGN_UP, false)) {
            NavHostFragment host = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host);
            host.getNavController().navigate(R.id.sign_up);
        }
    }
}
