package corp.katet.evernote;

import android.os.Bundle;
import android.widget.EditText;

public abstract class TextNoteActivity extends NoteActivity
    implements  SaveChangesDialogFragment.DialogListener {

    protected EditText mTitleEditText;
    protected EditText mNotePaneEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_text);
        mTitleEditText = (EditText) findViewById(R.id.title_edit_text);
        mNotePaneEditText = (EditText) findViewById(R.id.note_pane_edit_text);
    }

    public void onDialogPositiveClick(DialogFragment dialog) {
        if (saveChanges()) {
            finish();
        }
    }

    public void onDialogNegativeClick(DialogFragment dialog) {
        finish();
    }

    public void onDialogNeutralClick(DialogFragment dialog) {
    }
}
