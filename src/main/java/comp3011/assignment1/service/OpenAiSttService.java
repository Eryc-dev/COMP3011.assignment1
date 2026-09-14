package comp3011.assignment1.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class OpenAiSttService {
    private final RestClient restClient;
    private final String apiKey;

    public OpenAiSttService(
            RestClient.Builder restClientBuilder,
            @Value("${OPENAI_API_KEY:dummy_key_for_test}") String apiKey) {
        this.restClient = restClientBuilder
                .baseUrl("https://api.openai.com/v1")
                .build();
        this.apiKey = apiKey;
    }

    public String transcribe(MultipartFile file) throws IOException {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();

        String filename = (file.getOriginalFilename() != null && !file.getOriginalFilename().isEmpty())
                ? file.getOriginalFilename()
                : "recording.wav";

        ByteArrayResource audioResource = new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return filename;
            }
        };

        builder.part("file", audioResource)
               .header("Content-Disposition", "form-data; name=\"file\"; filename=\"" + filename + "\"");
        builder.part("model", "whisper-1");

        Map<?, ?> response = restClient.post()
                .uri("/audio/transcriptions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(builder.build())
                .retrieve()
                .body(Map.class);

        if (response != null && response.containsKey("text")) {
            return (String) response.get("text");
        }
        return "";
    }
}
