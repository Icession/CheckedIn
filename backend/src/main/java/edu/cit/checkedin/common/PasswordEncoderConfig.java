package edu.cit.checkedin.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * The hashing used for emailed sign-in codes (NFR-008): BCrypt, a slow and salted hash, so a
 * leaked database does not reveal the codes. Kept apart from SecurityConfig because it is
 * needed even when the app runs without a web server (for example, the test-email command).
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
