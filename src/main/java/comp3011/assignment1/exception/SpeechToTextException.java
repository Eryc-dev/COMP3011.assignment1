package comp3011.assignment1.exception;

/** Thrown when the upstream Cloud STT service fails or is unreachable (mapped to HTTP 502). */
public class SpeechToTextException extends RuntimeException {

    public SpeechToTextException(String message, Throwable cause) {
        super(message, cause);
    }
}
