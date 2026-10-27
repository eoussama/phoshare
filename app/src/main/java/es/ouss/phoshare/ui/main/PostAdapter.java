package es.ouss.phoshare.ui.main;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;

import es.ouss.phoshare.R;
import es.ouss.phoshare.data.PhotoStore;
import es.ouss.phoshare.data.db.PostItem;
import es.ouss.phoshare.databinding.ItemPostBinding;
import es.ouss.phoshare.ui.common.Images;
import es.ouss.phoshare.ui.common.Times;

/** Post cards, for the feed and the favorites, and (expanded, one card) at the top of a post's screen. */
public class PostAdapter extends ListAdapter<PostItem, PostAdapter.Holder> {

    public interface Listener {
        void onOpen(PostItem post);

        void onAuthor(PostItem post);

        void onFavorite(PostItem post, boolean favorite);

        void onShare(PostItem post);
    }

    private static final DiffUtil.ItemCallback<PostItem> DIFF = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull PostItem a, @NonNull PostItem b) {
            return a.id == b.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull PostItem a, @NonNull PostItem b) {
            return a.sameContent(b);
        }
    };

    private final PhotoStore photos;
    private final Listener listener;
    private final boolean expanded;

    /** @param expanded show the whole caption, and let the card itself not open anything (it is open already) */
    public PostAdapter(PhotoStore photos, Listener listener, boolean expanded) {
        super(DIFF);
        this.photos = photos;
        this.listener = listener;
        this.expanded = expanded;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemPostBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(getItem(position));
    }

    final class Holder extends RecyclerView.ViewHolder {
        private final ItemPostBinding binding;

        Holder(ItemPostBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(PostItem post) {
            Context context = binding.getRoot().getContext();

            Images.avatar(binding.avatar, photos, post.authorAvatar);
            binding.username.setText(post.authorName);
            binding.time.setText(Times.ago(context, post.createdAt));
            binding.author.setContentDescription(context.getString(R.string.post_open_author, post.authorName));
            binding.author.setOnClickListener(v -> listener.onAuthor(post));

            Images.photo(binding.photo, photos, post.photo);
            binding.photo.setContentDescription(context.getString(R.string.post_photo_description, post.authorName));

            binding.favorite.setText(String.valueOf(post.favoriteCount));
            binding.favorite.setIconResource(post.favorite ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
            binding.favorite.setIconTint(post.favorite
                    ? ColorStateList.valueOf(ContextCompat.getColor(context, R.color.brand_red))
                    : ColorStateList.valueOf(MaterialColors.getColor(binding.favorite, com.google.android.material.R.attr.colorOnSurface)));
            binding.favorite.setContentDescription(context.getString(post.favorite ? R.string.post_unfavorite : R.string.post_favorite)
                    + ", " + context.getResources().getQuantityString(R.plurals.post_favorite_count, post.favoriteCount, post.favoriteCount));
            binding.favorite.setOnClickListener(v -> listener.onFavorite(post, !post.favorite));

            binding.comments.setText(String.valueOf(post.commentCount));
            binding.comments.setContentDescription(context.getResources().getQuantityString(R.plurals.post_comment_count, post.commentCount, post.commentCount));
            binding.comments.setOnClickListener(v -> listener.onOpen(post));

            binding.share.setOnClickListener(v -> listener.onShare(post));

            binding.caption.setText(post.caption);
            binding.caption.setVisibility(post.caption.isEmpty() ? View.GONE : View.VISIBLE);
            binding.caption.setMaxLines(expanded ? Integer.MAX_VALUE : 3);

            if (expanded) {
                binding.getRoot().setOnClickListener(null);
                binding.getRoot().setClickable(false);
            } else {
                binding.getRoot().setOnClickListener(v -> listener.onOpen(post));
            }
        }
    }
}
