package corp.katet.evernote.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;

import corp.katet.evernote.R;

public class SaveChangesDialogFragment extends DialogFragment {

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Chain together various setter methods to set the dialog characteristics
        builder.setTitle(R.string.save_note_title)
                .setMessage(R.string.save_changes_question)
                .setPositiveButton(R.string.save, buttonListener)
                .setNeutralButton(R.string.dismiss, buttonListener)
                .setNegativeButton(R.string.discard, buttonListener);
        // Create the AlertDialog object and return it
        return builder.create();
    }
}
