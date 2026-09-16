package comp3011.assignment1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import comp3011.assignment1.exception.InvalidAudioException;
import comp3011.assignment1.exception.SpeechToTextException;

/** Unit tests for {@link OpenAiSttService} against a mocked OpenAI endpoint (no network access). */
class OpenAiSttServiceTest {

    private static final String BASE_URL = "https://api.openai.com/v1";

    private MockRestServiceServer server;
    private TokenUsageService tokenUsage;
    private OpenAiSttService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader("Authorization", "Bearer test-key");
        server = MockRestServiceServer.bindTo(builder).build();
        tokenUsage = new TokenUsageService();
        service = new OpenAiSttService(builder.build(), tokenUsage);
    }

    @Test
    void sendsAudioWithMatchingExtensionAndRecordsTokens() {
        server.expect(requestTo(BASE_URL + "/audio/transcriptions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andExpect(content().string(containsString("gpt-4o-mini-transcribe")))
                .andExpect(content().string(containsString("filename=\"audio.webm\"")))
                .andRespond(withSuccess("""
                        {"text":"hello world","usage":{"type":"tokens","input_tokens":42,"output_tokens":7,"total_tokens":49}}
                        """, MediaType.APPLICATION_JSON));

        TranscriptionResult result = service.transcribe(
                new MockMultipartFile("file", "recording.webm", "audio/webm;codecs=opus", new byte[] {1, 2, 3}));

        assertThat(result.text()).isEqualTo("hello world");
        assertThat(tokenUsage.snapshot().inputTokens()).isEqualTo(42);
        assertThat(tokenUsage.snapshot().outputTokens()).isEqualTo(7);
        server.verify();
    }

    @Test
    void upstreamErrorBecomesSpeechToTextExceptionAndCountsNothing() {
        server.expect(requestTo(BASE_URL + "/audio/transcriptions"))
                .andRespond(withBadRequest().body("{\"error\":{\"message\":\"Invalid file format.\"}}")
                        .contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.transcribe(
                new MockMultipartFile("file", "a.webm", "audio/webm", new byte[] {1})))
                .isInstanceOf(SpeechToTextException.class)
                .hasMessageContaining("400");
        assertThat(tokenUsage.snapshot().inputTokens()).isZero();
    }

    @Test
    void rejectsEmptyUpload() {
        assertThatThrownBy(() -> service.transcribe(
                new MockMultipartFile("file", "a.webm", "audio/webm", new byte[0])))
                .isInstanceOf(InvalidAudioException.class);
    }

    @Test
    void resolvesExtensionFromContentTypeThenFilename() {
        assertThat(OpenAiSttService.resolveExtension("audio/webm;codecs=opus", "x.wav")).isEqualTo("webm");
        assertThat(OpenAiSttService.resolveExtension("audio/ogg", null)).isEqualTo("ogg");
        assertThat(OpenAiSttService.resolveExtension("audio/mp4", null)).isEqualTo("mp4");
        assertThat(OpenAiSttService.resolveExtension("audio/mpeg", null)).isEqualTo("mp3");
        assertThat(OpenAiSttService.resolveExtension("application/octet-stream", "talk.MP3")).isEqualTo("mp3");
        assertThat(OpenAiSttService.resolveExtension(null, "unknown.xyz")).isEqualTo("webm");
    }

    @Test
    void parsesMissingUsageAsZero() {
        TranscriptionResult result = OpenAiSttService.parse(Map.of("text", "hi"));
        assertThat(result).isEqualTo(new TranscriptionResult("hi", 0, 0));
    }
}
