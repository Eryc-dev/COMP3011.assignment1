package comp3011.assignment1.controller;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.InputStream;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestClient;

import comp3011.assignment1.exception.ApiExceptionHandler;
import comp3011.assignment1.service.OpenAiSttService;
import comp3011.assignment1.service.ServerLifecycleService;
import comp3011.assignment1.service.TokenUsageService;

/**
 * Checks every endpoint's status codes and JSON field names against the YAML specification,
 * using MockMvc with real services and a mocked OpenAI server.
 */
class ApiEndpointsTest {

    private static final String OPENAI_URL = "https://api.openai.com/v1/audio/transcriptions";

    private MockMvc mockMvc;
    private MockRestServiceServer openAi;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.openai.com/v1");
        openAi = MockRestServiceServer.bindTo(builder).build();

        TokenUsageService tokenUsage = new TokenUsageService();
        OpenAiSttService stt = new OpenAiSttService(builder.build(), tokenUsage);
        // No-op shutdown action so the test JVM is not stopped.
        ServerLifecycleService lifecycle = new ServerLifecycleService(Instant.now().minusSeconds(5), () -> { });

        mockMvc = MockMvcBuilders
                .standaloneSetup(new TranscriptionController(stt), new AdminController(lifecycle),
                        new GlobalStatsController(tokenUsage))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void uptimeMatchesSchema() throws Exception {
        mockMvc.perform(get("/api/v1/admin/uptime"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.utcServerStart").isString())
                .andExpect(jsonPath("$.utcNow").isString())
                .andExpect(jsonPath("$.serverUptimeSeconds").value(greaterThanOrEqualTo(5.0)));
    }

    @Test
    void statsStartAtZero() throws Exception {
        mockMvc.perform(get("/api/v1/global/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inputTokens").value(0))
                .andExpect(jsonPath("$.outputTokens").value(0));
    }

    @Test
    void shutdownReturns202ThenConflict() throws Exception {
        mockMvc.perform(post("/api/v1/admin/shutdown"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value("Graceful shutdown requested."));

        mockMvc.perform(post("/api/v1/admin/shutdown"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Graceful shutdown is already in progress."))
                .andExpect(jsonPath("$.path").value("/api/v1/admin/shutdown"))
                .andExpect(jsonPath("$.timestamp").isString());
    }

    @Test
    void transcribeReturnsTextAndUpdatesStats() throws Exception {
        openAi.expect(requestTo(OPENAI_URL)).andRespond(withSuccess(
                "{\"text\":\"testing one two three\",\"usage\":{\"input_tokens\":120,\"output_tokens\":9}}",
                MediaType.APPLICATION_JSON));

        byte[] audio;
        try (InputStream in = getClass().getResourceAsStream("/test.mp3")) {
            audio = in.readAllBytes();
        }

        mockMvc.perform(multipart("/api/v1/transcribe")
                        .file(new MockMultipartFile("file", "test.mp3", "audio/mpeg", audio)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("testing one two three"));

        mockMvc.perform(get("/api/v1/global/stats"))
                .andExpect(jsonPath("$.inputTokens").value(120))
                .andExpect(jsonPath("$.outputTokens").value(9));
    }

    @Test
    void transcribeUpstreamFailureReturnsErrorResponse() throws Exception {
        openAi.expect(requestTo(OPENAI_URL)).andRespond(withServerError());

        mockMvc.perform(multipart("/api/v1/transcribe")
                        .file(new MockMultipartFile("file", "a.webm", "audio/webm", new byte[] {1, 2})))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.path").value("/api/v1/transcribe"));
    }

    @Test
    void transcribeEmptyFileIsBadRequest() throws Exception {
        mockMvc.perform(multipart("/api/v1/transcribe")
                        .file(new MockMultipartFile("file", "a.webm", "audio/webm", new byte[0])))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
