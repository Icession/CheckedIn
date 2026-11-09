package edu.cit.checkedin.common;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;

import javax.sql.DataSource;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Setup check (Phase 0).
 * Lets the frontend confirm that the backend is running and can reach the database (PostgreSQL on Supabase).
 *
 * GET /api/health  ->  { "status": "UP", "database": "UP", "time": "..." }
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private static final int DB_CHECK_TIMEOUT_SECONDS = 2;

    private final DataSource dataSource;

    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping
    public HealthResponse checkHealth() {
        String database = isDatabaseUp() ? "UP" : "DOWN";
        return new HealthResponse("UP", database, LocalDateTime.now());
    }

    /** Opens a connection and asks the database if it is still valid. */
    private boolean isDatabaseUp() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(DB_CHECK_TIMEOUT_SECONDS);
        } catch (SQLException e) {
            return false;
        }
    }
}
