package comp3011.assignment1;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

/**
 * Full application tests on a real embedded server (random port).
 *
 * <p>No OpenAI calls are made here; the concurrency test uses the local admin/statistics endpoints
 * to show the server serves well over 200 overlapping requests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Assignment1ApplicationTests {

    @Autowired
    private Environment environment;

    private final HttpClient client = HttpClient.newHttpClient();

    private String baseUrl() {
        return "http://localhost:" + environment.getProperty("local.server.port");
    }

    @Test
    void servesIndexPageAtRoot() throws Exception {
        HttpResponse<String> response = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl() + "/")).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("Record Audio").contains("Stop Recording");
    }

    @Test
    void handlesMoreThan200ConcurrentRequests() throws Exception {
        int requests = 300;
        List<Future<Integer>> results = new ArrayList<>();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < requests; i++) {
                String path = i % 2 == 0 ? "/api/v1/admin/uptime" : "/api/v1/global/stats";
                results.add(executor.submit(() -> client.send(
                        HttpRequest.newBuilder(URI.create(baseUrl() + path)).GET().build(),
                        HttpResponse.BodyHandlers.discarding()).statusCode()));
            }
            for (Future<Integer> result : results) {
                assertThat(result.get()).isEqualTo(200);
            }
        }
    }
}
