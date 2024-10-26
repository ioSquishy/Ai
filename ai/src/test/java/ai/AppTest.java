package ai;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.user.User;

import ai.API.OpenAI.ModerationEndpoint;
import ai.API.OpenAI.ModerationEndpoint.ModerationResult;
import io.github.cdimascio.dotenv.Dotenv;

public class AppTest {
    public static void main(String[] args) throws Exception {
        long tta = 5439768;
        String s = (tta/1000) + "s and " + (tta%1000) + "ms";
        System.out.println(s);
        // String inputText = "testt";
        // String[] inputImages = new String[] {
        // };

        // ModerationEndpoint.moderateTextAndImages(inputText, inputImages).thenAcceptAsync(modResult -> {
        //     System.out.println(modResult);
        //     for (String url : modResult.flaggedImageURLs) {
        //         System.out.println(url);
        //     }
        // });
    }
}
