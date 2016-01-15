package corp.katet.evernote;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

public class NewNoteActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_note_menu);

        findViewById(R.id.new_text_note_button).setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivity(new Intent(NewNoteActivity.this,
                                CreateTextNoteActivity.class));
                        finish();
                    }
                });

        findViewById(R.id.new_graphic_note_button).setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivity(new Intent(NewNoteActivity.this,
                                CreateDrawNoteActivity.class));
                        finish();
                    }
        });
    }
}
