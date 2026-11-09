package edu.cit.checkedin.common;

import java.time.LocalDateTime;

/**
 * JSON returned by GET /api/health.
 *
 * @param status   "UP" when the backend is running
 * @param database "UP" when the database answers, "DOWN" when it does not
 * @param time     server time when the check ran
 */
public record HealthResponse(String status, String database, LocalDateTime time) {
}
