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
            RestClient.Builder builder,
            @Value("${openai.base-url:${openai.api-base:${OPENAI_BASE_URL:${OPENAI_API_BASE:https://api.openai.com/v1}}}}")
            String baseUrl,

            @Value("${openai.api-key:${openai.key:${OPENAI_API_KEY:dummy_key}}}")
            String apiKey) {
 
        String normalizedBaseUrl = baseUrl.trim();
        if (normalizedBaseUrl.endsWith("/")) {
            normalizedBaseUrl = normalizedBaseUrl.substring(0, normalizedBaseUrl.length() - 1);
        }
        if (!normalizedBaseUrl.endsWith("/v1")) {
            normalizedBaseUrl = normalizedBaseUrl + "/v1";
        }
 
        this.restClient = builder.baseUrl(normalizedBaseUrl).build();
        this.apiKey = apiKey;
    }
 
   
    public TranscriptionResult transcribe(MultipartFile file) throws IOException {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
 
        String originalFilename = file.getOriginalFilename();
        String finalFilename;
        if (originalFilename == null || originalFilename.isBlank()) {
            finalFilename = "audio.wav";
        } else if (!originalFilename.contains(".")) {
            finalFilename = originalFilename + ".wav";
        } else {
            finalFilename = originalFilename;
        }
 
        byte[] bytes = file.getBytes();
 
        ByteArrayResource audioResource = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return finalFilename;
            }
        };
 
        builder.part("file", audioResource, MediaType.parseMediaType("audio/wav"))
                .filename(finalFilename);

        builder.part("model", "gpt-4o-mini-transcribe");

        builder.part("response_format", "json");
 
        try {
            Map<?, ?> response = restClient.post()
                    .uri("/audio/transcriptions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(builder.build())
                    .retrieve()
                    .body(Map.class);
 
            if (response == null) {
                return new TranscriptionResult("", 0, 0);
            }
 
            String text = response.get("text") != null ? response.get("text").toString() : "";
            long inputTokens = 0;
            long outputTokens = 0;
 
            Object usageObj = response.get("usage");
            if (usageObj instanceof Map<?, ?> usage) {
                inputTokens = readTokenCount(usage, "input_tokens", "prompt_tokens");
                outputTokens = readTokenCount(usage, "output_tokens", "completion_tokens");
            }
 
            return new TranscriptionResult(text, inputTokens, outputTokens);
        } catch (Exception e) {
            throw new IOException("STT processing error: " + e.getMessage(), e);
        }
    }
 
   
    private long readTokenCount(Map<?, ?> usage, String primaryKey, String fallbackKey) {
        Object value = usage.get(primaryKey);
        if (value == null) {
            value = usage.get(fallbackKey);
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return 0;
    }
}
