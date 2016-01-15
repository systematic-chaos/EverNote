package corp.katet.evernote;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class NewNoteActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_note_menu);

        ((Button) findViewById(R.id.new_text_note_button)).setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivity(new Intent(NewNoteActivity.this,
                                CreateTextNoteActivity.class));
                        finish();
                    }
                });

        ((Button) findViewById(R.id.new_graphic_note_button)).setOnClickListener(
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
