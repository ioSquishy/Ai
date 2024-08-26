package ai.API.OpenAI;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import org.bson.Document;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonAdapter;

import ai.App;
import ai.Utility.Http;

public class ModerationEndpoint {
    private static final String moderationEndpoint = "https://api.openai.com/v1/moderations";
    private static final JsonAdapter<ModerationObject> moderationObjectAdapter = App.Moshi.adapter(ModerationObject.class);

    private static Callable<HttpResponse<String>> createModerationRequest(String inputText) {
        String requestBody = new Document("input", inputText).toJson();
        // System.out.println(requestBody);
        return Http.createRequest(Http.POST(moderationEndpoint, OpenaiApi.headers, requestBody));
    }

    public static CompletableFuture<ModerationResult> moderateText(String text) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // verify request success
                HttpResponse<String> apiResponse = createModerationRequest(text).call();
                if (apiResponse.statusCode() != 200) {
                    throw new Exception(apiResponse.body());
                }
    
                // System.out.println(apiResponse.body());
                return new ModerationResult(moderationObjectAdapter.fromJson(apiResponse.body()), text);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        });
    }

    public static class ModerationResult {
        public final String id;
        public final String inputText;
        public final boolean flagged;
        public final Flags flags;
        public static class Flags {
            public final boolean hate;
            public final boolean harassment;
            public final boolean selfHarm;
            public final boolean sexual;
            public final boolean violence;
            
            public Flags(boolean hate, boolean harassment, boolean selfHarm, boolean sexual, boolean violence) {
                this.hate = hate;
                this.harassment = harassment;
                this.selfHarm = selfHarm;
                this.sexual = sexual;
                this.violence = violence;
            }

            @Override
            public String toString() {
                String reasons = "";
                if (hate) reasons += "hate, ";
                if (harassment) reasons += "harassment, ";
                if (selfHarm) reasons += "self-harm, ";
                if (sexual) reasons += "sexual, ";
                if (violence) reasons += "violence, ";
                return reasons.isEmpty() ? "" : reasons.substring(0, reasons.length()-2);
            }
        }

        public ModerationResult(ModerationObject modObject, String inputText) {
            this.id = modObject.id;
            this.inputText = inputText;
            this.flagged = modObject.results.get(0).flagged;

            ModerationObjectCategories categories = modObject.results.get(0).categories;
            this.flags = new Flags(categories.hate, categories.harassment, categories.selfharm, categories.sexual, categories.violence);
        }

        public String toString() {
            return "id: " + id + "\ninputText: " + inputText + "\nflagged: " + flagged + "\nflagReasons: " + flags.toString();
        }
    }

    private static class ModerationObject {
        public String id;
        private List<ModerationObjectResults> results;
    }

    private static class ModerationObjectResults {
        public boolean flagged;
        public ModerationObjectCategories categories;
    }

    private static class ModerationObjectCategories {
        public boolean hate;
        public boolean harassment;
        public @Json(name = "self-harm") boolean selfharm;
        public boolean sexual;
        public boolean violence;

        @Override
        public String toString() {
            return "hate: " + hate + 
                "\nharassment: " + harassment +
                "\nself-harm: " + selfharm +
                "\nsexual: " + sexual + 
                "\nviolence: " + violence;
        }
    }
}
