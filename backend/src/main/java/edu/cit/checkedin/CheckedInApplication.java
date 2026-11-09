package edu.cit.checkedin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Starting point of the CheckedIn backend.
 * Run this class (or "mvnw spring-boot:run") to start the REST API on port 8080.
 */
@SpringBootApplication
public class CheckedInApplication {

    public static void main(String[] args) {
        SpringApplication.run(CheckedInApplication.class, args);
    }
}
