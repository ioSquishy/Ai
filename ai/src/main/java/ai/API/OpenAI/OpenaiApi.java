package ai.API.OpenAI;

import java.util.Map;

import io.github.cdimascio.dotenv.Dotenv;

public class OpenaiApi {
    private static final String apiKey = Dotenv.load().get("OPENAI_KEY");
    
    protected static final Map<String, String> headers = Map.of(
        "Content-Type", "application/json",
        "Authorization", "Bearer "+apiKey,
        "OpenAI-Organization", "org-H3RmQBHZoOkdEgoxLocQ5W3w",
        "OpenAI-Project", "proj_Vn4oBEEG7ZJ4pR7iWDEDPNMG"
    );
}
