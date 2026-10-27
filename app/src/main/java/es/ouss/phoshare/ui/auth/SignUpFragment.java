package es.ouss.phoshare.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Arrays;

import es.ouss.phoshare.PhoShareApp;
import es.ouss.phoshare.R;
import es.ouss.phoshare.data.Problem;
import es.ouss.phoshare.data.Result;
import es.ouss.phoshare.data.db.User;
import es.ouss.phoshare.databinding.FragmentSignUpBinding;
import es.ouss.phoshare.security.Credentials;
import es.ouss.phoshare.ui.common.Forms;
import es.ouss.phoshare.ui.common.Insets;
import es.ouss.phoshare.ui.common.Screens;
import es.ouss.phoshare.ui.common.TaskViewModel;

public class SignUpFragment extends Fragment {

    private static final int SIGN_UP = 1;

    private FragmentSignUpBinding binding;
    private TaskViewModel tasks;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSignUpBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Screens.appBar(this, binding.bar, R.string.sign_up_title, true);
        Insets.bottom(binding.scroll);
        tasks = new ViewModelProvider(this).get(TaskViewModel.class);

        Forms.clearErrorsOnEdit(binding.usernameLayout, binding.passwordLayout, binding.confirmationLayout, binding.questionLayout, binding.answerLayout);
        Forms.onDone(binding.answer, () -> Forms.hideKeyboard(binding.answer));
        binding.termsLink.setOnClickListener(v -> new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.terms_title)
                .setMessage(R.string.terms_body)
                .setPositiveButton(R.string.action_ok, (dialog, which) -> binding.terms.setChecked(true))
                .show());
        binding.signUp.setOnClickListener(v -> signUp());

        tasks.busy().observe(getViewLifecycleOwner(), busy -> {
            Screens.busy(binding.bar, busy);
            binding.signUp.setEnabled(!busy);
        });
        tasks.outcome().observe(getViewLifecycleOwner(), outcome -> {
            if (outcome != null) {
                tasks.consumed();
                onSignedUp(outcome.as());
            }
        });
    }

    /** The picked question's position; the field keeps its text across a rotation, so the text is the source. */
    private int question() {
        return Arrays.asList(getResources().getStringArray(R.array.security_questions)).indexOf(Forms.text(binding.question));
    }

    private void signUp() {
        String username = Forms.text(binding.username);
        String password = Forms.text(binding.password);
        String answer = Forms.text(binding.answer);
        int question = question();

        boolean valid = Forms.check(binding.usernameLayout, Credentials.checkUsername(username));
        valid &= Forms.check(binding.passwordLayout, Credentials.checkPassword(password));
        valid &= Forms.check(binding.confirmationLayout, Credentials.checkConfirmation(password, Forms.text(binding.confirmation)));
        valid &= Forms.check(binding.questionLayout, question < 0 ? R.string.error_question_empty : 0);
        valid &= Forms.check(binding.answerLayout, Credentials.checkAnswer(answer));
        if (!valid) {
            return;
        }
        if (!binding.terms.isChecked()) {
            Screens.message(this, R.string.error_terms);
            return;
        }

        Forms.hideKeyboard(binding.signUp);
        PhoShareApp app = PhoShareApp.from(requireContext());
        tasks.<User>run(SIGN_UP, callback -> app.users().signUp(username, password, question, answer, callback));
    }

    private void onSignedUp(Result<User> result) {
        if (result.isOk() && result.value != null) {
            Screens.enterApp(requireActivity(), result.value.id, getString(R.string.sign_up_welcome, result.value.username));
        } else if (result.problem == Problem.USERNAME_TAKEN) {
            Forms.check(binding.usernameLayout, result.problem.message);
            binding.scroll.smoothScrollTo(0, 0);
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
