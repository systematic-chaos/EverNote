package corp.katet.evernote.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;

public abstract class DialogFragment extends android.support.v4.app.DialogFragment {

    @Override
    @NonNull
    abstract public Dialog onCreateDialog(Bundle savedInstanceState);

    protected DialogInterface.OnClickListener buttonListener
            = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    mListener.onDialogPositiveClick(DialogFragment.this);
                    break;
                case DialogInterface.BUTTON_NEGATIVE:
                    mListener.onDialogNegativeClick(DialogFragment.this);
                    break;
                case DialogInterface.BUTTON_NEUTRAL:
                    mListener.onDialogNeutralClick(DialogFragment.this);
            }
        }
    };

    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Every method passes the DialogFragment in case the host needs to query it. */
     public interface DialogListener {
        void onDialogPositiveClick(DialogFragment dialog);
        void onDialogNeutralClick(DialogFragment dialog);
        void onDialogNegativeClick(DialogFragment dialog);
    }

    // Use this instance of the interface to deliver action events
    protected DialogListener mListener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the DialogListener so we can send events back to the host
            mListener = (DialogListener) activity;
        } catch (ClassCastException cce) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.getLocalClassName() + " must implement "
                    + DialogListener.class.getName());
        }
    }
}
