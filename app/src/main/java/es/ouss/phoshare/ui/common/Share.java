package es.ouss.phoshare.ui.common;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import es.ouss.phoshare.R;
import es.ouss.phoshare.data.PhotoStore;
import es.ouss.phoshare.data.db.PostItem;

/** The one way a photo leaves PhoShare: Android's share sheet, to whatever app the user picks. */
public final class Share {

    private Share() {
    }

    public static void post(Context context, PhotoStore photos, PostItem post) {
        Uri uri = photos.shareUri(post.photo);
        Intent send = new Intent(Intent.ACTION_SEND)
                .setType("image/jpeg")
                .putExtra(Intent.EXTRA_STREAM, uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (!post.caption.isEmpty()) {
            send.putExtra(Intent.EXTRA_TEXT, post.caption);
        }
        // The chooser and the app picked both need the read permission, which travels with the clip data.
        send.setClipData(ClipData.newRawUri(null, uri));
        context.startActivity(Intent.createChooser(send, context.getString(R.string.post_share_chooser)));
    }
}
