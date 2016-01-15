package corp.katet.evernote;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
        if (checkNetworkConnection()) {
            if (!EvernoteSession.getInstance().isLoggedIn()) {
                EvernoteSession.getInstance().authenticate(this);
            } else {
                onLoginFinished(true);
            }
        } else {
            Toast.makeText(this, R.string.connection_error, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onLoginFinished(boolean successful) {
        // Handle login result
        if (successful) {
            startActivity(new Intent(this, NoteDashboardActivity.class));
        } else {
            Toast.makeText(this, getString(R.string.login_error), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case EvernoteSession.REQUEST_CODE_LOGIN:
                onLoginFinished(resultCode == AppCompatActivity.RESULT_OK);
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    /**
     * Before attempting to authenticate, makes sure that there is a network connection.
     */
    private boolean checkNetworkConnection() {
        NetworkInfo networkInfo = ((ConnectivityManager) getSystemService(
                Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }
}
