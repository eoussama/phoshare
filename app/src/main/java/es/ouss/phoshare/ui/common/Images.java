package es.ouss.phoshare.ui.common;

import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;

import es.ouss.phoshare.R;
import es.ouss.phoshare.data.PhotoStore;

/** Loads stored photos into views, off the main thread and cached (Glide). Every file name is unique, so is the cache. */
public final class Images {

    private Images() {
    }

    public static void photo(ImageView view, PhotoStore store, String name) {
        Glide.with(view)
                .load(store.photo(name))
                .placeholder(R.drawable.bg_photo_placeholder)
                .centerCrop()
                .into(view);
    }

    public static void avatar(ImageView view, PhotoStore store, @Nullable String name) {
        if (name == null) {
            Glide.with(view).clear(view);
            view.setImageResource(R.drawable.ic_avatar_placeholder);
            return;
        }
        Glide.with(view)
                .load(store.avatar(name))
                .placeholder(R.drawable.ic_avatar_placeholder)
                .centerCrop()
                .into(view);
    }
}
