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
            @Value("${openai.base-url:${openai.api-base:${OPENAI_BASE_URL:${OPENAI_API_BASE:https://api.openai.com/v1}}}}") String baseUrl,
            @Value("${openai.api-key:${openai.key:${OPENAI_API_KEY:dummy_key}}}") String apiKey) {

        String url = baseUrl.trim();
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        if (!url.endsWith("/v1")) {
            url = url + "/v1";
        }

        this.restClient = restClientBuilder.baseUrl(url).build();
        this.apiKey = apiKey;
    }

    public String transcribe(MultipartFile file) throws IOException {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            filename = "audio.wav";
        } else if (!filename.contains(".")) {
            filename = filename + ".wav";
        }

        byte[] bytes = file.getBytes();
        String finalFilename = filename;
        
        ByteArrayResource audioResource = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return finalFilename;
            }
        };

        builder.part("file", audioResource, MediaType.parseMediaType("audio/wav"))
               .filename(finalFilename);
        builder.part("model", "whisper-1");

        try {
            Map<?, ?> response = restClient.post()
                    .uri("/audio/transcriptions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(builder.build())
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.get("text") != null) {
                return response.get("text").toString();
            }
        } catch (Exception e) {
            throw new IOException("STT processing error: " + e.getMessage(), e);
        }
        return "";
    }
}