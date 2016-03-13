package corp.katet.evernote.image;

import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Map;

import corp.katet.evernote.R;
import corp.katet.evernote.util.Callback;

public class ImagePicker extends ImageRetriever {

    public ImagePicker(int size, String format, int imageViewId,
                       FragmentActivity context, Callback<Bitmap> callback) {
        super(size, format, imageViewId, context, callback);
    }

    protected Map.Entry<String, Bitmap> doInBackground(String... uri) {
        try {
            Bitmap resultImage = retrieveImage(Uri.parse(uri[0]));
            if (callback != null && resultImage != null) {
                callback.execute(resultImage);
            }
            return new AbstractMap.SimpleEntry<>(uri[0], resultImage);
        } catch (Exception e) {
            Toast.makeText(context, R.string.retrieve_image_error, Toast.LENGTH_LONG).show();
            return null;
        }
    }

    @Override
    public void loadBitmap(String imageKey) {
        final Bitmap bitmap = mBitmapCache.getBitmapFromMemCache(imageKey);
        if (bitmap != null) {
            mExpandedImageView.setImageBitmap(bitmap);
        } else {
            mExpandedImageView.setImageResource(R.drawable.ic_placeholder_elephant);
            // Download image in background
            if (getStatus() == Status.PENDING) {
                this.execute(imageKey);
            } else {
                new ImagePicker(size, format, mExpandedImageView.getId(), context, callback)
                        .execute(imageKey);
            }
        }
    }

    /**
     * Given a filesystem URI, retrieve and returns the full-size bitmap bounded to it
     */
    public Bitmap retrieveImage(Uri uri) throws IOException {
        return MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
    }
}
