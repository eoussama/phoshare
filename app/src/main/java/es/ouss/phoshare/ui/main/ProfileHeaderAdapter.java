package es.ouss.phoshare.ui.main;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import es.ouss.phoshare.R;
import es.ouss.phoshare.data.PhotoStore;
import es.ouss.phoshare.data.db.User;
import es.ouss.phoshare.databinding.ItemProfileHeaderBinding;
import es.ouss.phoshare.ui.common.Images;
import es.ouss.phoshare.ui.common.Times;

/** The top of a profile: who it is, since when, and how many posts. Shown once the account is loaded. */
public class ProfileHeaderAdapter extends RecyclerView.Adapter<ProfileHeaderAdapter.Holder> {

    private final PhotoStore photos;
    private final boolean own;
    private final Runnable onEdit;
    private User user;
    private int posts;

    public ProfileHeaderAdapter(PhotoStore photos, boolean own, Runnable onEdit) {
        this.photos = photos;
        this.own = own;
        this.onEdit = onEdit;
    }

    public void setUser(User user) {
        boolean had = this.user != null;
        this.user = user;
        if (had) {
            notifyItemChanged(0);
        } else {
            notifyItemInserted(0);
        }
    }

    public void setPostCount(int posts) {
        this.posts = posts;
        if (user != null) {
            notifyItemChanged(0);
        }
    }

    @Override
    public int getItemCount() {
        return user == null ? 0 : 1;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemProfileHeaderBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        ItemProfileHeaderBinding binding = holder.binding;
        Context context = binding.getRoot().getContext();

        Images.avatar(binding.avatar, photos, user.avatar);
        binding.avatar.setContentDescription(context.getString(R.string.profile_avatar_description, user.username));
        binding.username.setText(user.username);
        binding.joined.setText(context.getString(R.string.profile_joined, Times.monthYear(user.joinedAt)));
        binding.bio.setText(user.bio);
        binding.bio.setVisibility(user.bio.isEmpty() ? View.GONE : View.VISIBLE);
        binding.postCount.setText(context.getResources().getQuantityString(R.plurals.profile_post_count, posts, posts));
        binding.noPosts.setVisibility(posts == 0 ? View.VISIBLE : View.GONE);
        binding.edit.setVisibility(own ? View.VISIBLE : View.GONE);
        binding.edit.setOnClickListener(v -> onEdit.run());
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ItemProfileHeaderBinding binding;

        Holder(ItemProfileHeaderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
