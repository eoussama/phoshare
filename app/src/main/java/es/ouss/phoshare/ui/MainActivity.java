package es.ouss.phoshare.ui;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import es.ouss.phoshare.PhoShareApp;
import es.ouss.phoshare.R;
import es.ouss.phoshare.data.Session;
import es.ouss.phoshare.databinding.ActivityMainBinding;
import es.ouss.phoshare.ui.common.Screens;

/**
 * The app's entry point. Behind the launch screen it sends whoever is not logged in to the login screens; for the
 * logged-in user it hosts the main screens, with the bottom navigation on the three tabs.
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        PhoShareApp app = PhoShareApp.from(this);
        Session session = app.session();
        if (!session.isLoggedIn()) {
            Screens.logOut(this, false);
            return;
        }

        EdgeToEdge.enable(this);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavHostFragment host = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host);
        NavController navigation = host.getNavController();
        NavigationUI.setupWithNavController(binding.bottomNav, navigation);
        navigation.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int id = destination.getId();
            boolean tab = id == R.id.feed || id == R.id.favorites || id == R.id.profile;
            binding.bottomNav.setVisibility(tab ? View.VISIBLE : View.GONE);
        });

        // The remembered account may be gone (its data cleared from a backup restore, say): back to the login then.
        app.users().exists(session.userId(), result -> {
            if (result.isOk() && Boolean.FALSE.equals(result.value) && !isFinishing()) {
                Screens.logOut(this, false);
            }
        });
    }
}
