package corp.katet.evernote.activity;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.text.method.KeyListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.evernote.client.android.EvernoteUtil;
import com.evernote.client.android.asyncclient.EvernoteCallback;
import com.evernote.edam.limits.Constants;
import com.evernote.edam.type.Note;
import com.evernote.edam.type.Resource;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import corp.katet.evernote.R;
import corp.katet.evernote.image.ImageDownloader;
import corp.katet.evernote.image.ImagePicker;
import corp.katet.evernote.image.ImageRecognitionTask;
import corp.katet.evernote.image.ImageThumbnailDownloader;
import corp.katet.evernote.util.Callback;
import corp.katet.evernote.util.EvernoteSnippets;

public class UpdateTextNoteActivity extends TextNoteActivity {

    public static final String EXTRA_NOTE_GUID = "EXTRA_NOTE_GUID";
    private static final int LOAD_IMAGE_ACTIVITY_REQUEST_CODE = 2;

    private KeyListener mDefaultKeyListener;
    private boolean mIsEditModeActive;
    private Note mFullNote;
    private DrawerLayout mDrawerLayout;
    private ListView mThumbnailListView;
    private ImageAdapter mImageAdapter;
    private FloatingActionButton fab, attachImageFab;
    private AnimatorSet fabFlipLeftIn, fabFlipLeftOut, fabFlipRightIn, fabFlipRightOut;
    private Animation fabOpen, fabClose;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDefaultKeyListener = mNotePaneEditText.getKeyListener();
        mIsEditModeActive = true;

        setupDrawerLayout();

        setupFloatingActionButton();

        setupListView();

