package corp.katet.evernote;

import android.support.v7.app.AppCompatActivity;

public abstract class NoteActivity extends AppCompatActivity
    implements SaveChangesDialogFragment.DialogListener {

    protected abstract boolean saveChanges();

    @Override
    public void onBackPressed() {
        SaveChangesDialogFragment dialogFragment = new SaveChangesDialogFragment();
        dialogFragment.show(getSupportFragmentManager(), getString(R.string.save));
    }
}
