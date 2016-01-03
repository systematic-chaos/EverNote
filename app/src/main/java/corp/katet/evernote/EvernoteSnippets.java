package corp.katet.evernote;

import android.app.Activity;
import android.content.Context;
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
import com.evernote.edam.type.LinkedNotebook;
import com.evernote.edam.type.Note;
import com.evernote.edam.type.Notebook;
import com.evernote.thrift.TException;

import java.util.ArrayList;
import java.util.List;

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
}
