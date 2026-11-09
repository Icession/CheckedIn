package edu.cit.checkedin.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * The one place where errors from any controller are turned into a JSON response
 * { "message": "..." } with the correct HTTP status code.
 * Covers NFR-020 (error messages say what went wrong and how to fix it).
 *
 * Services report a broken rule by throwing, for example:
 *   throw new ResponseStatusException(HttpStatus.CONFLICT, "This student ID is already linked to another account.");
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** A business rule was broken. The service already chose the status and the message. */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleRuleBroken(ResponseStatusException exception) {
        String message = exception.getReason();
        if (message == null) {
            message = "The request could not be completed. Please check your input and try again.";
        }
        return ResponseEntity.status(exception.getStatusCode()).body(new ErrorResponse(message));
    }

    /** A request DTO failed its @NotBlank / @Pattern / ... checks. We show the first problem found. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleInvalidInput(MethodArgumentNotValidException exception) {
        FieldError firstError = exception.getBindingResult().getFieldError();
        String message = (firstError != null)
                ? firstError.getDefaultMessage()
                : "Some of the information you entered is not valid. Please check it and try again.";
        return buildResponse(HttpStatus.BAD_REQUEST, message);
    }

    /** The request body is missing or is not valid JSON. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException exception) {
        return buildResponse(HttpStatus.BAD_REQUEST,
                "The data sent to the server is missing or has the wrong format. Please try again.");
    }

    /** A value in the URL has the wrong type, e.g. /api/sections/abc where a number is expected. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleWrongTypeInUrl(MethodArgumentTypeMismatchException exception) {
        return buildResponse(HttpStatus.BAD_REQUEST,
                "The value '" + exception.getValue() + "' is not valid for " + exception.getName() + ".");
    }

    /** No endpoint exists at this address. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleUnknownAddress(NoResourceFoundException exception) {
        return buildResponse(HttpStatus.NOT_FOUND, "This address does not exist. Please check the URL.");
    }

    /** The endpoint exists but not for this method, e.g. DELETE where only GET is allowed. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleWrongMethod(HttpRequestMethodNotSupportedException exception) {
        return buildResponse(HttpStatus.METHOD_NOT_ALLOWED,
                "This action is not allowed here (" + exception.getMethod() + ").");
    }

    // Anything we did not expect. We log the details for us, but never send them to the browser.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedError(Exception exception) {
        log.error("Unexpected error", exception);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "Something went wrong on our side. Please try again in a moment.");
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(message));
    }
}
