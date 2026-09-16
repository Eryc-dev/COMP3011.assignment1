package comp3011.assignment1.service;

/**
 * Result of one call to the Cloud STT service.
 *
 * @param text         transcribed text
 * @param inputTokens  input tokens reported by the provider's {@code usage} block
 * @param outputTokens output tokens reported by the provider's {@code usage} block
 */
public record TranscriptionResult(String text, long inputTokens, long outputTokens) {
}
