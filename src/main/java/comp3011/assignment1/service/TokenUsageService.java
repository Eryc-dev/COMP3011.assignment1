package comp3011.assignment1.service;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

import comp3011.assignment1.dto.GlobalStatsResponse;

/**
 * Thread-safe, in-memory global token counters since server start.
 *
 * <p>{@link AtomicLong} gives lock-free updates, so hundreds of concurrent transcriptions never
 * block each other while recording usage. Counters reset when the process restarts, as required.
 */
@Service
public class TokenUsageService {

    private final AtomicLong inputTokens = new AtomicLong();
    private final AtomicLong outputTokens = new AtomicLong();

    /** Adds the usage of one completed transcription to the global totals. */
    public void record(long input, long output) {
        inputTokens.addAndGet(Math.max(0, input));
        outputTokens.addAndGet(Math.max(0, output));
    }

    /** Returns a snapshot of the current totals. */
    public GlobalStatsResponse snapshot() {
        return new GlobalStatsResponse(inputTokens.get(), outputTokens.get());
    }
}
