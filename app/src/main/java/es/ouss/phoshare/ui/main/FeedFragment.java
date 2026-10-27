package es.ouss.phoshare.ui.main;

import android.view.View;

import androidx.lifecycle.LiveData;

import java.util.List;

import es.ouss.phoshare.R;
import es.ouss.phoshare.data.db.PostItem;
import es.ouss.phoshare.databinding.ViewEmptyBinding;
import es.ouss.phoshare.ui.common.Screens;

/** Every post on this device, newest first. */
public class FeedFragment extends PostListFragment {

    @Override
    protected int title() {
        return R.string.app_name;
    }

    @Override
    protected LiveData<List<PostItem>> posts() {
        return app.posts().feed(viewerId);
    }

    @Override
    protected boolean offersNewPost() {
        return true;
    }

    @Override
    protected void describeEmpty(ViewEmptyBinding empty) {
        empty.emptyTitle.setText(R.string.feed_empty_title);
        empty.emptyMessage.setText(R.string.feed_empty_message);
        empty.emptyAction.setText(R.string.feed_empty_action);
        empty.emptyAction.setVisibility(View.VISIBLE);
        empty.emptyAction.setOnClickListener(v -> Screens.logOut(requireActivity(), true));
    }
}
