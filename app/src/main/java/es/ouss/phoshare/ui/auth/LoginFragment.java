package es.ouss.phoshare.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import es.ouss.phoshare.PhoShareApp;
import es.ouss.phoshare.R;
import es.ouss.phoshare.data.Problem;
import es.ouss.phoshare.data.Result;
import es.ouss.phoshare.data.db.User;
import es.ouss.phoshare.databinding.FragmentLoginBinding;
import es.ouss.phoshare.security.Credentials;
import es.ouss.phoshare.ui.common.Forms;
import es.ouss.phoshare.ui.common.Insets;
import es.ouss.phoshare.ui.common.Screens;
import es.ouss.phoshare.ui.common.TaskViewModel;

public class LoginFragment extends Fragment {

    private static final int LOG_IN = 1;

    private FragmentLoginBinding binding;
    private TaskViewModel tasks;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Insets.all(binding.scroll);
        tasks = new ViewModelProvider(this).get(TaskViewModel.class);

        Forms.clearErrorsOnEdit(binding.usernameLayout, binding.passwordLayout);
        Forms.onDone(binding.password, this::logIn);
        binding.logIn.setOnClickListener(v -> logIn());
        binding.forgotPassword.setOnClickListener(v -> {
            Bundle arguments = new Bundle();
            arguments.putString("username", Credentials.normalizeUsername(Forms.text(binding.username)));
            NavHostFragment.findNavController(this).navigate(R.id.recover, arguments);
        });
        binding.createAccount.setOnClickListener(v -> NavHostFragment.findNavController(this).navigate(R.id.sign_up));

        tasks.busy().observe(getViewLifecycleOwner(), busy -> {
            binding.progress.setVisibility(busy ? View.VISIBLE : View.INVISIBLE);
            binding.logIn.setEnabled(!busy);
        });
        tasks.outcome().observe(getViewLifecycleOwner(), outcome -> {
            if (outcome != null) {
                tasks.consumed();
                onLoggedIn(outcome.as());
            }
        });
    }

    private void logIn() {
        String username = Forms.text(binding.username);
        String password = Forms.text(binding.password);
        boolean valid = Forms.check(binding.usernameLayout, username.trim().isEmpty() ? R.string.error_username_empty : 0);
        valid &= Forms.check(binding.passwordLayout, password.isEmpty() ? R.string.error_password_empty : 0);
        if (!valid) {
            return;
        }

        Forms.hideKeyboard(binding.password);
        PhoShareApp app = PhoShareApp.from(requireContext());
        tasks.<User>run(LOG_IN, callback -> app.users().logIn(username, password, callback));
    }

    private void onLoggedIn(Result<User> result) {
        if (result.isOk() && result.value != null) {
            Screens.enterApp(requireActivity(), result.value.id, null);
        } else if (result.problem == Problem.UNKNOWN_USER) {
            Forms.check(binding.usernameLayout, result.problem.message);
        } else if (result.problem == Problem.WRONG_PASSWORD) {
            Forms.check(binding.passwordLayout, result.problem.message);
        } else {
            Screens.message(this, R.string.error_unexpected);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
