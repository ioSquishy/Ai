package ai.API.OpenAI;

import java.util.ArrayList;
import java.util.Arrays;

import ai.API.OpenAI.ModerationObject.ModerationObjectCategories;
import ai.API.OpenAI.ModerationObject.ModerationObjectCategoryInputs;

public class ModerationResult {
    public final String id;
    public final String inputText;
    public final String[] flaggedImageURLs;
    public final boolean flagged;
    public final Flags flags;
    
    public static class Flags {
        public final boolean hate;
        public final boolean harassment;
        public final boolean selfHarm;
        public final boolean sexual;
        public final boolean violence;
        public final boolean illicit;
        
        public Flags(boolean hate, boolean harassment, boolean selfHarm, boolean sexual, boolean violence, boolean illicit) {
            this.hate = hate;
            this.harassment = harassment;
            this.selfHarm = selfHarm;
            this.sexual = sexual;
            this.violence = violence;
            this.illicit = illicit;
        }

        @Override
        public String toString() {
            String reasons = "";
            if (hate) reasons += "hate, ";
            if (harassment) reasons += "harassment, ";
            if (selfHarm) reasons += "self-harm, ";
            if (sexual) reasons += "sexual, ";
            if (violence) reasons += "violence, ";
            if (illicit) reasons += "illicit, ";
            return reasons.isEmpty() ? "" : reasons.substring(0, reasons.length()-2);
        }
    }

    public ModerationResult(ModerationObject modObject, String inputText, String inputImageURL) {
        this.id = modObject.id;
        this.inputText = inputText != null ? inputText : "";
        this.flagged = modObject.results.get(0).flagged;

        // add  inputImageURL to flaggedImageURLs if it was the reason the message got flagged
        if (this.flagged && ModerationObjectCategoryInputs.anyContainsImage(modObject.results.get(0).category_applied_input_types)) {
            this.flaggedImageURLs = new String[] {inputImageURL};
        } else {
            this.flaggedImageURLs = new String[0];
        }

        // set flags
        ModerationObjectCategories categories = modObject.results.get(0).categories;
        this.flags = new Flags(categories.hate, categories.harassment, categories.selfharm, categories.sexual, categories.violence, categories.illicit);
    }

    protected ModerationResult(String id, String inputText, String[] flaggedImageURLs, boolean flagged, Flags flags) {
        this.id = id;
        this.inputText = inputText;
        this.flaggedImageURLs = flaggedImageURLs;
        this.flagged = flagged;
        this.flags = flags;
    }

    /**
     * Merges multiple moderation results to override each other with preference for 'true' flags
     * @param results
     * @return one composite ModerationResult
     */
    public static ModerationResult mergeResults(ModerationResult... results) {
        String id = "";
        String inputText = "";
        ArrayList<String> flaggedImageURLs = new ArrayList<String>();
        boolean flagged = false;
        boolean hate = false;
        boolean harassment = false;
        boolean selfHarm = false;
        boolean sexual = false;
        boolean violence = false;
        boolean illicit = false;

        for (ModerationResult result : results) {
            id += result.id + " ";
            inputText += result.inputText + "\n";
            if (result.flaggedImageURLs != null) {
                flaggedImageURLs.addAll(Arrays.asList(result.flaggedImageURLs));
            }
            if (result.flagged) flagged = true;
            if (result.flags.hate) hate = true;
            if (result.flags.harassment) harassment = true;
            if (result.flags.selfHarm) selfHarm = true;
            if (result.flags.sexual) sexual = true;
            if (result.flags.violence) violence = true;
            if (result.flags.illicit) illicit = true;
        }

        id = id.strip();
        inputText = inputText.strip();
        Flags flags = new Flags(hate, harassment, selfHarm, sexual, violence, illicit);
        return new ModerationResult(id, inputText, flaggedImageURLs.toArray(String[]::new), flagged, flags);
    }

    public String toString() {
        return "id: " + id + "\ninputText: " + inputText + "\nflagged: " + flagged + "\nflagReasons: " + flags.toString();
    }
}
