package corp.katet.evernote.activity;

import android.content.Context;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import corp.katet.evernote.dialog.DialogFragment;
import corp.katet.evernote.R;

public abstract class TextNoteActivity extends NoteActivity
    implements DialogFragment.DialogListener {

    protected EditText mTitleEditText;
    protected EditText mNotePaneEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_text);
        mTitleEditText = (EditText) findViewById(R.id.title_edit_text);
        mNotePaneEditText = (EditText) findViewById(R.id.note_pane_edit_text);
        View.OnTouchListener focusListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                InputMethodManager imm
                        = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (v.isFocusable() && v.isFocusableInTouchMode()) {
                    v.requestFocusFromTouch();
                    imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT);
                } else {
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
                return true;
            }
        };
        mTitleEditText.setOnTouchListener(focusListener);
        mNotePaneEditText.setOnTouchListener(focusListener);
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
