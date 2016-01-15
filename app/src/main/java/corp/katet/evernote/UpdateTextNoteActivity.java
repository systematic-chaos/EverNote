package corp.katet.evernote;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.KeyListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.evernote.client.android.EvernoteSession;
import com.evernote.client.android.EvernoteUtil;
import com.evernote.client.android.asyncclient.EvernoteCallback;
import com.evernote.client.android.asyncclient.EvernoteUserStoreClient;
import com.evernote.edam.limits.Constants;
import com.evernote.edam.type.Note;
import com.evernote.edam.type.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class UpdateTextNoteActivity extends TextNoteActivity {

    public static final String EXTRA_NOTE_GUID = "EXTRA_NOTE_GUID";

    private KeyListener mDefaultKeyListener;
    private boolean mIsEditModeActive;
    private Note mFullNote;
    private ImageAdapter mImageAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDefaultKeyListener = mNotePaneEditText.getKeyListener();
        mIsEditModeActive = true;

        // Setup the image resources list view
        ((ListView) findViewById(R.id.reco_thumbnail_list_view)).setAdapter(mImageAdapter
                = new ImageAdapter(UpdateTextNoteActivity.this, R.id.resource_thumbnail));

        findViewById(R.id.note_fab).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                changeAccessMode(v);
            }
        });

        EvernoteSnippets.createNoteStoreClient()
                .getNoteAsync(getIntent().getStringExtra(EXTRA_NOTE_GUID),
                        true, false, true, false, new OpenNoteCallback());
    }

    public void changeAccessMode(View v) {
        if (mIsEditModeActive = !mIsEditModeActive) {
            ((ImageView) v).setImageResource(R.drawable.ic_save);
            mTitleEditText.setVisibility(View.VISIBLE);
            mNotePaneEditText.setFocusableInTouchMode(true);
            mNotePaneEditText.setKeyListener(mDefaultKeyListener);
            mNotePaneEditText.setFocusable(true);
        } else {
            if (saveChanges()) {
                ((ImageView) v).setImageResource(R.drawable.ic_edit);
                mTitleEditText.clearFocus();
                mTitleEditText.setVisibility(View.GONE);
                mNotePaneEditText.clearFocus();
                mNotePaneEditText.setFocusable(false);
                mNotePaneEditText.setFocusableInTouchMode(false);
                mNotePaneEditText.setKeyListener(null);
                getSupportActionBar().setTitle(mTitleEditText.getText().toString());
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (mIsEditModeActive) {
            super.onBackPressed();
        } else {
            finish();
        }
    }

    @Override
    protected boolean saveChanges() {
        mFullNote.setTitle(mTitleEditText.getText().toString());
        mFullNote.setContent(EvernoteUtil.NOTE_PREFIX + mNotePaneEditText.getText().toString()
                + EvernoteUtil.NOTE_SUFFIX);
        EvernoteSnippets.createNoteStoreClient().updateNoteAsync(mFullNote,
                new EvernoteCallback<Note>() {
            @Override
            public void onSuccess(Note note) {
                mFullNote = note;
            }

            @Override
            public void onException(Exception e) {
                Toast.makeText(UpdateTextNoteActivity.this,
                        R.string.save_note_error + ": " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
        return true;
    }

    private class ImageRecognitionTask extends AsyncTask<Note, Void, List<String>> {

        private Callback<String> callback;

        public ImageRecognitionTask(Callback<String> callback) {
            this.callback = callback;
        }

        @Override
        protected List<String> doInBackground(Note... notes) {
            List<String> result = null;
            EvernoteXmlParser parser = EvernoteXmlParser.newInstance();
            try {
                result = parser.recognizeResourcesData(notes[0], UpdateTextNoteActivity.this);
            } catch (final Exception e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(UpdateTextNoteActivity.this, e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
            }
            return result;
        }

        @Override
        protected void onPostExecute(List<String> result) {
            callback.execute(result.toArray(new String[0]));
        }
    }

    private class OpenNoteCallback implements EvernoteCallback<Note> {

        @Override
        public void onSuccess(Note note) {
            mFullNote = note;
            if (note.isSetTitle()) {
                mTitleEditText.setText(note.getTitle());
            }
            // Crop the note html content (excluding the note's prefix and
            // suffix) before setting its plain contents in the text area
            String content = note.isSetContent() ? note.getContent().substring(
                    EvernoteUtil.NOTE_PREFIX.length(),
                    note.getContent().length()
                            - EvernoteUtil.NOTE_SUFFIX.length()) : "";

            // In case the note content is filled, set it in the text area
            // Otherwise, look for recognition data into the note resources
            if (content.trim().length() > 0) {
                mNotePaneEditText.setText(content);
                changeAccessMode(findViewById(R.id.note_fab));
            } else {
                new ImageRecognitionTask(new Callback<String>() {
                    @Override
                    public void execute(String... result) {
                        StringBuilder content = new StringBuilder();
                        for (String text : result) {
                            content.append(text).append("    ");
                        }
                        mNotePaneEditText.setText(content.toString());
                        changeAccessMode(findViewById(R.id.note_fab));
                    }
                }).execute(note);
            }

            retrieveImageResources();
        }

        @Override
        public void onException(Exception e) {
            Toast.makeText(UpdateTextNoteActivity.this, R.string.open_note_error + ": "
                    + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        private void retrieveImageResources() {
            List<Resource> noteResources = mFullNote.getResources();
            if (noteResources == null || noteResources.isEmpty())
                return;

            List<String> resourcesGuid = new ArrayList<>();
            for (Resource rsc : noteResources) {
                String format;
                switch (rsc.getMime()) {
                    case Constants.EDAM_MIME_TYPE_PNG:
                        format = "png";
                        break;
                    case Constants.EDAM_MIME_TYPE_JPEG:
                        format = "jpeg";
                        break;
                    case Constants.EDAM_MIME_TYPE_GIF:
                        format = "gif";
                        break;
                    default:
                        format = null;
                }
                if (format != null) {
                    resourcesGuid.add(rsc.getGuid());
                }
            }

            if (!resourcesGuid.isEmpty()) {
                new ImageResourceDownloader(true, 75, "png", true,
                        new Callback<Bitmap>() {
                            @Override
                            public void execute(Bitmap... result) {
                                mImageAdapter.addAll(result);
                            }
                        })
                        .execute(resourcesGuid.toArray(new String[0]));
            }
        }
    }

    /**
     * Uses AsyncTask to create a task away from the main UI thread. This task takes a
     * URL string and uses it to create an HttpUrlConnection. Once the connection
     * has been established, the AsyncTask downloads the images as an InputStream.
     * Finally, the InputStream is converted into an array of bitmaps, whose contents are
     * displayed in the UI by the AsyncTask's onPostExecute method.
     */
    private class ImageResourceDownloader extends AsyncTask<String, Bitmap, Bitmap[]> {

        private boolean thumbnail;
        private int size;
        private String format;
        private boolean incrementalResult;
        private Callback<Bitmap> callback;

        public ImageResourceDownloader(boolean thumbnail, int size, String format,
               boolean incrementalResult, Callback<Bitmap> callback) {
            this.thumbnail = thumbnail;
            this.size = size;
            this.format = format;
            this.incrementalResult = incrementalResult;
            this.callback = callback;
        }

        @Override
        public Bitmap[] doInBackground(String... guid) {
            EvernoteUserStoreClient userStoreClient = EvernoteSnippets.createUserStoreClient();
            // Build the base URL for every request. GUIDs bounded to each image resource
            // will be individually appended and managed afterwards.
            StringBuilder url = new StringBuilder();
            try {
                url.append(userStoreClient.getPublicUserInfo(
                        userStoreClient.getUser().getUsername()));
                if (thumbnail) {
                    url.append("/shard/");
                    url.append(userStoreClient.getUser().getShardId());
                    url.append("thm");
                }
                url.append("/res/");
            } catch (Exception e) {
                return null;
            }

            // Guids come from the execute() call parameters
            String urlPrefix = url.toString();
            Bitmap[] resultImages = new Bitmap[guid.length];
            String resGuid;

            for (int n = 0; n < guid.length; n++) {
                resGuid = guid[n];
                url = new StringBuilder(urlPrefix);
                url.append(resGuid);
                url.append(".").append(format);
                url.append("?size=").append(size);
                resultImages[n] = downloadImage(url.toString());
                if (resultImages[n] != null) {
                    publishProgress(resultImages[n]);
                }
            }

            return resultImages;
        }

        /**
         * onProgressUpdate displays the results of the AsyncTask as they are generated
         */
        @Override
        protected void onProgressUpdate(Bitmap... processedItems) {
            if (incrementalResult) {
                callback.execute(processedItems);
            }
        }

        /**
         * onPostExecute displays the results of the AsyncTask once they have been fully
         * processed and generated
         */
        @Override
        protected void onPostExecute(Bitmap[] result) {
            if (!incrementalResult) {
                callback.execute(result);
            }
        }

        /**
         * Given a URL, establishes an HttpUrlConnection and retrieves
         * the image content as an InputStream, which it returns as
         * a bitmap.
         */
        private Bitmap downloadImage(String urlString) {
            InputStream is = null;
            Bitmap image;

            try {
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(10000 /* milliseconds */);
                conn.setConnectTimeout(15000 /* milliseconds */);
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

        private String getAuthData() {
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

    private class ImageAdapter extends ArrayAdapter<Bitmap> {

        private Context context;
        private List<Bitmap> data = null;

        public ImageAdapter(Context context, int layoutResourceId) {
            this(context, layoutResourceId, new ArrayList<Bitmap>());
        }

        public ImageAdapter(Context context, int layoutResourceId, List<Bitmap> data) {
            super(context, layoutResourceId, data);
            this.context = context;
            this.data = data;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (position < 0 || position >= data.size()) {
                return null;
            }
            ImageView view = (ImageView) (convertView == null ?
                    ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                            .inflate(R.layout.thumbnail_layout, parent, false) : convertView);
            view.setImageBitmap(data.get(position));
            return view;
        }
    }

    private interface Callback<X> {
        void execute(X... result);
    }
}
