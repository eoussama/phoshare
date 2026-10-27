package es.ouss.phoshare.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import es.ouss.phoshare.PhoShareApp;
import es.ouss.phoshare.R;
import es.ouss.phoshare.data.Problem;
import es.ouss.phoshare.data.Result;
import es.ouss.phoshare.data.UserRepository;
import es.ouss.phoshare.databinding.FragmentRecoverBinding;
import es.ouss.phoshare.security.Credentials;
import es.ouss.phoshare.ui.common.Forms;
import es.ouss.phoshare.ui.common.Insets;
import es.ouss.phoshare.ui.common.Screens;
import es.ouss.phoshare.ui.common.TaskViewModel;

/**
 * The password reset, in three steps: the account, its security question, the new password. Back goes back a step.
 * The answer is checked once to move on, and again with the reset itself.
 */
public class RecoverFragment extends Fragment {

    private static final int STEP_USERNAME = 0;
    private static final int STEP_ANSWER = 1;
    private static final int STEP_PASSWORD = 2;

    private static final int FIND = 1;
    private static final int CHECK = 2;
    private static final int RESET = 3;

    /** The reset's progress, kept across rotations. */
    public static class RecoverViewModel extends TaskViewModel {
        int step = STEP_USERNAME;
        String username = "";
        String answer = "";
        int question = -1;
    }

    private FragmentRecoverBinding binding;
    private RecoverViewModel state;
    private OnBackPressedCallback back;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentRecoverBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Screens.appBar(this, binding.bar, R.string.recover_title, true);
        Insets.bottom(binding.scroll);
        state = new ViewModelProvider(this).get(RecoverViewModel.class);
        UserRepository users = PhoShareApp.from(requireContext()).users();

        if (savedInstanceState == null && state.step == STEP_USERNAME) {
            binding.username.setText(requireArguments().getString("username", ""));
        }

        back = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                show(state.step - 1);
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), back);

        Forms.clearErrorsOnEdit(binding.usernameLayout, binding.answerLayout, binding.passwordLayout, binding.confirmationLayout);

        Runnable find = () -> {
            String username = Forms.text(binding.username);
            if (Forms.check(binding.usernameLayout, username.trim().isEmpty() ? R.string.error_username_empty : 0)) {
                state.<Integer>run(FIND, callback -> users.securityQuestion(username, callback));
            }
        };
        binding.usernameNext.setOnClickListener(v -> find.run());
        Forms.onDone(binding.username, find);

        Runnable check = () -> {
            String answer = Forms.text(binding.answer);
            if (Forms.check(binding.answerLayout, Credentials.checkAnswer(answer))) {
                state.<Void>run(CHECK, callback -> users.checkAnswer(state.username, answer, callback));
            }
        };
        binding.answerNext.setOnClickListener(v -> check.run());
        Forms.onDone(binding.answer, check);

        Runnable reset = () -> {
            String password = Forms.text(binding.password);
            boolean valid = Forms.check(binding.passwordLayout, Credentials.checkPassword(password));
            valid &= Forms.check(binding.confirmationLayout, Credentials.checkConfirmation(password, Forms.text(binding.confirmation)));
            if (valid) {
                Forms.hideKeyboard(binding.reset);
                state.<Void>run(RESET, callback -> users.resetPassword(state.username, state.answer, password, callback));
            }
        };
        binding.reset.setOnClickListener(v -> reset.run());
        Forms.onDone(binding.confirmation, reset);

        state.busy().observe(getViewLifecycleOwner(), busy -> {
            Screens.busy(binding.bar, busy);
            binding.usernameNext.setEnabled(!busy);
            binding.answerNext.setEnabled(!busy);
            binding.reset.setEnabled(!busy);
        });
        state.outcome().observe(getViewLifecycleOwner(), outcome -> {
            if (outcome != null) {
                state.consumed();
                onOutcome(outcome);
            }
        });

        show(state.step);
    }

    private void onOutcome(TaskViewModel.Outcome outcome) {
        Result<?> result = outcome.result;
        if (!result.isOk()) {
            if (result.problem == Problem.UNKNOWN_USER) {
                show(STEP_USERNAME);
                Forms.check(binding.usernameLayout, result.problem.message);
            } else if (result.problem == Problem.WRONG_ANSWER) {
                show(STEP_ANSWER);
                Forms.check(binding.answerLayout, result.problem.message);
            } else {
                Screens.message(this, R.string.error_unexpected);
            }
            return;
        }

        switch (outcome.what) {
            case FIND:
                Result<Integer> found = outcome.as();
                state.username = Credentials.normalizeUsername(Forms.text(binding.username));
                state.question = found.value == null ? 0 : found.value;
                show(STEP_ANSWER);
                break;
            case CHECK:
                state.answer = Forms.text(binding.answer);
                show(STEP_PASSWORD);
                break;
            case RESET:
                Screens.toast(requireActivity(), getString(R.string.recover_done));
                NavHostFragment.findNavController(this).popBackStack();
                break;
            default:
                break;
        }
    }

    private void show(int step) {
        state.step = step;
        back.setEnabled(step > STEP_USERNAME);
        binding.stepUsername.setVisibility(step == STEP_USERNAME ? View.VISIBLE : View.GONE);
        binding.stepAnswer.setVisibility(step == STEP_ANSWER ? View.VISIBLE : View.GONE);
        binding.stepPassword.setVisibility(step == STEP_PASSWORD ? View.VISIBLE : View.GONE);

        if (step == STEP_USERNAME) {
            binding.step.setText(R.string.recover_step_username);
        } else if (step == STEP_ANSWER) {
            binding.step.setText(R.string.recover_step_answer);
            String[] questions = getResources().getStringArray(R.array.security_questions);
            int question = state.question >= 0 && state.question < questions.length ? state.question : 0;
            binding.question.setText(questions[question]);
        } else {
            binding.step.setText(R.string.recover_step_password);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
