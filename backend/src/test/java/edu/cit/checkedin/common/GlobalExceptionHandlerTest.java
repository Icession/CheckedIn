package edu.cit.checkedin.common;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * Checks that every kind of error comes back as { "message": "..." } with the right HTTP status.
 * A small fake controller (below) throws each kind of error on purpose.
 */
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FailingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void ruleBrokenUsesTheStatusAndMessageFromTheService() throws Exception {
        mockMvc.perform(get("/test/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("This student ID is already linked to another account."));
    }

    @Test
    void ruleBrokenWithoutMessageStillReturnsAMessage() throws Exception {
        mockMvc.perform(get("/test/conflict-without-message"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void invalidInputReturnsTheValidationMessage() throws Exception {
        mockMvc.perform(post("/test/validated")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"name\": \"\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Please enter a name."));
    }

    @Test
    void unreadableBodyReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/test/validated")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("this is not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void wrongTypeInUrlReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/test/number/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The value 'abc' is not valid for id."));
    }

    @Test
    void unknownAddressReturnsNotFound() throws Exception {
        mockMvc.perform(get("/test/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void wrongMethodReturnsMethodNotAllowed() throws Exception {
        mockMvc.perform(delete("/test/conflict"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void unexpectedErrorHidesTheDetails() throws Exception {
        mockMvc.perform(get("/test/crash"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value(not(containsString("secret"))))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    /** JSON body for the validation test. */
    record NameRequest(@NotBlank(message = "Please enter a name.") String name) {
    }

    /** Fake controller used only in this test. Each endpoint fails in a different way. */
    @RestController
    static class FailingController {

        @GetMapping("/test/conflict")
        void conflict() {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This student ID is already linked to another account.");
        }

        @GetMapping("/test/conflict-without-message")
        void conflictWithoutMessage() {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }

        @PostMapping("/test/validated")
        void validated(@Valid @RequestBody NameRequest request) {
        }

        @GetMapping("/test/number/{id}")
        void number(@PathVariable("id") Long id) {
        }

        @GetMapping("/test/missing")
        void missing() throws NoResourceFoundException {
            throw new NoResourceFoundException(HttpMethod.GET, "test/missing");
        }

        @GetMapping("/test/crash")
        void crash() {
            throw new IllegalStateException("secret database detail");
        }
    }
}
