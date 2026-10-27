package es.ouss.phoshare.ui.main;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.function.Consumer;

import es.ouss.phoshare.R;
import es.ouss.phoshare.data.PhotoStore;
import es.ouss.phoshare.data.db.PostItem;
import es.ouss.phoshare.databinding.ItemGridPhotoBinding;
import es.ouss.phoshare.ui.common.Images;

/** A profile's posts as a grid of square photos. */
public class GridAdapter extends ListAdapter<PostItem, GridAdapter.Holder> {

    private static final DiffUtil.ItemCallback<PostItem> DIFF = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull PostItem a, @NonNull PostItem b) {
            return a.id == b.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull PostItem a, @NonNull PostItem b) {
            return a.photo.equals(b.photo);
        }
    };

    private final PhotoStore photos;
    private final Consumer<PostItem> onOpen;

    public GridAdapter(PhotoStore photos, Consumer<PostItem> onOpen) {
        super(DIFF);
        this.photos = photos;
        this.onOpen = onOpen;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemGridPhotoBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        PostItem post = getItem(position);
        Images.photo(holder.binding.photo, photos, post.photo);
        holder.binding.photo.setContentDescription(holder.binding.photo.getContext().getString(R.string.profile_photo_description, position + 1));
        holder.binding.photo.setOnClickListener(v -> onOpen.accept(post));
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ItemGridPhotoBinding binding;

        Holder(ItemGridPhotoBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
