package es.ouss.phoshare.ui.main;

import android.content.ActivityNotFoundException;
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
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;

import java.io.IOException;

import es.ouss.phoshare.PhoShareApp;
import es.ouss.phoshare.R;
import es.ouss.phoshare.data.Problem;
import es.ouss.phoshare.databinding.FragmentNewPostBinding;
import es.ouss.phoshare.ui.common.Forms;
import es.ouss.phoshare.ui.common.Insets;
import es.ouss.phoshare.ui.common.Screens;
import es.ouss.phoshare.ui.common.TaskViewModel;

/**
 * Posting: a photo from the system photo picker (no storage permission needed) or from the camera app (which writes
 * into a file of ours, so no camera permission either), a caption, and publish.
 */
public class NewPostFragment extends Fragment {

    private static final int PUBLISH = 1;

    /** The chosen photo, and the file a camera shot is being written to, kept across rotations. */
    public static class NewPostViewModel extends TaskViewModel {
        final MutableLiveData<Uri> photo = new MutableLiveData<>();
        Uri pendingShot;
    }

    private FragmentNewPostBinding binding;
    private NewPostViewModel state;

    private final ActivityResultLauncher<PickVisualMediaRequest> pick = registerForActivityResult(
            new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    state.photo.setValue(uri);
                }
            });

    private final ActivityResultLauncher<Uri> shoot = registerForActivityResult(
            new ActivityResultContracts.TakePicture(), taken -> {
                if (Boolean.TRUE.equals(taken) && state.pendingShot != null) {
                    state.photo.setValue(state.pendingShot);
                }
            });

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        state = new ViewModelProvider(this).get(NewPostViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentNewPostBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        PhoShareApp app = PhoShareApp.from(requireContext());
        Screens.appBar(this, binding.bar, R.string.new_post_title, true);
        Insets.bottom(binding.scroll);

        binding.gallery.setOnClickListener(v -> pick.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build()));
        binding.camera.setOnClickListener(v -> {
            try {
                state.pendingShot = app.photos().newCameraUri();
                shoot.launch(state.pendingShot);
            } catch (IOException | ActivityNotFoundException e) {
                Screens.message(this, R.string.new_post_no_camera);
            }
        });

        state.photo.observe(getViewLifecycleOwner(), uri -> {
            binding.pickHint.setVisibility(uri == null ? View.VISIBLE : View.GONE);
            if (uri == null) {
                Glide.with(this).clear(binding.preview);
            } else {
                Glide.with(this).load(uri).centerCrop().into(binding.preview);
            }
        });

        binding.publish.setOnClickListener(v -> {
            Uri photo = state.photo.getValue();
            if (photo == null) {
                Screens.message(this, R.string.new_post_no_photo);
                return;
            }
            String caption = Forms.text(binding.caption);
            Forms.hideKeyboard(binding.publish);
            long me = app.session().userId();
            state.<Long>run(PUBLISH, callback -> app.posts().publish(me, photo, caption, callback));
        });

        state.busy().observe(getViewLifecycleOwner(), busy -> {
            Screens.busy(binding.bar, busy);
            binding.publish.setEnabled(!busy);
            binding.gallery.setEnabled(!busy);
            binding.camera.setEnabled(!busy);
        });
        state.outcome().observe(getViewLifecycleOwner(), outcome -> {
            if (outcome == null) {
                return;
            }
            state.consumed();
            if (outcome.result.isOk()) {
                Screens.toast(requireActivity(), getString(R.string.new_post_published));
                NavHostFragment.findNavController(this).navigateUp();
            } else {
                Screens.message(this, outcome.result.problem == Problem.PHOTO ? R.string.error_photo : R.string.error_unexpected);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
