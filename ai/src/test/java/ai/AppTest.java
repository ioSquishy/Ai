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
        DiscordApi api = new DiscordApiBuilder().setToken(Dotenv.load().get("DISCORD_TOKEN")).setAllIntents().login().join();
        File openDoorGif = new File("ai\\src\\main\\Assets\\open_door.gif");
        try {
            api.getTextChannelById(842061663389614220L).get().sendMessage(openDoorGif).get();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
