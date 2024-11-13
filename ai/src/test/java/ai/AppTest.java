package ai;

import ai.API.OpenAI.ModerationEndpoint;

public class AppTest {
    public static void main(String[] args) throws Exception {
        // long tta = 5439768;
        // String s = (tta/1000) + "s and " + (tta%1000) + "ms";
        // System.out.println(s);

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
