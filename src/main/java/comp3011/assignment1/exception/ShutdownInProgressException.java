package comp3011.assignment1.exception;

/** Thrown when shutdown is requested while one is already in progress (mapped to HTTP 409). */
public class ShutdownInProgressException extends RuntimeException {

    public ShutdownInProgressException() {
        super("Graceful shutdown is already in progress.");
    }
}
