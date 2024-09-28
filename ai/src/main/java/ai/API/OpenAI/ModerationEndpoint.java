package ai.API.OpenAI;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import org.bson.Document;

import com.squareup.moshi.Json;
import com.squareup.moshi.JsonAdapter;

import ai.App;
import ai.Utility.Http;

public class ModerationEndpoint {
    private static final String moderationEndpoint = "https://api.openai.com/v1/moderations";
    private static final JsonAdapter<RequestBody> requestBodyAdapter = App.Moshi.adapter(RequestBody.class);
    private static final JsonAdapter<ModerationObject> moderationObjectAdapter = App.Moshi.adapter(ModerationObject.class);

    private static Callable<HttpResponse<String>> createModerationRequest(String text) {
        String requestBodyJson = new Document("input", text).toJson();
        return Http.createRequest(Http.POST(moderationEndpoint, OpenaiApi.headers, requestBodyJson));
    }

    private static Callable<HttpResponse<String>> createModerationRequest(String text, String[] imageURLs) {
        RequestBody requestBody = new RequestBody();
        requestBody.addInput(RequestBodyInput.createTextInput(text));
        requestBody.addInput(RequestBodyInput.createImageInput(imageURLs));

        String requestBodyJson =requestBodyAdapter.toJson(requestBody);
        System.out.println(requestBodyJson);
        return Http.createRequest(Http.POST(moderationEndpoint, OpenaiApi.headers, requestBodyJson));
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
                return new ModerationResult(moderationObjectAdapter.fromJson(apiResponse.body()), text, null);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Moderates text and multiple images.
     * @param text
     * @param imageURLs
     * @return
     */
    public static CompletableFuture<ModerationResult> moderateTextAndImages(String text, String[] imageURLs) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // verify request success
                HttpResponse<String> apiResponse = createModerationRequest(text, imageURLs).call();
                if (apiResponse.statusCode() != 200) {
                    throw new Exception(apiResponse.body());
                }
    
                // System.out.println(apiResponse.body());
                return new ModerationResult(moderationObjectAdapter.fromJson(apiResponse.body()), text, imageURLs);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        });
    }

    public static class ModerationResult {
        public final String id;
        public final String inputText;
        public final String[] inputImageURLs;
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

        public ModerationResult(ModerationObject modObject, String inputText, String[] inputImageURLs) {
            this.id = modObject.id;
            this.inputText = inputText != null ? inputText : "";
            this.inputImageURLs = inputImageURLs;
            this.flagged = modObject.results.get(0).flagged;

            ModerationObjectCategories categories = modObject.results.get(0).categories;
            this.flags = new Flags(categories.hate, categories.harassment, categories.selfharm, categories.sexual, categories.violence, categories.illicit);
        }

        public String getInputImageURLs() {
            String result = "";
            for (int i = 0; i < inputImageURLs.length; i++) {
                result += inputImageURLs[i] + "\n";
            }
            return result.stripTrailing();
        }

        public String toString() {
            return "id: " + id + "\ninputText: " + inputText + "\nflagged: " + flagged + "\nflagReasons: " + flags.toString();
        }
    }

    /// open ai object classes
    // input body classes
    private static class RequestBody {
        public final String model = "omni-moderation-latest";
        public List<RequestBodyInput> input;

        public RequestBody() {
            this.input = new ArrayList<RequestBodyInput>();
        }

        public void addInput(RequestBodyInput input) {
            if (input.text == null && input.image_url == null) {
                return;
            }
            this.input.add(input);
        }
    }

    private static class RequestBodyInput {
        public final String type;
        public String text = null;
        public Map<String, String> image_url = null;

        public static RequestBodyInput createTextInput(String text) {
            return new RequestBodyInput("text", text, null);
        }

        public static RequestBodyInput createImageInput(String[] imageURLs) {
            return new RequestBodyInput("image_url", null, imageURLs);
        }

        /**
         * Creates an input for the RequestBody class
         * @param type can be either "text" or "image_url"
         * @param text text to be moderated if type is "text"
         * @param imageURLs images to be moderated if type is "image_url"
         */
        private RequestBodyInput(String type, String text, String[] imageURLs) {
            this.type = type;
            this.text = (text != null && !text.isBlank()) ? text : null;

            if (imageURLs != null && imageURLs.length > 0) {
                Map<String, String> imageUrlMap = new HashMap<String, String>();
                for (int i = 0; i < imageURLs.length; i++) {
                    imageUrlMap.put("url", imageURLs[i]);
                }
                // TODO url is constantly being overried because its a map with unique keys duh
                this.image_url = imageUrlMap;
            }
        }
    }

    // response classes
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
        public boolean illicit;
    }
}
