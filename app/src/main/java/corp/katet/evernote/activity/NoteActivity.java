package corp.katet.evernote.activity;

import android.support.v7.app.AppCompatActivity;

import corp.katet.evernote.dialog.DialogFragment;
import corp.katet.evernote.R;
import corp.katet.evernote.dialog.SaveChangesDialogFragment;

public abstract class NoteActivity extends AppCompatActivity
    implements DialogFragment.DialogListener {

    protected abstract boolean saveChanges();

    @Override
    public void onBackPressed() {
        SaveChangesDialogFragment dialogFragment = new SaveChangesDialogFragment();
        dialogFragment.show(getSupportFragmentManager(), getString(R.string.save));
    }
}
