package corp.katet.evernote.image;

import android.graphics.Bitmap;
import android.os.AsyncTask;

import com.evernote.client.android.asyncclient.EvernoteUserStoreClient;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import corp.katet.evernote.util.Callback;
import corp.katet.evernote.util.EvernoteSnippets;

/**
 * Uses AsyncTask to create a task away from the main UI thread. This task takes an
 * URL string and uses it to create an HttpUrlConnection. Once the connection
 * has been established, the AsyncTask downloads the images as an InputStream.
 * Finally, the InputStream is converted into an array of bitmaps, whose contents are
 * displayed in the UI by the AsyncTask's onPostExecute method.
 */
public class ImageThumbnailDownloader
        extends AsyncTask<String, Map.Entry<String, Bitmap>, List<Map.Entry<String, Bitmap>>> {

    protected int size;
    protected String format;
    protected boolean incrementalResult;
    private List<Map.Entry<String, Bitmap>> resultImages;
    private Callback<Map.Entry<String, Bitmap>> callback;

    public ImageThumbnailDownloader(int size, String format, boolean incrementalResult,
                                    Callback<Map.Entry<String, Bitmap>> callback) {
        this.size = size;
        this.format = format;
        this.incrementalResult = incrementalResult;
        resultImages = new ArrayList<>();
        this.callback = callback;
    }

    @Override
    protected List<Map.Entry<String, Bitmap>> doInBackground(String... guid) {
        EvernoteUserStoreClient userStoreClient = EvernoteSnippets.createUserStoreClient();

        // Build the base URL for every request. GUIDs bounded to each image resource
        // will be individually appended and managed afterwards.
        StringBuilder url = new StringBuilder();
        try {
            url.append(userStoreClient.getPublicUserInfo(
                    userStoreClient.getUser().getUsername()).getWebApiUrlPrefix());
            url.append("thm");
            url.append("/res/");
        } catch (Exception e) {
            return null;
        }

        // Guids come from the execute() call parameters
        String urlPrefix = url.toString();

        AbstractMap.SimpleImmutableEntry<String, Bitmap> downloadedImage;
        for (String resGuid : guid) {
            url = new StringBuilder(urlPrefix);
            url.append(resGuid);
            url.append(".").append(format);
            url.append("?size=").append(size);
            downloadedImage = new AbstractMap.SimpleImmutableEntry<>
                    (resGuid, ImageDownloader.downloadImage(url.toString()));
            if (downloadedImage.getValue() != null) {
                resultImages.add(downloadedImage);
                publishProgress(downloadedImage);
            }
        }

        return resultImages;
    }

    /**
     * onProgressUpdate displays the results of the AsyncTask as they are generated
     */
    @Override
    protected void onProgressUpdate(Map.Entry<String, Bitmap>... processedItems) {
        if (!isCancelled() && incrementalResult) {
            callback.execute(processedItems);
        }
    }

    @Override
    protected void onPostExecute(List<Map.Entry<String, Bitmap>> processedItems) {
        if (!isCancelled() && !incrementalResult) {
            for (Map.Entry<String, Bitmap> processedEntry : processedItems) {
                callback.execute(processedEntry);
            }
        }
    }
}
