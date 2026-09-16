package comp3011.assignment1.dto;

import java.time.Instant;

import org.springframework.http.HttpStatus;

/**
 * Standard JSON error body returned by every endpoint on failure (schema {@code ErrorResponse}).
 *
 * @param timestamp RFC 3339 UTC timestamp at which the error was generated
 * @param status    HTTP status code
 * @param error     HTTP reason phrase
 * @param message   human-readable description of the failure (never contains secrets)
 * @param path      request path that produced the error
 */
public record ErrorResponse(String timestamp, int status, String error, String message, String path) {

    /** Creates an error body stamped with the current UTC time. */
    public static ErrorResponse of(HttpStatus status, String message, String path) {
        return new ErrorResponse(Instant.now().toString(), status.value(), status.getReasonPhrase(), message, path);
    }
}
