package es.ouss.phoshare.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;

import androidx.exifinterface.media.ExifInterface;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

@RunWith(AndroidJUnit4.class)
public class PhotoStoreTest {

    private Context context;
    private PhotoStore store;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        store = new PhotoStore(context);
    }

    private Uri image(String name, int width, int height, Bitmap.CompressFormat format) throws IOException {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(Color.rgb(251, 208, 0));
        File file = new File(context.getCacheDir(), name);
        try (OutputStream out = new FileOutputStream(file)) {
            bitmap.compress(format, 90, out);
        }
        bitmap.recycle();
        return Uri.fromFile(file);
    }

    private BitmapFactory.Options bounds(File file) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getPath(), options);
        return options;
    }

    @Test
    public void scalesLargePhotosDown() throws Exception {
        String name = store.importPhoto(image("large.jpg", 4096, 1024, Bitmap.CompressFormat.JPEG));

        BitmapFactory.Options saved = bounds(store.photo(name));
        assertEquals(PhotoStore.PHOTO_MAX_SIZE, saved.outWidth);
        assertEquals(PhotoStore.PHOTO_MAX_SIZE / 4, saved.outHeight);
        assertEquals("image/jpeg", saved.outMimeType);
    }

    @Test
    public void keepsSmallPhotosAsTheyAre() throws Exception {
        String name = store.importAvatar(image("small.png", 300, 200, Bitmap.CompressFormat.PNG));

        BitmapFactory.Options saved = bounds(store.avatar(name));
        assertEquals(300, saved.outWidth);
        assertEquals(200, saved.outHeight);
        assertEquals("image/jpeg", saved.outMimeType);
    }

    @Test
    public void turnsPhotosUpright() throws Exception {
        Uri sideways = image("sideways.jpg", 400, 100, Bitmap.CompressFormat.JPEG);
        ExifInterface exif = new ExifInterface(new File(sideways.getPath()));
        exif.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(ExifInterface.ORIENTATION_ROTATE_90));
        exif.saveAttributes();

        String name = store.importPhoto(sideways);

        BitmapFactory.Options saved = bounds(store.photo(name));
        assertEquals(100, saved.outWidth);
        assertEquals(400, saved.outHeight);
    }

    @Test(expected = IOException.class)
    public void refusesWhatIsNotAnImage() throws Exception {
        File file = new File(context.getCacheDir(), "not-an-image.jpg");
        try (OutputStream out = new FileOutputStream(file)) {
            out.write("hello".getBytes());
        }
        store.importPhoto(Uri.fromFile(file));
    }

    @Test
    public void deletesPhotos() throws Exception {
        String name = store.importPhoto(image("gone.jpg", 10, 10, Bitmap.CompressFormat.JPEG));
        assertTrue(store.photo(name).exists());

        store.deletePhoto(name);

        assertFalse(store.photo(name).exists());
    }
}
