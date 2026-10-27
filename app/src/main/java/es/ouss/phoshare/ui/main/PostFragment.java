package es.ouss.phoshare.ui.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.ConcatAdapter;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Collections;

import es.ouss.phoshare.PhoShareApp;
import es.ouss.phoshare.R;
import es.ouss.phoshare.data.PostRepository;
import es.ouss.phoshare.data.db.CommentItem;
import es.ouss.phoshare.data.db.PostItem;
import es.ouss.phoshare.databinding.FragmentPostBinding;
import es.ouss.phoshare.ui.common.Forms;
import es.ouss.phoshare.ui.common.Insets;
import es.ouss.phoshare.ui.common.Screens;
import es.ouss.phoshare.ui.common.Share;
import es.ouss.phoshare.ui.common.TaskViewModel;

/** One post with its whole caption and its comments, and the box to add one. Its author can delete it. */
public class PostFragment extends Fragment implements PostAdapter.Listener, CommentAdapter.Listener {

    private static final int COMMENT = 1;
    private static final int DELETE = 2;

    private FragmentPostBinding binding;
    private TaskViewModel tasks;
    private PhoShareApp app;
    private NavController navigation;
    private long postId;
    private long viewerId;
    private boolean closed;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPostBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        app = PhoShareApp.from(requireContext());
        navigation = NavHostFragment.findNavController(this);
        tasks = new ViewModelProvider(this).get(TaskViewModel.class);
        postId = requireArguments().getLong("postId");
        viewerId = app.session().userId();
        closed = false;

        Screens.appBar(this, binding.bar, R.string.post_title, true);
        Insets.sides(binding.list);
        Insets.bottom(binding.composer);
        binding.bar.toolbar.inflateMenu(R.menu.post);
        MenuItem delete = binding.bar.toolbar.getMenu().findItem(R.id.delete);
        binding.bar.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.delete) {
                confirmDelete();
                return true;
            }
            return false;
        });

        PostAdapter post = new PostAdapter(app.photos(), this, true);
        LabelAdapter label = new LabelAdapter();
        CommentAdapter comments = new CommentAdapter(app.photos(), this);
        binding.list.setAdapter(new ConcatAdapter(post, label, comments));

        PostRepository posts = app.posts();
        posts.item(postId, viewerId).observe(getViewLifecycleOwner(), item -> {
            if (item == null) {
                // Gone. While our own deletion runs, its result closes the screen (with a message) instead.
                if (!tasks.isBusy()) {
                    close();
                }
                return;
            }
            post.submitList(Collections.singletonList(item));
            delete.setVisible(item.authorId == viewerId);
            label.setText(item.commentCount == 0
                    ? getString(R.string.comments_empty)
                    : getResources().getQuantityString(R.plurals.post_comment_count, item.commentCount, item.commentCount));
        });
        posts.comments(postId).observe(getViewLifecycleOwner(), comments::submitList);

        binding.send.setOnClickListener(v -> send());
        tasks.busy().observe(getViewLifecycleOwner(), busy -> {
            Screens.busy(binding.bar, busy);
            binding.send.setEnabled(!busy);
        });
        tasks.outcome().observe(getViewLifecycleOwner(), outcome -> {
            if (outcome == null) {
                return;
            }
            tasks.consumed();
            if (!outcome.result.isOk()) {
                Screens.message(this, R.string.error_unexpected);
            } else if (outcome.what == COMMENT) {
                binding.comment.setText(null);
                binding.list.post(() -> {
                    if (binding != null && binding.list.getAdapter() != null) {
                        binding.list.smoothScrollToPosition(binding.list.getAdapter().getItemCount() - 1);
                    }
                });
            } else if (outcome.what == DELETE) {
                Screens.toast(requireActivity(), getString(R.string.post_deleted));
                close();
            }
        });
    }

    private void send() {
        String text = Forms.text(binding.comment).trim();
        if (text.isEmpty()) {
            return;
        }
        tasks.<Void>run(COMMENT, callback -> app.posts().comment(postId, viewerId, text, callback));
    }

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(requireContext())
                .setIcon(R.drawable.ic_delete)
                .setTitle(R.string.post_delete)
                .setMessage(R.string.post_delete_message)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.post_delete, (dialog, which) ->
                        tasks.<Void>run(DELETE, callback -> app.posts().delete(postId, viewerId, callback)))
                .show();
    }

    /** Closes the screen, once: both the deletion's result and the post's disappearance ask for it. */
    private void close() {
        if (closed || binding == null) {
            return;
        }
        closed = true;
        navigation.navigateUp();
    }

    @Override
    public void onOpen(PostItem post) {
        Forms.showKeyboard(binding.comment);
    }

    @Override
    public void onAuthor(PostItem post) {
        openProfile(post.authorId);
    }

    @Override
    public void onFavorite(PostItem post, boolean favorite) {
        app.posts().setFavorite(viewerId, post.id, favorite);
    }

    @Override
    public void onShare(PostItem post) {
        Share.post(requireContext(), app.photos(), post);
    }

    @Override
    public void onAuthor(CommentItem comment) {
        openProfile(comment.authorId);
    }

    @Override
    public void onLongPress(CommentItem comment) {
        if (comment.authorId != viewerId) {
            return;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.comment_delete)
                .setMessage(comment.text)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.comment_delete, (dialog, which) -> app.posts().deleteComment(comment.id, viewerId))
                .show();
    }

    private void openProfile(long userId) {
        Bundle arguments = new Bundle();
        arguments.putLong("userId", userId);
        navigation.navigate(R.id.user_profile, arguments);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
