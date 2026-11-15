package edu.cit.checkedin;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Starting point of the CheckedIn backend.
 * Run this class (or "mvnw spring-boot:run") to start the REST API on port 8080.
 */
@SpringBootApplication
// Finds settings records such as LoginCodeSettings and fills them from application.properties
@ConfigurationPropertiesScan
public class CheckedInApplication {

    private static final String PHILIPPINE_TIME_ZONE = "Asia/Manila";

    public static void main(String[] args) {
        // NFR-035: all dates and times are in Philippine time. Servers like Render run on UTC,
        // where "today" would start 8 hours late (at 8:00 AM Manila time). Setting the default
        // here, before Spring starts, makes LocalDate.now() and LocalDateTime.now() use Manila time everywhere.
        TimeZone.setDefault(TimeZone.getTimeZone(PHILIPPINE_TIME_ZONE));
        SpringApplication.run(CheckedInApplication.class, args);
    }
}
