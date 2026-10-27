package es.ouss.phoshare.data;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * The photos themselves, as JPEG files in the app's private storage (the database keeps their names). Imports read
 * whatever the photo picker or the camera hands over, turn it upright from its EXIF orientation, and scale it down so a
 * phone's 50 MP shot does not cost 20 MB. Photos are shared with other apps through a {@link FileProvider}.
 */
public final class PhotoStore {

    public static final int PHOTO_MAX_SIZE = 2048;
    public static final int AVATAR_MAX_SIZE = 640;

    private static final int JPEG_QUALITY = 90;

    private final Context context;
    private final File photos;
    private final File avatars;
    private final File camera;

    public PhotoStore(Context context) {
        this.context = context.getApplicationContext();
        photos = new File(this.context.getFilesDir(), "photos");
        avatars = new File(this.context.getFilesDir(), "avatars");
        camera = new File(this.context.getCacheDir(), "camera");
    }

    /** Copies a photo into the store; returns its new file name. Runs in the background. */
    public String importPhoto(Uri source) throws IOException {
        return importInto(photos, source, PHOTO_MAX_SIZE);
    }

    /** Copies an avatar into the store; returns its new file name. Runs in the background. */
    public String importAvatar(Uri source) throws IOException {
        return importInto(avatars, source, AVATAR_MAX_SIZE);
    }

    public File photo(String name) {
        return new File(photos, name);
    }

    public File avatar(String name) {
        return new File(avatars, name);
    }

    public void deletePhoto(@Nullable String name) {
        delete(photos, name);
    }

    public void deleteAvatar(@Nullable String name) {
        delete(avatars, name);
    }

    /** Where the camera app should save a new shot: an empty file in the cache, offered through the FileProvider. */
    public Uri newCameraUri() throws IOException {
        if (!camera.isDirectory() && !camera.mkdirs()) {
            throw new IOException("Cannot create " + camera);
        }
        File file = new File(camera, UUID.randomUUID() + ".jpg");
        if (!file.createNewFile()) {
            throw new IOException("Cannot create " + file);
        }
        return FileProvider.getUriForFile(context, authority(context), file);
    }

    /** Forgets the camera's shots once they are imported (or abandoned). */
    public void clearCamera() {
        File[] shots = camera.listFiles();
        if (shots != null) {
            for (File shot : shots) {
                //noinspection ResultOfMethodCallIgnored
                shot.delete();
            }
        }
    }

    /** A content URI another app can read the photo from, once given the permission to. */
    public Uri shareUri(String photo) {
        return FileProvider.getUriForFile(context, authority(context), photo(photo));
    }

    public static String authority(Context context) {
        return context.getPackageName() + ".files";
    }

    private String importInto(File folder, Uri source, int maxSize) throws IOException {
        if (!folder.isDirectory() && !folder.mkdirs()) {
            throw new IOException("Cannot create " + folder);
        }

        Bitmap bitmap = decode(source, maxSize);
        String name = UUID.randomUUID() + ".jpg";
        File target = new File(folder, name);
        File partial = new File(folder, name + ".part");

        try (OutputStream out = new FileOutputStream(partial)) {
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)) {
                throw new IOException("Cannot encode the photo");
            }
        } catch (IOException e) {
            //noinspection ResultOfMethodCallIgnored
            partial.delete();
            throw e;
        } finally {
            bitmap.recycle();
        }

        if (!partial.renameTo(target)) {
            //noinspection ResultOfMethodCallIgnored
            partial.delete();
            throw new IOException("Cannot save " + target);
        }
        return name;
    }

    /** Decodes the image upright, at most maxSize pixels on its longest side, on an opaque background. */
    private Bitmap decode(Uri source, int maxSize) throws IOException {
        ContentResolver resolver = context.getContentResolver();

        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream in = open(resolver, source)) {
            BitmapFactory.decodeStream(in, null, bounds);
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw new IOException("Not an image: " + source);
        }

        // Decode at the smallest power-of-two reduction that still leaves at least maxSize pixels to scale from.
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;
        while (Math.max(bounds.outWidth, bounds.outHeight) / (options.inSampleSize * 2) >= maxSize) {
            options.inSampleSize *= 2;
        }

        Bitmap decoded;
        try (InputStream in = open(resolver, source)) {
            decoded = BitmapFactory.decodeStream(in, null, options);
        }
        if (decoded == null) {
            throw new IOException("Cannot decode " + source);
        }

        int orientation;
        try (InputStream in = open(resolver, source)) {
            orientation = new ExifInterface(in).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        } catch (IOException | RuntimeException e) {
            // A photo without readable EXIF is taken as it comes.
            orientation = ExifInterface.ORIENTATION_NORMAL;
        }

        Matrix matrix = orientationMatrix(orientation);
        float scale = Math.min(1f, (float) maxSize / Math.max(decoded.getWidth(), decoded.getHeight()));
        matrix.postScale(scale, scale);

        Bitmap upright = Bitmap.createBitmap(decoded, 0, 0, decoded.getWidth(), decoded.getHeight(), matrix, true);
        if (upright != decoded) {
            decoded.recycle();
        }
        if (!upright.hasAlpha()) {
            return upright;
        }

        // JPEG has no transparency: a transparent PNG would turn black, so it goes on white instead.
        Bitmap opaque = Bitmap.createBitmap(upright.getWidth(), upright.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(opaque);
        canvas.drawColor(Color.WHITE);
        canvas.drawBitmap(upright, 0, 0, null);
        upright.recycle();
        return opaque;
    }

    private static Matrix orientationMatrix(int orientation) {
        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setScale(1, -1);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(-90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(-90);
                break;
            default:
                break;
        }
        return matrix;
    }

    private static InputStream open(ContentResolver resolver, Uri source) throws IOException {
        InputStream in = resolver.openInputStream(source);
        if (in == null) {
            throw new IOException("Cannot open " + source);
        }
        return in;
    }

    private static void delete(File folder, @Nullable String name) {
        if (name != null && !name.isEmpty()) {
            //noinspection ResultOfMethodCallIgnored
            new File(folder, name).delete();
        }
    }
}
