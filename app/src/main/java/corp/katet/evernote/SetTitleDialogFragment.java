package corp.katet.evernote;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

public class SetTitleDialogFragment extends DialogFragment {

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because it's going in the dialog layout
        View inflated = inflater.inflate(R.layout.dialog_title, null);
        mTitleEditText = (EditText) inflated.findViewById(R.id.title_edit_text);
        // Chain together various setter methods to set the dialog characteristics
        builder.setView(inflated)
                .setTitle(R.string.note_title)
                .setPositiveButton(R.string.ok, buttonListener)
                .setNegativeButton(R.string.dismiss, buttonListener);

        // Create the AlertDialog object and return it
        return builder.create();
    }

    private EditText mTitleEditText;

    public String getTitle() {
        return mTitleEditText.getText().toString();
    }
}
