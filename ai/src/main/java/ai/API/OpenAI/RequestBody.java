package ai.API.OpenAI;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RequestBody {
    public final String model = "omni-moderation-latest";
    public List<RequestBodyInput> input;

    public RequestBody() {
        this.input = new ArrayList<RequestBodyInput>();
    }

    public void addInput(RequestBodyInput input) {
        if (input.text == null && input.image_url == null) {
            return;
        }
        this.input.add(input);
    }

    protected static class RequestBodyInput {
        public final String type;
        public String text = null;
        public Map<String, String> image_url = null;

        public static RequestBodyInput createTextInput(String text) {
            return new RequestBodyInput("text", text, null);
        }

        public static RequestBodyInput createImageInput(String imageURL) {
            return new RequestBodyInput("image_url", null, imageURL);
        }

        /**
         * Creates an input for the RequestBody class
         * @param type can be either "text" or "image_url"
         * @param text text to be moderated if type is "text"
         * @param imageURLs image to be moderated if type is "image_url"
         */
        protected RequestBodyInput(String type, String text, String imageURL) {
            this.type = type;
            this.text = (text != null && !text.isBlank()) ? text : null;
            this.image_url = (imageURL != null && !imageURL.isBlank()) ? Map.of("url", imageURL) : null;
        }
    }
}
