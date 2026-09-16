package comp3011.assignment1.config;

import java.net.http.HttpClient;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Builds the single, shared {@link RestClient} used to call the OpenAI transcription API.
 *
 * <p>The API key is read at runtime from the {@code OPENAI_API_KEY} environment variable and is
 * only ever placed in the outbound Authorization header. It is never logged, stored in source
 * code, or returned to browsers/API clients.
 *
 * <p>The underlying JDK {@link HttpClient} is thread-safe and pools connections, so one instance
 * serves every concurrent request. Explicit timeouts stop a slow upstream from holding request
 * threads indefinitely.
 */
@Configuration
public class OpenAiClientConfig {

    private static final Logger log = LoggerFactory.getLogger(OpenAiClientConfig.class);

    @Bean
    public RestClient openAiRestClient(
            RestClient.Builder builder,
            @Value("${openai.base-url}") String baseUrl,
            @Value("${openai.connect-timeout-seconds:5}") long connectTimeoutSeconds,
            @Value("${openai.read-timeout-seconds:60}") long readTimeoutSeconds,
            @Value("${OPENAI_API_KEY:}") String apiKey) {

        log.info("OpenAI base URL: {} (API key present: {})", baseUrl, !apiKey.isBlank());
        if (apiKey.isBlank()) {
            // Only report that the key is missing - never its value.
            log.warn("OPENAI_API_KEY is not set; transcription requests will fail.");
        }

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(connectTimeoutSeconds))
                // HTTP/1.1 avoids HTTP/2 stream issues with large multipart uploads.
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(readTimeoutSeconds));

        return builder
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
    }
}
