package ai;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import ai.API.OpenAI.ModerationEndpoint;
import ai.API.OpenAI.ModerationEndpoint.ModerationResult;

public class AppTest {
    public static void main(String[] args) throws Exception {
        String inputText = "testt";
        String[] inputImages = new String[] {
        };

        List<CompletableFuture<ModerationResult>> cfs = new ArrayList<CompletableFuture<ModerationResult>>();
        cfs.add(ModerationEndpoint.moderateText(inputText));
        for (String imageURL : inputImages) {
            cfs.add(ModerationEndpoint.moderateTextAndImage(null, imageURL));
        }
        CompletableFuture.runAsync(() -> {
            CompletableFuture.allOf(cfs.toArray(new CompletableFuture[0])).thenRun(() -> {
                ModerationResult mergedResult = ModerationResult.mergeResults(cfs.stream().map(future -> {
                    try {
                        return future.get();
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                        return null;
                    }
                }).filter(result -> result != null).toArray(ModerationResult[]::new));
                
            });
        });
        
    }
}
