package comp3011.assignment1.dto;

/**
 * Response body for {@code POST /api/v1/transcribe}.
 *
 * @param text the transcription of the uploaded audio
 */
public record TranscriptionResponse(String text) {
}
