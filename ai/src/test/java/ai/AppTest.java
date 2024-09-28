package ai;

import ai.API.OpenAI.ModerationEndpoint;

public class AppTest {
    public static void main(String[] args) throws Exception {
        String inputText = "";
        String[] inputImages = new String[] {
            "https://cdn.discordapp.com/attachments/720032587313315962/1289312609589399664/image0.jpg?ex=66f85d70&is=66f70bf0&hm=2d1ffc6712546ecfa65a896b843442b2046e82326099307aebf8303826f25f65&",
            "https://media.discordapp.net/attachments/777746556828123157/1289380709345267733/image.png?ex=66f89cdc&is=66f74b5c&hm=de19a3e226688c09320ea6ca80aeece7ef4428ce616189c99d1f9eee366276ce&=&format=webp&quality=lossless&width=1866&height=752",
        };

        ModerationEndpoint.moderateTextAndImages(inputText, inputImages).thenAcceptAsync(modResult -> {
            System.out.println(modResult);
        });
    }
}
