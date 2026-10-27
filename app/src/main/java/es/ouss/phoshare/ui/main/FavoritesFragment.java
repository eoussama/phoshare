package es.ouss.phoshare.ui.main;

import androidx.lifecycle.LiveData;

import java.util.List;

import es.ouss.phoshare.R;
import es.ouss.phoshare.data.db.PostItem;
import es.ouss.phoshare.databinding.ViewEmptyBinding;

/** The posts you favorited, the latest favorite first. */
public class FavoritesFragment extends PostListFragment {

    @Override
    protected int title() {
        return R.string.nav_favorites;
    }

    @Override
    protected LiveData<List<PostItem>> posts() {
        return app.posts().favorites(viewerId);
    }

    @Override
    protected void describeEmpty(ViewEmptyBinding empty) {
        empty.emptyTitle.setText(R.string.favorites_empty_title);
        empty.emptyMessage.setText(R.string.favorites_empty_message);
    }
}