        EvernoteSnippets.createNoteStoreClient()
                .getNoteAsync(getIntent().getStringExtra(EXTRA_NOTE_GUID),
                        true, false, true, false, new OpenNoteCallback());
    }

    public void changeAccessMode(View v) {
        if (mIsEditModeActive = !mIsEditModeActive) {
            flipFab(fabFlipLeftOut, fabFlipLeftIn, R.drawable.ic_save);
            mTitleEditText.setVisibility(View.VISIBLE);
            mTitleEditText.setSelection(mTitleEditText.getText().length());
            mNotePaneEditText.setFocusableInTouchMode(true);
            mNotePaneEditText.setKeyListener(mDefaultKeyListener);
            mNotePaneEditText.setFocusable(true);
            mNotePaneEditText.setSelection(mNotePaneEditText.getText().length());
            findViewById(R.id.expanded_image).setVisibility(View.INVISIBLE);
            closeDrawerLayout(true);
        } else {
            if (saveChanges()) {
                flipFab(fabFlipRightOut, fabFlipRightIn, R.drawable.ic_edit);
                mTitleEditText.clearFocus();
                mTitleEditText.setVisibility(View.GONE);
                mNotePaneEditText.clearFocus();
                mNotePaneEditText.setFocusable(false);
                mNotePaneEditText.setFocusableInTouchMode(false);
                mNotePaneEditText.setKeyListener(null);
                ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                        .hideSoftInputFromWindow(mNotePaneEditText.getWindowToken(), 0);
                getSupportActionBar().setTitle(mTitleEditText.getText().toString());
                if (!(mFullNote.getResources() == null || mFullNote.getResources().isEmpty())) {
                    openDrawerLayout();
                } else {
                    closeDrawerLayout(true);
                }
            }
        }
    }

    public void attachImage(View v) {
        // Create Intent to ask the user to pick a photo
        // Using FLAC_ACTIVITY_CLEAR_WHEN_TASK_RESET ensure that relaunching the
        // application from the device home screen does not return to the
        // external activity
        Intent externalActivityIntent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        externalActivityIntent.setType("image/*");
        externalActivityIntent.addFlags(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ?
                Intent.FLAG_ACTIVITY_NEW_DOCUMENT : Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        startActivityForResult(externalActivityIntent, LOAD_IMAGE_ACTIVITY_REQUEST_CODE);
    }

    @Override
    public void onBackPressed() {
        if (mIsEditModeActive) {
            super.onBackPressed();
        } else {
            finish();
        }
    }

    @Override
    protected boolean saveChanges() {
        mFullNote.setTitle(mTitleEditText.getText().toString());
        String noteContent = mFullNote.getContent();
        if (noteContent.startsWith(EvernoteUtil.NOTE_PREFIX)
                && noteContent.endsWith(EvernoteUtil.NOTE_SUFFIX)) {
            int startIndex = EvernoteUtil.NOTE_PREFIX.length();
            int endIndex = noteContent.contains("<br /><br />") ?
                    noteContent.indexOf("<br /><br />", startIndex)
                    : noteContent.length() - EvernoteUtil.NOTE_SUFFIX.length();
            mFullNote.setContent(noteContent.substring(0, startIndex)
                    + mNotePaneEditText.getText().toString() + noteContent.substring(endIndex));
        } else {
            mFullNote.setContent(EvernoteUtil.NOTE_PREFIX + mNotePaneEditText.getText().toString()
                    + EvernoteUtil.NOTE_SUFFIX);
        }
        EvernoteSnippets.createNoteStoreClient().updateNoteAsync(mFullNote,
                new EvernoteCallback<Note>() {
                    @Override
                    public void onSuccess(Note note) {
                        if (!mFullNote.isSetGuid()) {
                            mFullNote.setGuid(note.getGuid());
                        }
                    }

                    @Override
                    public void onException(Exception e) {
                        Toast.makeText(UpdateTextNoteActivity.this,
                                getString(R.string.save_note_error) + ": " + e.toString(),
                                Toast.LENGTH_LONG).show();
                    }
                });
        return true;
    }

    private void setupDrawerLayout() {
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerLayout.setScrimColor(Color.TRANSPARENT);
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        mDrawerLayout.setDrawerListener(new DrawerLayout.SimpleDrawerListener() {

            public void onDrawerClosed(View view) {
                closeDrawerLayout(mDrawerLayout.getDrawerLockMode(GravityCompat.START)
                        == DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            }

            public void onDrawerOpened(View view) {
                openDrawerLayout();
            }
        });
    }

    private void setupFloatingActionButton() {
        fab = (FloatingActionButton) findViewById(R.id.note_fab);
        fab.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                changeAccessMode(v);
            }
        });

        fabFlipLeftIn = (AnimatorSet) AnimatorInflater.loadAnimator(this,
                R.animator.fab_flip_left_in);
        fabFlipLeftOut = (AnimatorSet) AnimatorInflater.loadAnimator(this,
                R.animator.fab_flip_left_out);
        fabFlipRightIn = (AnimatorSet) AnimatorInflater.loadAnimator(this,
                R.animator.fab_flip_right_in);
        fabFlipRightOut = (AnimatorSet) AnimatorInflater.loadAnimator(this,
                R.animator.fab_flip_right_out);
        fabFlipLeftIn.setTarget(fab);
        fabFlipLeftOut.setTarget(fab);
        fabFlipRightIn.setTarget(fab);
        fabFlipRightOut.setTarget(fab);
        fabFlipLeftOut.setStartDelay(fabFlipLeftIn.getDuration());
        fabFlipRightOut.setStartDelay(fabFlipRightIn.getDuration());

        attachImageFab = (FloatingActionButton) findViewById(R.id.attach_image_fab);
        fabOpen = AnimationUtils.loadAnimation(this, R.anim.fab_open);
        fabClose = AnimationUtils.loadAnimation(this, R.anim.fab_close);
    }

    // Setup the image resources list view
    private void setupListView() {
        mThumbnailListView = (ListView) findViewById(R.id.reco_thumbnail_list_view);
        mThumbnailListView.setAdapter(mImageAdapter = new ImageAdapter(UpdateTextNoteActivity.this,
                R.id.resource_thumbnail));
        final ImageDownloader imageDownloader = new ImageDownloader(
                getResources().getInteger(R.integer.full_image_size),
                "jpeg", R.id.expanded_image, UpdateTextNoteActivity.this, null);
        mThumbnailListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                View expandedImage = findViewById(R.id.expanded_image);
                if (expandedImage.getVisibility() == View.VISIBLE) {
                    imageDownloader.zoomBackImageToThumb();
                    openDrawerLayout();
                }
                if (expandedImage.getVisibility() != View.VISIBLE ||
                        imageDownloader.getCurrentThumbView() != view) {
                    imageDownloader.zoomImageFromThumb(
                            (ImageView) view, mImageAdapter.getItem(position).getKey());
                }
                expandedImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        imageDownloader.zoomBackImageToThumb();
                    }
                });
            }
        });
    }

    private void openDrawerLayout() {
        ((ViewGroup.MarginLayoutParams) findViewById(R.id.container).getLayoutParams())
                .leftMargin = (int) getResources().getDimension(R.dimen.thumbnail_image_width);
        mDrawerLayout.requestLayout();
        if (!mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.openDrawer(GravityCompat.START);
        }
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
    }

    private void closeDrawerLayout(boolean lock) {
        ((ViewGroup.MarginLayoutParams) findViewById(R.id.container).getLayoutParams())
                .leftMargin = 0;
        mDrawerLayout.requestLayout();
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        }
        mDrawerLayout.setDrawerLockMode(lock ? DrawerLayout.LOCK_MODE_LOCKED_CLOSED
                : DrawerLayout.LOCK_MODE_UNLOCKED);
    }

    private void flipFab(final AnimatorSet outAnim, final AnimatorSet inAnim,
                         final int drawableRes) {
        Handler handler = new Handler();
        outAnim.start();
        handler.postDelayed(new Runnable() {
            public void run() {
                fab.setImageResource(drawableRes);
                inAnim.start();
                if (mIsEditModeActive) {
                    attachImageFab.setVisibility(View.INVISIBLE);
                    attachImageFab.startAnimation(fabClose);
                } else {
                    attachImageFab.setVisibility(View.VISIBLE);
                    attachImageFab.startAnimation(fabOpen);
                }
            }
        }, getResources().getInteger(R.integer.fab_flip_time_half) - 2);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        /*
         * If the result code is Activity.RESULT_OK and the activity result has
         * brought data back, retrieve and display the image. Otherwise, show an
         * informative toast
         */
        if (resultCode == Activity.RESULT_OK && data != null) {
            // Decide what to do based on the original request code
            try {
                switch (requestCode) {
                    case LOAD_IMAGE_ACTIVITY_REQUEST_CODE:
                        Uri selectedImage = data.getData();

                        String[] fileIdColumn = {MediaStore.Images.Media._ID};

                        Cursor cursor = getContentResolver().query(selectedImage, fileIdColumn,
                                null, null, null);
                        cursor.moveToFirst();

                        int columnIndex = cursor.getColumnIndex(fileIdColumn[0]);
                        long pictureId = cursor.getLong(columnIndex);
                        cursor.close();

                        final Bitmap bitmap = MediaStore.Images.Thumbnails.getThumbnail(
                                getContentResolver(), pictureId,
                                MediaStore.Images.Thumbnails.MINI_KIND, null);
                        final AbstractMap.SimpleImmutableEntry<String, Bitmap> pickedEntry
                                = new AbstractMap.SimpleImmutableEntry<>(
                                    selectedImage.toString(), bitmap);
                        mImageAdapter.add(pickedEntry);
                        openDrawerLayout();
                        new ImagePicker(getResources().getInteger(R.integer.full_image_size), "jpeg",
                                R.id.expanded_image, UpdateTextNoteActivity.this,
                                new Callback<Bitmap>() {
                                    public void execute(Bitmap... processedImages) {
                                        EvernoteSnippets.addImageResource(mFullNote,
                                                processedImages);
                                        saveChanges();
                                        runOnUiThread(new Runnable() {
                                            public void run() {
                                                int pos = mImageAdapter.getPosition(pickedEntry);
                                                mThumbnailListView.performItemClick(
                                                        mThumbnailListView.getChildAt(pos), pos,
                                                        mThumbnailListView.getItemIdAtPosition(pos));
                                            }
                                        });
                                    }
                        }).loadBitmap(
                                selectedImage.toString());
                        break;
                }
            } catch (Exception e) {
                Toast.makeText(this, R.string.retrieve_image_error, Toast.LENGTH_LONG).show();
            }
        } else {
            if (resultCode == Activity.RESULT_CANCELED) {
                // User cancelled the image capture
                Toast.makeText(this, R.string.no_image_picked, Toast.LENGTH_LONG).show();
            } else {
                // Image capture failed, advise user
                Toast.makeText(this, R.string.retrieve_image_error, Toast.LENGTH_LONG).show();
            }
        }
    }

    private class OpenNoteCallback implements EvernoteCallback<Note> {

        @Override
        public void onSuccess(Note note) {
            mFullNote = note;
            if (note.isSetTitle()) {
                mTitleEditText.setText(note.getTitle());
            }
            // Crop the note html content (excluding the note's prefix and
            // suffix) before setting its plain contents in the text area
            int startIndex, endIndex;
            String content = "";
            if (note.isSetContent()) {
                content = note.getContent();
                content = content.substring(EvernoteUtil.NOTE_PREFIX.length(),
                        content.length() - EvernoteUtil.NOTE_SUFFIX.length());
            }

            // In case the note content is filled, set it in the text area
            // Otherwise, look for recognition data into the note resources
            if (content.trim().length() > 0) {
                mNotePaneEditText.setText(content);
                changeAccessMode(findViewById(R.id.note_fab));
            }

            new ImageRecognitionTask(new Callback<String>() {
                @Override
                public void execute(String... result) {
                    StringBuilder content = new StringBuilder(mNotePaneEditText.getText().toString());
                    for (String text : result) {
                        content.append(text).append('\t');
                    }
                    mNotePaneEditText.setText(content.toString());
                    saveChanges();
                }
            }, UpdateTextNoteActivity.this).execute(note);

            retrieveImageResources();
        }

        @Override
        public void onException(Exception e) {
            Toast.makeText(UpdateTextNoteActivity.this, R.string.open_note_error + ": "
                    + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        private void retrieveImageResources() {
            List<Resource> noteResources = mFullNote.getResources();
            if (noteResources == null || noteResources.isEmpty())
                return;

            List<String> resourcesGuid = new ArrayList<>();
            for (Resource rsc : noteResources) {
                String format;
                switch (rsc.getMime()) {
                    case Constants.EDAM_MIME_TYPE_PNG:
                        format = "png";
                        break;
                    case Constants.EDAM_MIME_TYPE_JPEG:
                        format = "jpeg";
                        break;
                    case Constants.EDAM_MIME_TYPE_GIF:
                        format = "gif";
                        break;
                    default:
                        format = null;
                }
                if (format != null) {
                    resourcesGuid.add(rsc.getGuid());
                }
            }

            if (!resourcesGuid.isEmpty()) {
                new ImageThumbnailDownloader(getResources().getInteger(R.integer.thumbnail_image_size), "jpeg", true,
                        new Callback<Map.Entry<String,Bitmap>>() {
                            @Override
                            public void execute(Map.Entry<String, Bitmap>... result) {
                                mImageAdapter.addAll(result);
                                openDrawerLayout();
                            }
                        })
                        .execute(resourcesGuid.toArray(new String[0]));
            }
        }
    }

    private class ImageAdapter extends ArrayAdapter<Map.Entry<String, Bitmap>> {

        private Context context;
        private List<Map.Entry<String, Bitmap>> data = null;

        public ImageAdapter(Context context, int layoutResourceId) {
            this(context, layoutResourceId, new ArrayList<Map.Entry<String, Bitmap>>());
        }

        public ImageAdapter(Context context, int layoutResourceId,
                            List<Map.Entry<String, Bitmap>> data) {
            super(context, layoutResourceId, data);
            this.context = context;
            this.data = data;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (position < 0 || position >= data.size()) {
                return null;
            }
            ImageView view = (ImageView) (convertView == null ?
                    ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                            .inflate(R.layout.thumbnail_layout, parent, false) : convertView);
            view.setImageBitmap(data.get(position).getValue());
            return view;
        }
    }
}
