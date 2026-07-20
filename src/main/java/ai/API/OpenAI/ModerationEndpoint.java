package ai.API.OpenAI;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.bson.Document;
import org.tinylog.Logger;

import com.squareup.moshi.JsonAdapter;

import ai.App;
import ai.API.OpenAI.RequestBody.*;
import ai.Utility.Http;

public class ModerationEndpoint {
    private static final String moderationEndpoint = "https://api.openai.com/v1/moderations";
    private static final JsonAdapter<RequestBody> requestBodyAdapter = App.Moshi.adapter(RequestBody.class);
    private static final JsonAdapter<ModerationObject> moderationObjectAdapter = App.Moshi.adapter(ModerationObject.class);

    private static Callable<HttpResponse<String>> createModerationRequest(String text) {
        String requestBodyJson = new Document("input", text).toJson();
        return Http.createRequest(Http.POST(moderationEndpoint, OpenaiApi.headers, requestBodyJson));
    }

    private static Callable<HttpResponse<String>> createModerationRequest(String text, String imageURL) {
        RequestBody requestBody = new RequestBody();
        requestBody.addInput(RequestBodyInput.createTextInput(text)); // add text input
        requestBody.addInput(RequestBodyInput.createImageInput(imageURL)); // add image input

        String requestBodyJson = requestBodyAdapter.toJson(requestBody);
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
    
                Logger.tag("ai").debug("Moderation response body: \n{}", apiResponse.body());
                return new ModerationResult(moderationObjectAdapter.fromJson(apiResponse.body()), text, null);
            } catch (Exception e) {
                Logger.error(e);
                throw new RuntimeException(e);
            }
        });
    }

    private static CompletableFuture<ModerationResult> moderateTextAndImage(String text, String imageURL) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // verify request success
                HttpResponse<String> apiResponse = createModerationRequest(text, imageURL).call();
                if (apiResponse.statusCode() != 200) {
                    throw new Exception(apiResponse.body());
                }
    
                Logger.tag("ai").debug("Moderation response body: \n{}", apiResponse.body());
                return new ModerationResult(moderationObjectAdapter.fromJson(apiResponse.body()), text, imageURL);
            } catch (Exception e) {
                Logger.error(e);
                throw new RuntimeException(e);
            }
        });
    }

    public static CompletableFuture<ModerationResult> moderateTextAndImages(String text, String[] imageURLs) {
        if (imageURLs.length == 0) return moderateText(text);
        if (imageURLs.length == 1) return moderateTextAndImage(text, imageURLs[0]);

        return CompletableFuture.supplyAsync(() -> {
            List<CompletableFuture<ModerationResult>> cfs = new ArrayList<CompletableFuture<ModerationResult>>();
            
            // add ModerationEndpoint requests to list
            cfs.add(ModerationEndpoint.moderateText(text));
            for (String imageURL : imageURLs) {
                cfs.add(ModerationEndpoint.moderateTextAndImage(null, imageURL));
            }
            
            // wait for requests to finish
            CompletableFuture.allOf(cfs.toArray(new CompletableFuture[0])).join();
            
            // return merged result
            return ModerationResult.mergeResults(cfs.stream().map(future -> {
                try {
                    return future.get();
                } catch (InterruptedException | ExecutionException e) {
                    Logger.error(e);
                    return null;
                }
            }).filter(result -> result != null).toArray(ModerationResult[]::new));
        });
    }
}
