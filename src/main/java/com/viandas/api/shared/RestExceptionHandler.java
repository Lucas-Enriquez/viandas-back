package com.viandas.api.shared;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
    ResponseEntity<ApiResponse<Void>> handleApiException(
            ApiException exception,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(exception.getStatus())
                .body(ApiResponse.error(
                        exception.getMessage(),
                        List.of(new ApiError(exception.getMessage())),
                        meta(exception.getStatus(), request)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Void>> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        List<ApiError> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(RestExceptionHandler::toApiError)
                .toList();

        String message = "One or more request fields are invalid";
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(message, errors, meta(HttpStatus.BAD_REQUEST, request)));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiResponse<Void>> handleUnreadableMessage(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        String message = "Request body is invalid or contains unsupported values";
        List<ApiError> errors = List.of(new ApiError(message));
        Map<String, Object> meta = meta(HttpStatus.BAD_REQUEST, request);

        InvalidFormatException invalidFormat = findCause(exception, InvalidFormatException.class);
        if (invalidFormat != null && invalidFormat.getTargetType().isEnum()) {
            String field = formatJsonPath(invalidFormat.getPath());
            List<String> acceptedValues = enumValues(invalidFormat.getTargetType());

            message = "Invalid value for enum field";
            errors = List.of(new ApiError(field, "Invalid value. Accepted values: " + String.join(", ", acceptedValues)));
            meta.put("rejectedValue", invalidFormat.getValue());
            meta.put("acceptedValues", acceptedValues);
        }

        return ResponseEntity.badRequest()
                .body(ApiResponse.error(message, errors, meta));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    ResponseEntity<ApiResponse<Void>> handleNoHandlerFound(
            NoHandlerFoundException exception,
            HttpServletRequest request
    ) {
        String message = "No endpoint found for " + exception.getHttpMethod() + " " + exception.getRequestURL();
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(
                        "Endpoint not found",
                        List.of(new ApiError(message)),
                        meta(HttpStatus.NOT_FOUND, request)));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiResponse<Void>> handleNoResourceFound(
            NoResourceFoundException exception,
            HttpServletRequest request
    ) {
        String message = "No endpoint found for " + request.getMethod() + " " + request.getRequestURI();
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(
                        "Endpoint not found",
                        List.of(new ApiError(message)),
                        meta(HttpStatus.NOT_FOUND, request)));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request
    ) {
        Map<String, Object> meta = meta(HttpStatus.METHOD_NOT_ALLOWED, request);
        if (exception.getSupportedMethods() != null) {
            meta.put("supportedMethods", exception.getSupportedMethods());
        }

        HttpHeaders headers = new HttpHeaders();
        if (exception.getSupportedMethods() != null) {
            for (String method : exception.getSupportedMethods()) {
                headers.add(HttpHeaders.ALLOW, method);
            }
        }

        String message = request.getMethod() + " is not supported for " + request.getRequestURI();
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .headers(headers)
                .body(ApiResponse.error(
                        "Method not allowed",
                        List.of(new ApiError(message)),
                        meta));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> handleUnexpected(
            Exception exception,
            HttpServletRequest request
    ) {
        String message = "Unexpected error";
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(
                        message,
                        List.of(new ApiError(message)),
                        meta(HttpStatus.INTERNAL_SERVER_ERROR, request)));
    }

    private static ApiError toApiError(FieldError error) {
        return new ApiError(error.getField(), error.getDefaultMessage());
    }

    private static Map<String, Object> meta(HttpStatus status, HttpServletRequest request) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("status", status.value());
        meta.put("path", request.getRequestURI());
        meta.put("timestamp", Instant.now());
        return meta;
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
}
