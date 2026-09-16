package comp3011.assignment1.dto;

/**
 * Response body for an accepted {@code POST /api/v1/admin/shutdown} (schema {@code ShutdownResponse}).
 *
 * @param message human-readable shutdown acknowledgement
 */
public record ShutdownResponse(String message) {
}
