package es.ouss.phoshare.ui.main;

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
import es.ouss.phoshare.databinding.FragmentChangePasswordBinding;
import es.ouss.phoshare.security.Credentials;
import es.ouss.phoshare.ui.common.Forms;
import es.ouss.phoshare.ui.common.Insets;
import es.ouss.phoshare.ui.common.Screens;
import es.ouss.phoshare.ui.common.TaskViewModel;

public class ChangePasswordFragment extends Fragment {

    private static final int CHANGE = 1;

    private FragmentChangePasswordBinding binding;
    private TaskViewModel tasks;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentChangePasswordBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        PhoShareApp app = PhoShareApp.from(requireContext());
        Screens.appBar(this, binding.bar, R.string.profile_change_password, true);
        Insets.bottom(binding.scroll);
        tasks = new ViewModelProvider(this).get(TaskViewModel.class);

        Forms.clearErrorsOnEdit(binding.currentLayout, binding.passwordLayout, binding.confirmationLayout);
        Runnable save = () -> {
            String current = Forms.text(binding.current);
            String password = Forms.text(binding.password);
            boolean valid = Forms.check(binding.currentLayout, current.isEmpty() ? R.string.error_password_empty : 0);
            valid &= Forms.check(binding.passwordLayout, Credentials.checkPassword(password));
            valid &= Forms.check(binding.confirmationLayout, Credentials.checkConfirmation(password, Forms.text(binding.confirmation)));
            if (valid) {
                Forms.hideKeyboard(binding.save);
                long me = app.session().userId();
                tasks.<Void>run(CHANGE, callback -> app.users().changePassword(me, current, password, callback));
            }
        };
        binding.save.setOnClickListener(v -> save.run());
        Forms.onDone(binding.confirmation, save);

        tasks.busy().observe(getViewLifecycleOwner(), busy -> {
            Screens.busy(binding.bar, busy);
            binding.save.setEnabled(!busy);
        });
        tasks.outcome().observe(getViewLifecycleOwner(), outcome -> {
            if (outcome == null) {
                return;
            }
            tasks.consumed();
            if (outcome.result.isOk()) {
                Screens.toast(requireActivity(), getString(R.string.change_password_done));
                NavHostFragment.findNavController(this).navigateUp();
            } else if (outcome.result.problem == Problem.WRONG_PASSWORD) {
                Forms.check(binding.currentLayout, R.string.error_wrong_password);
            } else {
                Screens.message(this, R.string.error_unexpected);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
