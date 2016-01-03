package corp.katet.evernote;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.evernote.client.android.EvernoteUtil;
import com.evernote.edam.type.Note;

public class CreateTextNoteActivity extends TextNoteActivity {

    private Note mSavedNote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ImageButton fab = (ImageButton) findViewById(R.id.note_fab);
        fab.setImageResource(R.drawable.ic_save);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveChanges();
            }
        });
    }

    @Override
    protected boolean saveChanges() {
        boolean saveSuccess = true;
        try {
            Note note = mSavedNote == null ? new Note() : mSavedNote;
            note.setTitle(mTitleEditText.getText().toString());
            note.setContent(EvernoteUtil.NOTE_PREFIX + mNotePaneEditText.getText().toString()
                    + EvernoteUtil.NOTE_SUFFIX);
            mSavedNote = mSavedNote == null ?
                    EvernoteSnippets.createNoteStoreClient().createNote(note)
                    : EvernoteSnippets.createNoteStoreClient().updateNote(note);
            if (mSavedNote != null && mSavedNote.isSetTitle()) {
                getSupportActionBar().setTitle(mSavedNote.getTitle());
            }
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.save_note_error) + ": " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
            saveSuccess = false;
        }
        return saveSuccess;
    }
}
