package corp.katet.evernote.image;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.FragmentActivity;

import com.evernote.client.android.EvernoteSession;
import com.evernote.client.android.asyncclient.EvernoteUserStoreClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import corp.katet.evernote.R;
import corp.katet.evernote.util.Callback;
import corp.katet.evernote.util.EvernoteSnippets;

/**
 * Uses AsyncTask to create a task away from the main UI thread. This task takes an
 * URL string and uses it to create an HttpUrlConnection. Once the connection
 * has been established, the AsyncTask downloads the images as an InputStream.
 * Finally, the InputStream is converted into an array of bitmaps, whose contents are
 * displayed in the UI by the AsyncTask's onPostExecute method.
 */
public class ImageDownloader extends ImageRetriever {

    public ImageDownloader(int size, String format, int imageViewId, FragmentActivity context,
                           Callback<Bitmap> callback) {
        super(size, format, imageViewId, context, callback);
    }

    @Override
    protected Map.Entry<String, Bitmap> doInBackground(String... guid) {
        EvernoteUserStoreClient userStoreClient = EvernoteSnippets.createUserStoreClient();

        // Build the base URL for the request, using the GUID bounded to the image resource
        StringBuilder url = new StringBuilder();
        try {
            url.append(userStoreClient.getPublicUserInfo(
                    userStoreClient.getUser().getUsername()).getWebApiUrlPrefix());
            url.append("thm");
            url.append("/res/");
        } catch (Exception e) {
            return null;
        }

        String resGuid = guid[0];

        url.append(resGuid);
        url.append(".").append(format);
        url.append("?size=").append(size);

        Bitmap resultImage = downloadImage(url.toString());
        if (callback != null && resultImage != null) {
            callback.execute(resultImage);
        }
        return new AbstractMap.SimpleImmutableEntry<>(resGuid, resultImage);
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
                new ImageDownloader(size, format, mExpandedImageView.getId(), context, callback)
                        .execute(imageKey);
            }
        }
    }

    /**
     * Given an URL, establishes an HttpUrlConnection and retrieves
     * the image content as an InputStream, which it returns as
     * a bitmap.
     */
    public static Bitmap downloadImage(String urlString) {
        InputStream is = null;
        Bitmap image;

        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(15000 /*milliseconds */);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            OutputStream os = conn.getOutputStream();
            os.write(getAuthData().getBytes("UTF-8"));
            os.flush();
            os.close();

            // Starts the query
            conn.connect();
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                is = conn.getInputStream();

                // Decode the InputStream into a bitmap
                image = BitmapFactory.decodeStream(is);
            } else {
                throw new IOException("Connection response status code NOT OK: "
                        + conn.getResponseCode());
            }
        } catch (IOException ioe) {
            image = null;
        } finally {
            // Make sure the InputStream is closed after the app is finished using it.
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException ioe2) {
            }
        }

        return image;
    }

    public static String getAuthData() {
        HashMap<String, String> paramsMap = new HashMap<>();
        paramsMap.put("auth", EvernoteSession.getInstance().getAuthToken());
        StringBuilder paramsString = new StringBuilder();
        Iterator<Map.Entry<String, String>> paramsIt = paramsMap.entrySet().iterator();
        Map.Entry<String, String> paramsEntry;
        while (paramsIt.hasNext()) {
            paramsEntry = paramsIt.next();
            paramsString.append(paramsEntry.getKey())
                    .append("=").append(paramsEntry.getValue());
            if (paramsIt.hasNext()) {
                paramsString.append("&");
            }
        }
        return paramsString.toString();
    }

}
