package comp3011.assignment1.dto;

/**
 * Response body for {@code GET /api/v1/admin/uptime} (schema {@code UptimeResponse}).
 *
 * @param utcServerStart      RFC 3339 UTC timestamp at which the server process started
 * @param utcNow              RFC 3339 UTC timestamp at response generation time
 * @param serverUptimeSeconds seconds between {@code utcServerStart} and {@code utcNow}
 */
public record UptimeResponse(String utcServerStart, String utcNow, double serverUptimeSeconds) {
}
