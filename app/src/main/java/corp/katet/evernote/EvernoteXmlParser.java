package corp.katet.evernote;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Xml;

import com.evernote.edam.type.Note;
import com.evernote.edam.type.Resource;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EvernoteXmlParser {

    private static EvernoteXmlParser instance = null;
    private XmlPullParser parser;

    private EvernoteXmlParser() {
        parser = Xml.newPullParser();
    }

    public static EvernoteXmlParser newInstance() {
        if (instance == null) {
            instance = new EvernoteXmlParser();
        }
        return instance;
    }

    public List<String> recognizeResourcesData(Note note, Context context)
            throws XmlPullParserException, IOException {
        ArrayList<String> recoItems = new ArrayList<>();
        if (note.isSetResources()) {
            for (Resource r : note.getResources()) {
                if (r.isSetRecognition() && r.getRecognition().isSetBody()) {
                    recoItems.add(parseRecognition(r.getRecognition().getBody()));
                }
            }
        }
        return recoItems;
    }

    private String parseRecognition(byte[] recoBytes) throws XmlPullParserException, IOException {
        String text = null;

        //try (InputStream in = new ByteArrayInputStream(recoBytes)) {
        InputStream in = new ByteArrayInputStream(recoBytes);
        try {
            List<RecoIndex> resultText = readRecoIndex();
            if (!resultText.isEmpty()) {
                Collections.sort(resultText, Collections.reverseOrder());
                text = resultText.get(0).text;
            }
        } finally {
            in.close();
        }

        return text;
    }

    private List<RecoIndex> readRecoIndex() throws XmlPullParserException, IOException {
        List<RecoIndex> entries = new ArrayList<>();

        parser.require(XmlPullParser.START_TAG, null, "recoIndex");
        String name = parser.getName();
        // Starts by looking for the entry tag
        if (name.equals("t")) {
            entries.add(readEntry());
        } else {
            skip();
        }
        return entries;
    }

    // Parses the contents of an entry. If it encounters a t RecoIndex entry, hands it off
    // to its specific "read" method for processing. Otherwise, skips the thag.
    private RecoIndex readEntry() throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, "recoIndex");
        RecoIndex t = null;
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals("t")) {
                t = readRecoIndexT();
            } else {
                skip();
            }
        }
        return t;
    }

    // Processes t RecoIndex tags in the feed.
    private RecoIndex readRecoIndexT() throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, null, "t");
        int weight = Integer.parseInt(parser.getAttributeValue(null, "w"));
        String text = readText();
        RecoIndex t = new RecoIndex(text, weight);
        parser.require(XmlPullParser.END_TAG, null, "t");
        return t;
    }

    // Extract the text value for a tag
    private String readText() throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    private void skip() throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }

    private class RecoIndex implements Comparable<RecoIndex> {

        String text;
        int weight;

        public RecoIndex(String text, int weight) {
            this.text = text;
            this.weight = weight;
        }

        public int compareTo(@NonNull RecoIndex ri) {
            return this.weight < ri.weight ? -1 : this.weight > ri.weight ? 1 : 0;
        }

        @Override
        public boolean equals(Object o) {
            boolean eq = false;
            if (o != null && o instanceof RecoIndex) {
                RecoIndex ri = (RecoIndex) o;
                eq = this.text.equals(ri.text) || this.weight == ri.weight;
            }
            return eq;
        }
    }
}
