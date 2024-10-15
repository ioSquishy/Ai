package ai;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.javacord.api.entity.user.User;

import ai.API.OpenAI.ModerationEndpoint;
import ai.API.OpenAI.ModerationEndpoint.ModerationResult;

public class AppTest {
    public static void main(String[] args) throws Exception {
        String inputText = "testt";
        String[] inputImages = new String[] {
        };

        ModerationEndpoint.moderateTextAndImages(inputText, inputImages).thenAcceptAsync(modResult -> {
            System.out.println(modResult);
            for (String url : modResult.flaggedImageURLs) {
                System.out.println(url);
            }
        });
        
    }
}
