package corp.katet.evernote.util;

public interface Callback<X> {
    void execute(X... result);
}
