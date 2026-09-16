package comp3011.assignment1.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;

import comp3011.assignment1.dto.GlobalStatsResponse;

class TokenUsageServiceTest {

    @Test
    void startsAtZero() {
        GlobalStatsResponse stats = new TokenUsageService().snapshot();
        assertThat(stats.inputTokens()).isZero();
        assertThat(stats.outputTokens()).isZero();
    }

    @Test
    void ignoresNegativeValues() {
        TokenUsageService service = new TokenUsageService();
        service.record(-5, -1);
        assertThat(service.snapshot()).isEqualTo(new GlobalStatsResponse(0, 0));
    }

    @Test
    void countsCorrectlyUnderConcurrentUpdates() {
        TokenUsageService service = new TokenUsageService();
        int tasks = 1_000;
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < tasks; i++) {
                executor.submit(() -> service.record(3, 2));
            }
        } // close() waits for all tasks

        assertThat(service.snapshot()).isEqualTo(new GlobalStatsResponse(3L * tasks, 2L * tasks));
    }
}
