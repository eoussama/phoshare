package es.ouss.phoshare.ui.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import es.ouss.phoshare.PhoShareApp;
import es.ouss.phoshare.R;
import es.ouss.phoshare.data.db.PostItem;
import es.ouss.phoshare.databinding.FragmentPostsBinding;
import es.ouss.phoshare.databinding.ViewEmptyBinding;
import es.ouss.phoshare.ui.common.Insets;
import es.ouss.phoshare.ui.common.Screens;
import es.ouss.phoshare.ui.common.Share;

/** A tab that lists post cards: the feed and the favorites differ in which posts, and in what the empty list says. */
public abstract class PostListFragment extends Fragment implements PostAdapter.Listener {

    protected PhoShareApp app;
    protected long viewerId;
    private FragmentPostsBinding binding;

    @StringRes
    protected abstract int title();

    protected abstract LiveData<List<PostItem>> posts();

    /** Fills the empty state's title, message and (optionally) action. */
    protected abstract void describeEmpty(ViewEmptyBinding empty);

    protected boolean offersNewPost() {
        return false;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPostsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        app = PhoShareApp.from(requireContext());
        viewerId = app.session().userId();

        Screens.appBar(this, binding.bar, title(), false);
        Insets.sides(binding.list);
        describeEmpty(binding.empty);

        PostAdapter adapter = new PostAdapter(app.photos(), this, false);
        binding.list.setAdapter(adapter);
        posts().observe(getViewLifecycleOwner(), posts -> {
            adapter.submitList(posts);
            binding.empty.getRoot().setVisibility(posts.isEmpty() ? View.VISIBLE : View.GONE);
        });

        if (offersNewPost()) {
            binding.newPost.setVisibility(View.VISIBLE);
            binding.newPost.setOnClickListener(v -> NavHostFragment.findNavController(this).navigate(R.id.new_post));
            binding.list.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView list, int dx, int dy) {
                    if (dy > 0) {
                        binding.newPost.shrink();
                    } else if (dy < 0) {
                        binding.newPost.extend();
                    }
                }
            });
        }
    }

    @Override
    public void onOpen(PostItem post) {
        Bundle arguments = new Bundle();
        arguments.putLong("postId", post.id);
        NavHostFragment.findNavController(this).navigate(R.id.post, arguments);
    }

    @Override
    public void onAuthor(PostItem post) {
        Bundle arguments = new Bundle();
        arguments.putLong("userId", post.authorId);
        NavHostFragment.findNavController(this).navigate(R.id.user_profile, arguments);
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
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
