package es.ouss.phoshare.ui.main;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;

import es.ouss.phoshare.PhoShareApp;
import es.ouss.phoshare.R;
import es.ouss.phoshare.data.Problem;
import es.ouss.phoshare.data.db.User;
import es.ouss.phoshare.databinding.FragmentEditProfileBinding;
import es.ouss.phoshare.ui.common.Forms;
import es.ouss.phoshare.ui.common.Images;
import es.ouss.phoshare.ui.common.Insets;
import es.ouss.phoshare.ui.common.Screens;
import es.ouss.phoshare.ui.common.TaskViewModel;

/** Your avatar and bio. Nothing changes until Save. */
public class EditProfileFragment extends Fragment {

    private static final int SAVE = 1;

    /** The edits so far, kept across rotations. */
    public static class EditProfileViewModel extends TaskViewModel {
        Uri newAvatar;
        boolean removeAvatar;
        boolean filled;
    }

    private FragmentEditProfileBinding binding;
    private EditProfileViewModel state;
    private PhoShareApp app;
    private User user;

    private final ActivityResultLauncher<PickVisualMediaRequest> pick = registerForActivityResult(
            new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    state.newAvatar = uri;
                    state.removeAvatar = false;
                    showAvatar();
                }
            });

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        state = new ViewModelProvider(this).get(EditProfileViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentEditProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        app = PhoShareApp.from(requireContext());
        long me = app.session().userId();
        Screens.appBar(this, binding.bar, R.string.profile_edit, true);
        Insets.bottom(binding.scroll);

        app.users().observe(me).observe(getViewLifecycleOwner(), loaded -> {
            if (loaded == null) {
                return;
            }
            user = loaded;
            binding.avatar.setContentDescription(getString(R.string.profile_avatar_description, loaded.username));
            if (!state.filled) {
                state.filled = true;
                binding.bio.setText(loaded.bio);
            }
            showAvatar();
        });

        binding.changeAvatar.setOnClickListener(v -> pick.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build()));
        binding.removeAvatar.setOnClickListener(v -> {
            state.newAvatar = null;
            state.removeAvatar = true;
            showAvatar();
        });
        binding.save.setOnClickListener(v -> {
            String bio = Forms.text(binding.bio);
            Forms.hideKeyboard(binding.save);
            state.<Void>run(SAVE, callback -> app.users().updateProfile(me, bio, state.newAvatar, state.removeAvatar, callback));
        });

        state.busy().observe(getViewLifecycleOwner(), busy -> {
            Screens.busy(binding.bar, busy);
            binding.save.setEnabled(!busy);
        });
        state.outcome().observe(getViewLifecycleOwner(), outcome -> {
            if (outcome == null) {
                return;
            }
            state.consumed();
            if (outcome.result.isOk()) {
                Screens.toast(requireActivity(), getString(R.string.edit_profile_saved));
                NavHostFragment.findNavController(this).navigateUp();
            } else {
                Screens.message(this, outcome.result.problem == Problem.PHOTO ? R.string.error_photo : R.string.error_unexpected);
            }
        });
    }

    /** The avatar as it will be after Save: the new pick, nothing, or the current one. */
    private void showAvatar() {
        if (binding == null) {
            return;
        }
        if (state.newAvatar != null) {
            Glide.with(this).load(state.newAvatar).centerCrop().into(binding.avatar);
        } else {
            Images.avatar(binding.avatar, app.photos(), state.removeAvatar || user == null ? null : user.avatar);
        }
        binding.removeAvatar.setEnabled(state.newAvatar != null || (!state.removeAvatar && user != null && user.avatar != null));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
