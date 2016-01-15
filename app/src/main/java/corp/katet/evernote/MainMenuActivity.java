package corp.katet.evernote;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.evernote.client.android.EvernoteSession;
import com.evernote.client.android.login.EvernoteLoginFragment;

public class MainMenuActivity extends AppCompatActivity
        implements EvernoteLoginFragment.ResultCallback {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_menu_layout);

        // User is already logged in
        if (EvernoteSession.getInstance().isLoggedIn()) {
            onLoginFinished(true);
        }
    }

    public void authenticate(View v) {
        EvernoteSession.getInstance().authenticate(this);
    }

    @Override
    public void onLoginFinished(boolean successful) {
        // Handle login result
        Toast.makeText(this, "onLoginFinished: " + successful, Toast.LENGTH_SHORT).show();

        if (successful) {
            startActivity(new Intent(this, NoteDashboardActivity.class));
        } else {
            Toast.makeText(this, getString(R.string.login_error), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Toast.makeText(this, "onActivityResult: "
                + (requestCode == EvernoteSession.REQUEST_CODE_LOGIN) + " "
                + (resultCode == AppCompatActivity.RESULT_OK), Toast.LENGTH_SHORT).show();
        switch (requestCode) {
            case EvernoteSession.REQUEST_CODE_LOGIN:
                onLoginFinished(resultCode == AppCompatActivity.RESULT_OK);
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }
}
