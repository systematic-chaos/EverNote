package corp.katet.evernote.activity;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.view.View;
import android.widget.Toast;

import com.evernote.client.android.asyncclient.EvernoteCallback;
import com.evernote.edam.limits.Constants;
import com.evernote.edam.type.Note;

import corp.katet.evernote.R;
import corp.katet.evernote.dialog.DialogFragment;
import corp.katet.evernote.dialog.SetTitleDialogFragment;
import corp.katet.evernote.image.FingerPaintView;
import corp.katet.evernote.util.EvernoteSnippets;

public class CreateDrawNoteActivity extends NoteActivity {

    private FingerPaintView mFingerPaintView;
    private String mNoteTitle = "";
    private FloatingActionButton drawModeFab;
    private AnimatorSet fabFlipLeftIn, fabFlipLeftOut, fabFlipRightIn, fabFlipRightOut;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_draw);

        mFingerPaintView = (FingerPaintView) findViewById(R.id.finger_paint);

        setupFloatingActionButton();
    }

    public void askNoteTitle(View v) {
        SetTitleDialogFragment dialogFragment = new SetTitleDialogFragment();
        dialogFragment.show(getSupportFragmentManager(), getString(R.string.note_title));
    }

    protected boolean saveChanges() {
        boolean saveSuccess = true;
        EvernoteCallback<Note> cb = new EvernoteCallback<Note>() {
            @Override
            public void onSuccess(Note note) {
                Toast.makeText(CreateDrawNoteActivity.this, R.string.save_note_success,
                        Toast.LENGTH_LONG).show();
                finish();
            }

            @Override
            public void onException(Exception e) {
                // Something was wrong with the note data
                // See EDAMErrorCode enumeration for error code explanation
                // http://dev.evernote.com/documentacion/reference/Errors.html#Enum_EDAMErrorCode
                Toast.makeText(CreateDrawNoteActivity.this, R.string.save_note_error,
                        Toast.LENGTH_LONG).show();
            }
        };

        EvernoteSnippets.makeNoteAsync(cb, EvernoteSnippets.createNoteStoreClient(),
                mNoteTitle, getString(R.string.note_xml_prefix),
                mFingerPaintView.getBitmap());
        return saveSuccess;
    }

    public void changeDrawMode(View v) {
        if (mFingerPaintView.toggleDrawModeActive()) {
            flipFab(fabFlipLeftOut, fabFlipLeftIn, R.drawable.ic_eraser);
        } else {
            flipFab(fabFlipRightOut, fabFlipRightIn, R.drawable.ic_draw);
        }
    }

    private void setupFloatingActionButton() {
        drawModeFab = (FloatingActionButton) findViewById(R.id.draw_mode_fab);

        fabFlipLeftIn = (AnimatorSet) AnimatorInflater.loadAnimator(this,
                R.animator.fab_flip_left_in);
        fabFlipLeftOut = (AnimatorSet) AnimatorInflater.loadAnimator(this,
                R.animator.fab_flip_left_out);
        fabFlipRightIn = (AnimatorSet) AnimatorInflater.loadAnimator(this,
                R.animator.fab_flip_right_in);
        fabFlipRightOut = (AnimatorSet) AnimatorInflater.loadAnimator(this,
                R.animator.fab_flip_right_out);
        fabFlipLeftIn.setTarget(drawModeFab);
        fabFlipLeftOut.setTarget(drawModeFab);
        fabFlipRightIn.setTarget(drawModeFab);
        fabFlipRightOut.setTarget(drawModeFab);
        fabFlipLeftOut.setStartDelay(fabFlipLeftIn.getDuration());
        fabFlipRightOut.setStartDelay(fabFlipRightIn.getDuration());

        drawModeFab.setVisibility(View.VISIBLE);
        drawModeFab.setImageResource(R.drawable.ic_eraser);
    }

    private void flipFab(final AnimatorSet outAnim, final AnimatorSet inAnim,
                         final int drawableRes) {
        Handler handler = new Handler();
        outAnim.start();
        handler.postDelayed(new Runnable() {
            public void run() {
                drawModeFab.setImageResource(drawableRes);
                inAnim.start();
            }
        }, 750 - 2);
    }

    public void onDialogPositiveClick(DialogFragment dialog) {
        if (getString(R.string.save).equals(dialog.getTag())) {
            askNoteTitle(null);
        } else if (getString(R.string.note_title).equals(dialog.getTag())) {
            mNoteTitle = ((SetTitleDialogFragment) dialog).getTitle();
            mNoteTitle = mNoteTitle.length() > Constants.EDAM_NOTE_TITLE_LEN_MAX ?
                    mNoteTitle.trim().substring(0, Constants.EDAM_NOTE_TITLE_LEN_MAX)
                    : mNoteTitle.trim();
            if (mNoteTitle.length() > Constants.EDAM_NOTE_TITLE_LEN_MIN) {
                saveChanges();
            }
        }
    }

    public void onDialogNegativeClick(DialogFragment dialog) {
        if (getString(R.string.save).equals(dialog.getTag())) {
            finish();
        }
    }

    public void onDialogNeutralClick(DialogFragment dialog) {
    }
}
