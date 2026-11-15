package edu.cit.checkedin.common;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Gives services one shared Clock for "now".
 * Services ask this Clock instead of calling the computer's clock directly, so tests can set
 * a fixed time (for example, to check that a sign-in code expires after 10 minutes).
 * The default time zone is Asia/Manila, set in CheckedInApplication (NFR-035).
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
