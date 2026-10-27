package es.ouss.phoshare.ui.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import es.ouss.phoshare.PhoShareApp;
import es.ouss.phoshare.R;
import es.ouss.phoshare.data.Problem;
import es.ouss.phoshare.data.Session;
import es.ouss.phoshare.databinding.DialogPasswordBinding;
import es.ouss.phoshare.databinding.FragmentProfileBinding;
import es.ouss.phoshare.ui.common.Forms;
import es.ouss.phoshare.ui.common.Insets;
import es.ouss.phoshare.ui.common.Screens;
import es.ouss.phoshare.ui.common.TaskViewModel;

/**
 * A profile and its photo grid. As the Profile tab it is your own, with the account's actions; opened from a post or a
 * comment it is anyone's, yours included.
 */
public class ProfileFragment extends Fragment {

    private static final int DELETE_ACCOUNT = 1;

    private FragmentProfileBinding binding;
    private TaskViewModel tasks;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        PhoShareApp app = PhoShareApp.from(requireContext());
        Session session = app.session();
        NavController navigation = NavHostFragment.findNavController(this);
        tasks = new ViewModelProvider(this).get(TaskViewModel.class);

        long requested = requireArguments().getLong("userId", Session.NOBODY);
        long userId = requested == Session.NOBODY ? session.userId() : requested;
        boolean own = userId == session.userId();
        boolean tab = requested == Session.NOBODY;

        Screens.appBar(this, binding.bar, R.string.nav_profile, !tab);
        Insets.sides(binding.list);
        if (own && tab) {
            binding.bar.toolbar.inflateMenu(R.menu.profile);
            binding.bar.toolbar.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.change_password) {
                    navigation.navigate(R.id.change_password);
                } else if (id == R.id.log_out) {
                    confirmLogOut();
                } else if (id == R.id.delete_account) {
                    confirmDeleteAccount(app, userId);
                }
                return true;
            });
        }

        ProfileHeaderAdapter header = new ProfileHeaderAdapter(app.photos(), own, () -> navigation.navigate(R.id.edit_profile));
        GridAdapter grid = new GridAdapter(app.photos(), post -> {
            Bundle arguments = new Bundle();
            arguments.putLong("postId", post.id);
            navigation.navigate(R.id.post, arguments);
        });

        int columns = getResources().getInteger(R.integer.grid_columns);
        GridLayoutManager layout = new GridLayoutManager(requireContext(), columns);
        layout.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return position < header.getItemCount() ? columns : 1;
            }
        });
        binding.list.setLayoutManager(layout);
        binding.list.setAdapter(new ConcatAdapter(header, grid));

        app.users().observe(userId).observe(getViewLifecycleOwner(), user -> {
            if (user == null) {
                // The account was deleted: nothing left to show.
                if (!tab) {
                    navigation.navigateUp();
                }
                return;
            }
            binding.bar.toolbar.setTitle(user.username);
            header.setUser(user);
        });
        app.posts().byAuthor(userId, session.userId()).observe(getViewLifecycleOwner(), posts -> {
            grid.submitList(posts);
            header.setPostCount(posts.size());
        });

        tasks.busy().observe(getViewLifecycleOwner(), busy -> Screens.busy(binding.bar, busy));
        tasks.outcome().observe(getViewLifecycleOwner(), outcome -> {
            if (outcome == null) {
                return;
            }
            tasks.consumed();
            if (outcome.result.isOk()) {
                Screens.toast(requireActivity(), getString(R.string.profile_account_deleted));
                Screens.logOut(requireActivity(), false);
            } else if (outcome.result.problem == Problem.WRONG_PASSWORD) {
                Screens.message(this, R.string.error_wrong_password);
            } else {
                Screens.message(this, R.string.error_unexpected);
            }
        });
    }

    private void confirmLogOut() {
        String username = String.valueOf(binding.bar.toolbar.getTitle());
        new MaterialAlertDialogBuilder(requireContext())
                .setIcon(R.drawable.ic_logout)
                .setTitle(R.string.profile_log_out)
                .setMessage(getString(R.string.profile_log_out_message, username))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.profile_log_out, (dialog, which) -> Screens.logOut(requireActivity(), false))
                .show();
    }

    private void confirmDeleteAccount(PhoShareApp app, long userId) {
        DialogPasswordBinding field = DialogPasswordBinding.inflate(getLayoutInflater());
        new MaterialAlertDialogBuilder(requireContext())
                .setIcon(R.drawable.ic_delete_forever)
                .setTitle(R.string.profile_delete_account)
                .setMessage(R.string.profile_delete_account_message)
                .setView(field.getRoot())
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.profile_delete_account_action, (dialog, which) -> {
                    String password = Forms.text(field.password);
                    if (password.isEmpty()) {
                        Screens.message(this, R.string.error_password_empty);
                        return;
                    }
                    tasks.<Void>run(DELETE_ACCOUNT, callback -> app.users().deleteAccount(userId, password, callback));
                })
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
