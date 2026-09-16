package comp3011.assignment1.dto;

/**
 * Response body for {@code GET /api/v1/global/stats} (schema {@code GlobalStatsResponse}).
 *
 * @param inputTokens  total input tokens consumed by the STT service since server start
 * @param outputTokens total output tokens produced by the STT service since server start
 */
public record GlobalStatsResponse(long inputTokens, long outputTokens) {
}
