package es.ouss.phoshare.ui.main;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import es.ouss.phoshare.data.PhotoStore;
import es.ouss.phoshare.data.db.CommentItem;
import es.ouss.phoshare.databinding.ItemCommentBinding;
import es.ouss.phoshare.ui.common.Images;
import es.ouss.phoshare.ui.common.Times;

/** A post's comments. A tap opens the author's profile; a long press offers to delete your own. */
public class CommentAdapter extends ListAdapter<CommentItem, CommentAdapter.Holder> {

    public interface Listener {
        void onAuthor(CommentItem comment);

        void onLongPress(CommentItem comment);
    }

    private static final DiffUtil.ItemCallback<CommentItem> DIFF = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull CommentItem a, @NonNull CommentItem b) {
            return a.id == b.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull CommentItem a, @NonNull CommentItem b) {
            return a.sameContent(b);
        }
    };

    private final PhotoStore photos;
    private final Listener listener;

    public CommentAdapter(PhotoStore photos, Listener listener) {
        super(DIFF);
        this.photos = photos;
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemCommentBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        CommentItem comment = getItem(position);
        ItemCommentBinding binding = holder.binding;
        Images.avatar(binding.avatar, photos, comment.authorAvatar);
        binding.username.setText(comment.authorName);
        binding.time.setText(Times.ago(binding.getRoot().getContext(), comment.createdAt));
        binding.text.setText(comment.text);
        binding.getRoot().setOnClickListener(v -> listener.onAuthor(comment));
        binding.getRoot().setOnLongClickListener(v -> {
            listener.onLongPress(comment);
            return true;
        });
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ItemCommentBinding binding;

        Holder(ItemCommentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
