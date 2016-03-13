package corp.katet.evernote.util;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.evernote.client.android.EvernoteSession;
import com.evernote.client.android.EvernoteUtil;
import com.evernote.client.android.asyncclient.EvernoteBusinessNotebookHelper;
import com.evernote.client.android.asyncclient.EvernoteCallback;
import com.evernote.client.android.asyncclient.EvernoteLinkedNotebookHelper;
import com.evernote.client.android.asyncclient.EvernoteNoteStoreClient;
import com.evernote.client.android.asyncclient.EvernoteUserStoreClient;
import com.evernote.edam.error.EDAMNotFoundException;
import com.evernote.edam.error.EDAMSystemException;
import com.evernote.edam.error.EDAMUserException;
import com.evernote.edam.limits.Constants;
import com.evernote.edam.type.Data;
import com.evernote.edam.type.LinkedNotebook;
import com.evernote.edam.type.Note;
import com.evernote.edam.type.Notebook;
import com.evernote.edam.type.Resource;
import com.evernote.thrift.TException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;

public class EvernoteSnippets {

    public static EvernoteNoteStoreClient createNoteStoreClient() {
        return EvernoteSession.getInstance().getEvernoteClientFactory().getNoteStoreClient();
    }

    public static EvernoteUserStoreClient createUserStoreClient() {
        return EvernoteSession.getInstance().getEvernoteClientFactory().getUserStoreClient();
    }

    public static EvernoteBusinessNotebookHelper createBusinessNotebookHelper()
    throws TException, EDAMUserException, EDAMSystemException {
        return EvernoteSession.getInstance().getEvernoteClientFactory().getBusinessNotebookHelper();
    }

    public static EvernoteLinkedNotebookHelper createLinkedNotebookHelper(LinkedNotebook linkedNotebook)
    throws EDAMUserException, EDAMSystemException, EDAMNotFoundException, TException {
        return EvernoteSession.getInstance().getEvernoteClientFactory()
                .getLinkedNotebookHelper(linkedNotebook);
    }

    private static final String LOGTAG = "EvernoteSnippets";

    public static void getNotebookList(final Context context) {

        if (!EvernoteSession.getInstance().isLoggedIn()) {
            return;
        }

        EvernoteNoteStoreClient noteStoreClient = createNoteStoreClient();
        noteStoreClient.listNotebooksAsync(new EvernoteCallback<List<Notebook>>() {
            @Override
            public void onSuccess(List<Notebook> result) {
                List<String> namesList = new ArrayList<>(result.size());
                for (Notebook notebook : result) {
                    namesList.add(notebook.getName());
                }
                String notebookNames = TextUtils.join(", ", namesList);
                Toast.makeText(context, notebookNames + " notebooks have been retrieved",
                        Toast.LENGTH_LONG).show();
            }

            @Override
            public void onException(Exception exception) {
                Log.e(LOGTAG, "Error retrieving notebooks", exception);
            }
        });
    }

    public static void createNote(final Context context, String title, String content) {
        if (!EvernoteSession.getInstance().isLoggedIn()) {
            return;
        }

        EvernoteNoteStoreClient noteStoreClient = createNoteStoreClient();

        Note note = new Note();
        note.setTitle(title);
        note.setContent(EvernoteUtil.NOTE_PREFIX + content + EvernoteUtil.NOTE_SUFFIX);

        noteStoreClient.createNoteAsync(note, new EvernoteCallback<Note>() {
            @Override
            public void onSuccess(Note result) {
                Toast.makeText(context, result.getTitle() + " has been created", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onException(Exception exception) {
                Log.e(LOGTAG, "Error creating note_layout", exception);
            }
        });
    }

    public static void accessBusinessData(final Activity context, final String title, final String content) {
        new Thread() {
                @Override
                public void run() {
                    try {
                        if (!createUserStoreClient().isBusinessUser()) {
                            Log.d(LOGTAG, "Not a business User");
                            return;
                        }

                        EvernoteBusinessNotebookHelper businessNotebookHelper
                                = createBusinessNotebookHelper();
                        List<LinkedNotebook> businessNotebooks
                                = businessNotebookHelper.listBusinessNotebooks(
                                EvernoteSession.getInstance());
                        if (businessNotebooks.isEmpty()) {
                            Log.d(LOGTAG, "No business notebooks found");
                        }

                        LinkedNotebook linkedNotebook = businessNotebooks.get(0);

                        Note note = new Note();
                        note.setTitle(title);
                        note.setContent(EvernoteUtil.NOTE_PREFIX + content
                                + EvernoteUtil.NOTE_SUFFIX);

                        EvernoteLinkedNotebookHelper linkedNotebookHelper
                                = createLinkedNotebookHelper(linkedNotebook);
                        final Note createdNote = linkedNotebookHelper.createNoteInLinkedNotebook(note);

                        context.runOnUiThread(new Runnable() {
                            @Override
                        public void run() {
                                Toast.makeText(context,
                                        createdNote.getTitle() + " has been created.",
                                        Toast.LENGTH_LONG).show();
                            }
                        });
                    } catch (TException | EDAMUserException | EDAMSystemException
                            | EDAMNotFoundException e) {
                        e.printStackTrace();
                    }
                }
        }.start();
    }

    public static Future<Note> makeNoteAsync(EvernoteCallback<Note> callback,
                                             EvernoteNoteStoreClient noteStore, String noteTitle,
                                             String noteBody, Bitmap... imageResources) {

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
                body.append("<div>").append(makeResourceBody(r)).append("</div>");
            }
            body.append(EvernoteUtil.NOTE_SUFFIX);

            note.setContent(body.toString());

            // Attempt to create note in Evernote account
            // Return a promise for the newly created note object
            return noteStore.createNoteAsync(note, callback);
        } else {
            callback.onException(new Exception());
            return null;
        }
    }

