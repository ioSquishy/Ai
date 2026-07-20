package ai.API.OpenAI;

import java.util.List;

import com.squareup.moshi.Json;

public class ModerationObject {
    public String id;
    protected List<ModerationObjectResults> results;

    protected static class ModerationObjectResults {
        public boolean flagged;
        public ModerationObjectCategories categories;
        public ModerationObjectCategoryInputs category_applied_input_types;
    }

    protected static class ModerationObjectCategories {
        public boolean hate;
        public boolean harassment;
        public @Json(name = "self-harm") boolean selfharm;
        public boolean sexual;
        public boolean violence;
        public boolean illicit;
    }
    
    protected static class ModerationObjectCategoryInputs {
        public List<String> hate;
        public List<String> harassment;
        public @Json(name = "self-harm") List<String> selfharm;
        public List<String> sexual;
        public List<String> violence;
        public List<String> illicit;

        public static boolean anyContainsImage(ModerationObjectCategoryInputs categoryInputs) {
            if (categoryInputs == null) return false;
            if (categoryInputs.hate.contains("image")) return true;
            if (categoryInputs.harassment.contains("image")) return true;
            if (categoryInputs.selfharm.contains("image")) return true;
            if (categoryInputs.sexual.contains("image")) return true;
            if (categoryInputs.violence.contains("image")) return true;
            if (categoryInputs.illicit.contains("image")) return true;
            return false;
        }
    }
}
