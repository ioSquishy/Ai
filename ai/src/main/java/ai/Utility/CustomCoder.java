package ai.Utility;

import org.bson.Document;

import ai.Constants;

public abstract class CustomCoder {
    public String encode() {
        throw new UnsupportedOperationException();
    }

    public static CustomCoder decode(String json) {
        throw new UnsupportedOperationException();
    }

    public static String getCustomID(String json) {
        Document parsedJson = Document.parse(json);
        return parsedJson.getString(Constants.GlobalJsonKeys.customID);
    }
}
