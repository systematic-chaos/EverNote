package corp.katet.evernote.image;

import android.app.Activity;
import android.os.AsyncTask;
import android.widget.Toast;

import com.evernote.edam.type.Note;

import java.util.List;

import corp.katet.evernote.util.Callback;
import corp.katet.evernote.util.EvernoteXmlParser;

public class ImageRecognitionTask extends AsyncTask<Note, Void, List<String>> {

    private Callback<String> callback;
    private Activity context;

    public ImageRecognitionTask(Callback<String> callback, Activity context) {
        this.callback = callback;
        this.context = context;
    }

    @Override
    protected List<String> doInBackground(Note... notes) {
        List<String> result = null;
        EvernoteXmlParser parser = EvernoteXmlParser.newInstance();
        try {
            result = parser.recognizeResourcesData(notes[0], context);
        } catch (final Exception e) {
            context.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }
        return result;
    }

    @Override
    protected void onPostExecute(List<String> result) {
        if (!isCancelled() && result != null && !result.isEmpty()) {
            callback.execute(result.toArray(new String[0]));
        }
    }
}
