package corp.katet.evernote;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.evernote.client.android.asyncclient.EvernoteCallback;
import com.evernote.client.android.asyncclient.EvernoteNoteStoreClient;
import com.evernote.edam.notestore.NoteFilter;
import com.evernote.edam.notestore.NoteMetadata;
import com.evernote.edam.notestore.NotesMetadataList;
import com.evernote.edam.notestore.NotesMetadataResultSpec;
import com.evernote.edam.type.NoteSortOrder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NoteDashboardActivity extends AppCompatActivity {

    private NoteOrdering mOrderBy = null;
    private NoteAdapter mNotesAdapter;
    private ProgressBar mProgressBarLoader;

    private static final int PAGE_SIZE = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_dashboard);

        initializeUiWidgets();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // After resuming the Activity, check and refresh the data in the note adapter
        if (mOrderBy != null) {
            retrieveNotes(true);
        }
    }

    private void initializeUiWidgets() {
        mProgressBarLoader = (ProgressBar) findViewById(R.id.list_progress_bar);
        Spinner orderingSpinner = (Spinner) findViewById(R.id.ordering_options_spinner);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.notes_ordering_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        orderingSpinner.setAdapter(adapter);
        orderingSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                switch (pos) {
                    case 0:
                        mOrderBy = NoteOrdering.TITLE;
                        break;
                    case 1:
                        mOrderBy = NoteOrdering.DATE;
                        break;
                }

                // After setting the ordering criteria, query for the notes to retrieve
                retrieveNotes(true);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        orderingSpinner.setSelection(0);

        // Setup the ListView
        ListView noteList = (ListView) findViewById(R.id.notes_list_view);
        noteList.setAdapter(mNotesAdapter = new NoteAdapter(this, R.layout.note_layout));
        //mNotesAdapter = new NoteAdapter(this, R.layout.note_layout);
        TextView emptyView = new TextView(this);
        emptyView.setText(R.string.no_notes);
        noteList.setEmptyView(emptyView);
        noteList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        noteList.setOnScrollListener(new ScrollListener());
        noteList.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                Intent openNoteIntent = new Intent(NoteDashboardActivity.this, UpdateTextNoteActivity.class);
                openNoteIntent.putExtra(UpdateTextNoteActivity.EXTRA_NOTE_GUID,
                        mNotesAdapter.getItem(pos).getGuid());
                startActivity(openNoteIntent);
            }
        });
    }

    private void retrieveNotes(boolean clearData) {
        mProgressBarLoader.setVisibility(View.VISIBLE);

        if (clearData) {
            mNotesAdapter.clear();
        }

        // Filter the search results and sort them according to the specified criteria
        NoteFilter filter = new NoteFilter();
        switch (mOrderBy) {
            case TITLE:
                filter.setOrder(NoteSortOrder.TITLE.getValue());
                filter.setAscending(true);
                break;
            case DATE:
                filter.setOrder(NoteSortOrder.UPDATED.getValue());
                filter.setAscending(false);
                break;
        }

        EvernoteNoteStoreClient noteStore = EvernoteSnippets.createNoteStoreClient();
        NotesMetadataResultSpec spec = new NotesMetadataResultSpec();
        spec.setIncludeTitle(true);
        spec.setIncludeCreated(true);
        spec.setIncludeUpdated(true);

        noteStore.findNotesMetadataAsync(filter, mNotesAdapter.getCount(), PAGE_SIZE, spec,
                new EvernoteCallback<NotesMetadataList>() {
            @Override
            public void onSuccess(NotesMetadataList noteList) {
                mNotesAdapter.addAll(noteList.getNotes());
                mProgressBarLoader.setVisibility(View.GONE);
            }

            @Override
            public void onException(Exception e) {
                Toast.makeText(NoteDashboardActivity.this,
                        getString(R.string.retrieval_error) + ": " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
                mProgressBarLoader.setVisibility(View.GONE);
            }
        });
    }

    public void newNote(View v) {
        startActivity(new Intent(this, NewNoteActivity.class));
    }

    enum NoteOrdering { TITLE, DATE }

    private class NoteAdapter extends ArrayAdapter<NoteMetadata> {

        private Context context;
        private List<NoteMetadata> data = null;

        public NoteAdapter(Context context, int layoutResourceId) {
            this(context, layoutResourceId, new ArrayList<NoteMetadata>());
        }

        public NoteAdapter(Context context, int layoutResourceId, List<NoteMetadata> data) {
            super(context, layoutResourceId, data);
            this.context = context;
            this.data = data;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (position < 0 || position >= data.size()) {
                return null;
            }
            View view = convertView == null ? ((LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                    .inflate(R.layout.note_layout, parent, false) : convertView;
            NoteMetadata note = data.get(position);
            ((TextView) view.findViewById(R.id.textViewNoteTitle)).setText(note.getTitle());
            ((TextView) view.findViewById(R.id.textViewNoteDate)).setText(
                    new SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.getDefault()).format(
                            new Date(note.isSetUpdated() ? note.getUpdated() : note.getCreated())));
            return view;
        }
    }

    /**
     * This ScrollListener attempts to query for more data when this
     * ListView's scroll reaches its bottom
     */
    private class ScrollListener implements OnScrollListener {

        private int currentFirstVisibleItem, currentVisibleItemCount, currentTotalItemCount;

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem,
                             int visibleItemCount, int totalItemCount) {
            currentFirstVisibleItem = firstVisibleItem;
            currentVisibleItemCount = visibleItemCount;
            currentTotalItemCount = totalItemCount;
        }

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            if (currentFirstVisibleItem + currentVisibleItemCount == currentTotalItemCount
                    && scrollState == SCROLL_STATE_IDLE) {
                // Scroll has completed
                retrieveNotes(false);
            }
        }
    }
}
