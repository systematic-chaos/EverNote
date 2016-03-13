package corp.katet.evernote.activity;

import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.evernote.client.android.EvernoteUtil;
import com.evernote.client.android.asyncclient.EvernoteCallback;
import com.evernote.edam.type.Note;

import corp.katet.evernote.util.EvernoteSnippets;
import corp.katet.evernote.R;

public class CreateTextNoteActivity extends TextNoteActivity {

    private Note mSavedNote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ((DrawerLayout) findViewById(R.id.drawer_layout))
                .setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

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
        Note note = mSavedNote == null ? new Note() : mSavedNote;
        note.setTitle(mTitleEditText.getText().toString());
        note.setContent(EvernoteUtil.NOTE_PREFIX + mNotePaneEditText.getText().toString()
                + EvernoteUtil.NOTE_SUFFIX);
        EvernoteCallback<Note> cb = new EvernoteCallback<Note>() {
            @Override
            public void onSuccess(Note note) {
                mSavedNote = note;
                if (mSavedNote != null && mSavedNote.isSetTitle()) {
                    getSupportActionBar().setTitle(mSavedNote.getTitle());
                }
                Toast.makeText(CreateTextNoteActivity.this, getString(R.string.save_note_success)
                        + " " + mSavedNote.getTitle(), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onException(Exception e) {
                Toast.makeText(CreateTextNoteActivity.this,
                        getString(R.string.save_note_error) + ": " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        };
        if (mSavedNote == null) {
            EvernoteSnippets.createNoteStoreClient().createNoteAsync(note, cb);
        } else {
            EvernoteSnippets.createNoteStoreClient().updateNoteAsync(note, cb);
        }

        return saveSuccess;
    }
}