    public static String makeResourceBody(Resource r) {
        StringBuilder hexhash = new StringBuilder();
        for (byte hashByte : r.getData().getBodyHash()) {
            int intVal = 0xff & hashByte;
            if (intVal < 0x10) {
                hexhash.append('0');
            }
            hexhash.append(Integer.toHexString(intVal));
        }
        StringBuilder resourceBody = new StringBuilder("Attachment with hash ");
        resourceBody.append(hexhash.toString())
                .append(": <br /><en-media type=\"").append(r.getMime())
                .append("\" hash=\"").append(hexhash.toString())
                .append("\" /><br />");
        return resourceBody.toString();
    }
    public static void addImageResource(Note note, Bitmap... imageResources) {
        List<Resource> noteResources = note.isSetResources() ?
                note.getResources() : new ArrayList<Resource>();
        Resource r;
        String c = note.isSetContent() ? note.getContent() : "";
        StringBuilder rBody = new StringBuilder();
        for (Bitmap bm : imageResources) {
            r = EvernoteSnippets.makeResource(bm);
            if (r == null)
                continue;
            noteResources.add(r);
            if (!c.contains("<br /><br />")) {
                rBody.append("<br /><br />");
            }
            rBody.append(EvernoteSnippets.makeResourceBody(r));
        }
        c = c.startsWith(EvernoteUtil.NOTE_PREFIX) && c.endsWith(EvernoteUtil.NOTE_SUFFIX) ?
                c.substring(0, c.length() - EvernoteUtil.NOTE_SUFFIX.length())
                        + rBody.toString() + EvernoteUtil.NOTE_SUFFIX
                : EvernoteUtil.NOTE_PREFIX + c + rBody.toString() + EvernoteUtil.NOTE_SUFFIX;
        note.setContent(c);
    }

    public static Resource makeResource(Bitmap image) {
        Resource imageResource = new Resource();
        Data resourceData = new Data();
        imageResource.setData(resourceData);
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            // Write bitmap to file using JPEG and 85% quality
            image.compress(Bitmap.CompressFormat.JPEG, 85, stream);
            byte[] byteArray = stream.toByteArray();
            stream.close();
            resourceData.setBody(byteArray);
            resourceData.setSize(byteArray.length);
            resourceData.setBodyHash(Arrays.copyOfRange(md5(byteArray), 0,
                    Constants.EDAM_HASH_LEN));
            imageResource.setMime(Constants.EDAM_MIME_TYPE_JPEG);
            imageResource.setHeight((short) image.getHeight());
            imageResource.setWidth((short) image.getWidth());
        } catch (IOException ioe) {
            Log.e("makeResource", "[IOException]: " + ioe.getMessage(), ioe);
            imageResource = null;
        }
        return imageResource;
    }

    public static byte[] md5(byte[] bytes) {
        try {
            // Create MD5 Hash
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(bytes);
            return messageDigest.digest();
        } catch (NoSuchAlgorithmException nsae) {
            Log.e("md5", "[NoSuchAlgorithmException]: " + nsae.getMessage(), nsae);
        }
        return bytes;
    }
}
