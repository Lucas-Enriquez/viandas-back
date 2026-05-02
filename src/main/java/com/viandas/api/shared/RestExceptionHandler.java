package com.viandas.api.shared;

import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.InvalidFormatException;

@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ErrorResponse> handleApiException(ApiException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(new ErrorResponse(exception.getStatus()
                        .value(), exception.getMessage(), List.of(), Instant.now()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {

        List<FieldViolation> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(RestExceptionHandler::toViolation)
                .toList();

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "One or more request fields are invalid");

        problem.setTitle("Validation failed");
        problem.setType(URI.create("https://api.viandas.local/problems/validation-error"));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("errors", errors);
        problem.setProperty("timestamp", Instant.now());

        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ProblemDetail> handleUnreadableMessage(
            HttpMessageNotReadableException exception,
            HttpServletRequest request) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Request body is invalid or contains unsupported values!");

        problem.setTitle("Invalid request body");
        problem.setType(URI.create("https://api.viandas.local/problems/invalid-request-body"));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", Instant.now());

        InvalidFormatException invalidFormat = findCause(exception, InvalidFormatException.class);
        if (invalidFormat != null && invalidFormat.getTargetType().isEnum()) {
            problem.setDetail("Invalid value for enum field");
            problem.setProperty("field", formatJsonPath(invalidFormat.getPath()));
            problem.setProperty("rejectedValue", invalidFormat.getValue());
            problem.setProperty("acceptedValues", enumValues(invalidFormat.getTargetType()));
        }

        return ResponseEntity.badRequest().body(problem);
    }

    private static FieldViolation toViolation(FieldError error) {
        return new FieldViolation(
                error.getField(),
                error.getDefaultMessage(),
                error.getCode(),
                error.getRejectedValue());
    }

    private static String formatJsonPath(List<JacksonException.Reference> path) {
        return path.stream()
                .map(JacksonException.Reference::getPropertyName)
                .filter(field -> field != null && !field.isBlank())
                .reduce((parent, child) -> parent + "." + child)
                .orElse(null);
    }

    private static List<String> enumValues(Class<?> enumType) {
        return Arrays.stream(enumType.getEnumConstants())
                .map(Object::toString)
                .toList();
    }

    private static <T extends Throwable> T findCause(Throwable throwable, Class<T> causeType) {
        Throwable current = throwable;
        while (current != null) {
            if (causeType.isInstance(current)) {
                return causeType.cast(current);
            }
            current = current.getCause();
        }
        return null;
    }

    public record FieldViolation(
            String field,
            String message,
            String code,
            Object rejectedValue
    ) {
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    ResponseEntity<ProblemDetail> handleNoHandlerFound(
            NoHandlerFoundException exception,
            HttpServletRequest request
    ) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                "No endpoint found for " + exception.getHttpMethod() + " " + exception.getRequestURL());

        problem.setTitle("Endpoint not found");
        problem.setType(URI.create("https://api.viandas.local/problems/not-found"));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", Instant.now());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }


    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ProblemDetail> handleNoResourceFound(
            NoResourceFoundException exception,
            HttpServletRequest request
    ) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                "No endpoint found for " + request.getMethod() + " " + request.getRequestURI());

        problem.setTitle("Endpoint not found");
        problem.setType(URI.create("https://api.viandas.local/problems/not-found"));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", Instant.now());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ProblemDetail> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request
    ) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.METHOD_NOT_ALLOWED,
                request.getMethod() + " is not supported for " + request.getRequestURI());

        problem.setTitle("Method not allowed");
        problem.setType(URI.create("https://api.viandas.local/problems/method-not-allowed"));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("method", request.getMethod());
        problem.setProperty("supportedMethods", exception.getSupportedMethods());
        problem.setProperty("timestamp", Instant.now());

        HttpHeaders headers = new HttpHeaders();
        for (String method : exception.getSupportedMethods()) {
            headers.add(HttpHeaders.ALLOW, method);
        }

        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .headers(headers)
                .body(problem);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(500, "Unexpected error", List.of(), Instant.now()));
    }

    private static String formatFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }

    public record ErrorResponse(
            int status,
            String message,
            List<String> errors,
            Instant timestamp
    ) {
    }
}
