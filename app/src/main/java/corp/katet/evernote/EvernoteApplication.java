package corp.katet.evernote;

import android.app.Application;

import com.evernote.client.android.EvernoteSession;

import java.util.Locale;

public class EvernoteApplication extends Application {

    private static final String CONSUMER_KEY = "fjfbp";
    private static final String CONSUMER_SECRET = "57d45aad12699fb5";
    private static final EvernoteSession.EvernoteService EVERNOTE_SERVICE
            = EvernoteSession.EvernoteService.SANDBOX;

    private EvernoteSession mEvernoteSession;

    @Override
    public void onCreate() {
        super.onCreate();

        mEvernoteSession = new EvernoteSession.Builder(this)
                .setEvernoteService(EVERNOTE_SERVICE)
                //.setSupportAppLinkedNotebooks(true)
                .setForceAuthenticationInThirdPartyApp(true)
                .setLocale(Locale.getDefault())
                .build(CONSUMER_KEY, CONSUMER_SECRET)
                .asSingleton();
    }

    public EvernoteSession getEvernoteSession() {
        return mEvernoteSession;
    }
}
