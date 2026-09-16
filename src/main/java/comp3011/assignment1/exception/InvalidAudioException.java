package comp3011.assignment1.exception;

/** Thrown when a client uploads a missing or empty audio file (mapped to HTTP 400). */
public class InvalidAudioException extends RuntimeException {

    public InvalidAudioException(String message) {
        super(message);
    }
}
