package ai;

import ai.API.OpenAI.ModerationEndpoint;

public class AppTest {
    public static void main(String[] args) throws Exception {
        String inputText = "";
        String[] inputImages = new String[] {""};

        ModerationEndpoint.moderateTextAndImages(inputText, inputImages).thenAcceptAsync(modResult -> {
            System.out.println(modResult);
            System.out.println(modResult.getInputImageURLs());
        });
    }
}
