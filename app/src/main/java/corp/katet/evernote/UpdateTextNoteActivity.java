package corp.katet.evernote;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.KeyListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.evernote.client.android.EvernoteUtil;
import com.evernote.client.android.asyncclient.EvernoteCallback;
import com.evernote.edam.type.Note;

import java.util.List;

public class UpdateTextNoteActivity extends TextNoteActivity {

    public static final String EXTRA_NOTE_GUID = "EXTRA_NOTE_GUID";

    private KeyListener mDefaultKeyListener;
    private boolean mIsEditModeActive;
    private Note mFullNote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDefaultKeyListener = mNotePaneEditText.getKeyListener();
        mIsEditModeActive = true;

        ImageButton fab = (ImageButton) findViewById(R.id.note_fab);
        fab.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                changeAccessMode(v);
            }
        });
        fab.performClick();

        final AsyncTask<Note, Void, List<String>> imageRecognitionTask
                = new AsyncTask<Note, Void, List<String>>() {
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
                StringBuilder content = new StringBuilder();
                for (String text : result) {
                    content.append(text).append("    ");
                }
                mNotePaneEditText.setText(content.toString());
            }
        };

        final EvernoteCallback<Note> openNoteCallback = new EvernoteCallback<Note>() {
                @Override
                public void onSuccess(Note note) {
                mFullNote = note;
                if (note.isSetTitle()) {
                    getSupportActionBar().setTitle(note.getTitle());
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
                } else {
                    imageRecognitionTask.execute(note);
                }
            }

            @Override
            public void onException(Exception e) {
                Toast.makeText(UpdateTextNoteActivity.this, R.string.open_note_error + ": "
                        + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        };

        EvernoteSnippets.createNoteStoreClient()
                .getNoteAsync(getIntent().getStringExtra(EXTRA_NOTE_GUID),
                        true, true, true, false, openNoteCallback);
    }

    public void changeAccessMode(View v) {
        if (mIsEditModeActive = !mIsEditModeActive) {
            ((ImageView) v).setImageResource(R.drawable.ic_save);
            mTitleEditText.setFocusableInTouchMode(true);
            mTitleEditText.setVisibility(View.VISIBLE);
            mNotePaneEditText.setFocusableInTouchMode(true);
            mNotePaneEditText.setKeyListener(mDefaultKeyListener);
        } else {
            if (saveChanges()) {
                ((ImageView) v).setImageResource(R.drawable.ic_edit);
                mTitleEditText.setFocusableInTouchMode(false);
                mTitleEditText.setVisibility(View.GONE);
                mNotePaneEditText.setFocusableInTouchMode(false);
                mNotePaneEditText.setKeyListener(null);
                mNotePaneEditText.setFocusable(true);
                getSupportActionBar().setTitle(mTitleEditText.getText().toString());
            }
        }
    }

    @Override
    protected boolean saveChanges() {
        boolean saveSuccess = true;
        mFullNote.setTitle(mTitleEditText.getText().toString());
        mFullNote.setContent(EvernoteUtil.NOTE_PREFIX + mNotePaneEditText.getText().toString()
                + EvernoteUtil.NOTE_SUFFIX);
        try {
            mFullNote = EvernoteSnippets.createNoteStoreClient().updateNote(mFullNote);
        } catch (Exception e) {
            Toast.makeText(this, R.string.save_note_error + ": " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            saveSuccess = false;
        }
        return saveSuccess;
    }
}
