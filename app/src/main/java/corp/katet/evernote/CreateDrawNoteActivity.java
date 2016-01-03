package corp.katet.evernote;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.evernote.client.android.EvernoteUtil;
import com.evernote.client.android.asyncclient.EvernoteNoteStoreClient;
import com.evernote.edam.limits.Constants;
import com.evernote.edam.type.Data;
import com.evernote.edam.type.Note;
import com.evernote.edam.type.Resource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CreateDrawNoteActivity extends NoteActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_note_draw);

        mFingerPaintView = (FingerPaintView) findViewById(R.id.finger_paint);

        findViewById(R.id.note_fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveChanges();
            }
        });
    }

    private FingerPaintView mFingerPaintView;
    private String mNoteTitle = "";

    public void askNoteTitle() {
        SetTitleDialogFragment dialogFragment = new SetTitleDialogFragment();
        dialogFragment.show(getSupportFragmentManager(), getString(R.string.note_title));
    }

    protected boolean saveChanges() {
        return makeNote(EvernoteSnippets.createNoteStoreClient(), mNoteTitle, "",
                mFingerPaintView.getBitmap()) != null;
    }

    public void onDialogPositiveClick(DialogFragment dialog) {
        if (getString(R.string.save).equals(dialog.getTag())) {
            askNoteTitle();
        } else if (getString(R.string.note_title).equals(dialog.getTag())) {
            mNoteTitle = ((SetTitleDialogFragment) dialog).getTitle().trim().substring(0,
                    Constants.EDAM_NOTE_TITLE_LEN_MAX);
            if (mNoteTitle.length() > Constants.EDAM_NOTE_TITLE_LEN_MIN && saveChanges()) {
                finish();
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

    public Note makeNote(EvernoteNoteStoreClient noteStore, String noteTitle, String noteBody,
                          Bitmap... imageResources) {

        // Create a Note instance with title and body
        // Send Note object to user's account
        Note note = new Note();
        note.setTitle(noteTitle);

        // Build body of note
        StringBuilder body = new StringBuilder(EvernoteUtil.NOTE_PREFIX).append(noteBody);

        if (imageResources != null && imageResources.length > 0) {
            // Create Resource objects from image resources and add them to note body
            body.append("<br /><br />");
            List<Resource> resources = new ArrayList<>(imageResources.length);
            note.setResources(resources);
            for (Bitmap image : imageResources) {
                Resource r = makeResource(image);
                if (r == null)
                    continue;
                resources.add(r);
                body.append("Attachment with hash ")
                        .append(Arrays.toString(r.getData().getBodyHash()))
                        .append(": <br /><en-media type=\"").append(r.getMime())
                        .append("\" hash=\"").append(Arrays.toString(r.getData().getBodyHash()))
                        .append("\" /><br />");
            }
            body.append(EvernoteUtil.NOTE_SUFFIX);

            note.setContent(body.toString());

            // Attempt to create note in Evernote account
            try {
                note = noteStore.createNote(note);
            } catch (Exception e) {
                // Something was wrong with the note data
                // See EDAMErrorCode enumeration for error code explanation
                // http://dev.evernote.com/documentation/reference/Errors.html#Enum_EDAMErrorCode
                Toast.makeText(this, R.string.save_note_error, Toast.LENGTH_LONG)
                        .show();
                note = null;
            }
        }

        // Return created note object
        return note;
    }

    private Resource makeResource(Bitmap image) {
        Resource imageResource = new Resource();
        Data resourceData = new Data();
        imageResource.setData(resourceData);
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            // Write bitmap to file using PNG and 85% quality
            image.compress(Bitmap.CompressFormat.PNG, 85, stream);
            byte[] byteArray = stream.toByteArray();
            stream.close();
            resourceData.setBody(byteArray);
            resourceData.setSize(byteArray.length);
            resourceData.setBodyHash(Arrays.copyOfRange(md5(byteArray), 0,
                    Constants.EDAM_HASH_LEN));
            imageResource.setMime(Constants.EDAM_MIME_TYPE_PNG);
            imageResource.setHeight((short) image.getHeight());
            imageResource.setWidth((short) image.getWidth());
            image.recycle();
        } catch (IOException ioe) {
            Log.e(FingerPaintView.class.getName(), "makeResource [IOException]: "
                    + ioe.getMessage(), ioe);
            imageResource = null;
        }
        return imageResource;
    }

    private byte[] md5(byte[] bytes) {
        try {
            // Create MD5 Hash
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(bytes);
            byte[] digest = messageDigest.digest();

            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                int intVal = 0xFF & b;
                if (intVal < 0x10)
                    hexString.append('0');
                hexString.append(Integer.toHexString(intVal));
            }
            return hexString.toString().getBytes();
        } catch (NoSuchAlgorithmException nsae) {
            Log.e(FingerPaintView.class.getName(), "md5 [NoSuchAlgorithmException]: "
                    + nsae.getMessage(), nsae);
        }
        return bytes;
    }
}
