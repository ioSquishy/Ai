package ai;

import ai.API.OpenAI.ModerationEndpoint;

public class AppTest {
    public static void main(String[] args) throws Exception {
        ModerationEndpoint.moderateText("").thenAcceptAsync(modResult -> {
            System.out.println(modResult);
        });
    }
}
