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
        public final List<String> flagReasons;

        public ModerationResult(ModerationObject modObject, String inputText) {
            this.id = modObject.id;
            this.inputText = inputText;
            this.flagged = modObject.results.get(0).flagged;
            this.flagReasons = getFlagReasons(modObject.results.get(0).categories);
        }

        private List<String> getFlagReasons(ModerationObjectCategories categories) {
            // System.out.println(categories.toString());
            List<String> reasons = new ArrayList<String>();
            if (categories.hate) reasons.add("hate");
            if (categories.harassment) reasons.add("harassment");
            if (categories.selfharm) reasons.add("self-harm");
            if (categories.sexual) reasons.add("sexual");
            if (categories.violence) reasons.add("violence");
            return reasons;
        }

        public String toString() {
            return "id: " + id + "\ninputText: " + inputText + "\nflagged: " + flagged + "\nflagReasons: " + flagReasons.toString();
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
